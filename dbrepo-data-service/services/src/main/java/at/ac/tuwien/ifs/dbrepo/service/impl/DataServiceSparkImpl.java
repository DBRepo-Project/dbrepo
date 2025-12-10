package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.service.DataService;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkException;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.ExtendedAnalysisException;
import org.apache.spark.sql.classic.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLSyntaxErrorException;
import java.util.List;
import java.util.UUID;

import static scala.collection.JavaConverters.asScalaIteratorConverter;

@Slf4j
@Service
public class DataServiceSparkImpl extends DataConnector implements DataService {

    private final S3Config s3Config;
    private final SparkSession sparkSession;

    @Autowired
    public DataServiceSparkImpl(S3Config s3Config, SparkSession sparkSession) {
        this.s3Config = s3Config;
        this.sparkSession = sparkSession;
    }

    @Timed(value = "dbrepo_data_get_subset_data", description = "Time spent getting data from subset", histogram = true)
    public String getSubset(Database database, String query, String format) throws QueryMalformedException,
            TableNotFoundException {
        try {
            final long start = System.currentTimeMillis();
            log.trace("get data via query: {}", query);
            final org.apache.spark.sql.Dataset<Row> dataset = sparkSession.read()
                    .format("jdbc")
                    .option("user", database.getContainer().getUsername())
                    .option("password", database.getContainer().getPassword())
                    .option("url", getSparkJdbcUrl(database))
                    .option("query", query)
                    .load();
            final String key = UUID.randomUUID()
                    .toString();
            final String path = "s3a://" + s3Config.getS3Bucket() + "/" + key;
            dataset.write()
                    .format(format)
                    .mode(SaveMode.ErrorIfExists)
                    .save(path);
            log.atDebug()
                    .setMessage("write " + format + " data to path: " + path)
                    .addKeyValue(Constants.FORMAT, format)
                    .addKeyValue(Constants.S3_KEY, key)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "jdbc_get_data")
                    .log();
            return key;
        } catch (Exception e) {
            if (e instanceof ExtendedAnalysisException && e.getMessage().contains("TABLE_OR_VIEW_NOT_FOUND")
                    || e instanceof SQLSyntaxErrorException && e.getMessage().contains("doesn't exist")) {
                log.atError()
                        .setMessage("Failed to find named reference")
                        .setCause(e)
                        .log();
                throw new TableNotFoundException("Failed to find named reference: " + e.getMessage()) /* remove throwable on purpose, clutters the output */;
            }
            log.error("Malformed query: {}", e.getMessage());
            throw new QueryMalformedException("Malformed query: " + e.getMessage(), e);
        }
    }

    @Override
    public Dataset<Row> getCsv(List<String> columns, String key, String delimiter, Boolean withHeader)
            throws StorageNotFoundException, StorageUnavailableException, MalformedException, TableMalformedException {
        final String path = "s3a://" + s3Config.getS3Bucket() + "/" + key + "/";
        Dataset<Row> dataset;
        try {
            dataset = (Dataset<Row>) sparkSession.read()
                    .option("delimiter", delimiter)
                    .option("header", withHeader)
                    .csv(path)
                    .toDF(columns.toArray(new String[0]));
            log.atDebug()
                    .setMessage("read dataset from path: " + path)
                    .addKeyValue(Constants.FORMAT, "csv")
                    .addKeyValue(Constants.S3_KEY, key)
                    .addKeyValue(Constants.S3_BUCKET, s3Config.getS3Bucket())
                    .addKeyValue("header", withHeader)
                    .log();
        } catch (Exception e) {
            if (e instanceof AnalysisException) {
                final AnalysisException exception = (AnalysisException) e;
                if (exception.getSimpleMessage().contains("PATH_NOT_FOUND")) {
                    log.atError()
                            .setMessage("Failed to find dataset " + key + " in storage service")
                            .addKeyValue(Constants.S3_KEY, key)
                            .setCause(e)
                            .log();
                    throw new StorageNotFoundException("Failed to find dataset in storage service: " + e.getMessage());
                }
                if (exception.getSimpleMessage().contains("UNRESOLVED_COLUMN")) {
                    log.atError()
                            .setMessage("Failed to resolve column from dataset in database")
                            .addKeyValue(Constants.S3_KEY, key)
                            .setCause(e)
                            .log();
                    throw new TableMalformedException("Failed to resolve column from dataset in database: " + e.getMessage());
                }
            } else if (e instanceof SparkException) {
                final SparkException exception = (SparkException) e;
                if (exception.getMessage().contains("FAILED_READ_FILE")) {
                    log.atError()
                            .setMessage("Failed to read dataset " + key + ": " + e.getMessage())
                            .addKeyValue(Constants.S3_KEY, key)
                            .setCause(e)
                            .log();
                    throw new StorageUnavailableException("Failed to read dataset " + key + ": " + e.getMessage());
                }
            } else if (e instanceof IllegalArgumentException) {
                log.atError()
                        .setMessage("Failed to map columns: " + e.getMessage())
                        .addKeyValue(Constants.S3_KEY, key)
                        .setCause(e)
                        .log();
                throw new MalformedException("Failed to map columns: " + e.getMessage());
            }
            log.atError()
                    .setMessage("Failed to connect to storage service: " + e.getMessage())
                    .addKeyValue(Constants.S3_KEY, key)
                    .setCause(e)
                    .log();
            throw new StorageUnavailableException("Failed to connect to storage service: " + e.getMessage());
        }
        if (!withHeader) {
            log.atDebug()
                    .setMessage("no header provided: use table column names")
                    .addKeyValue("columns", columns)
                    .log();
            try {
                dataset = dataset.toDF(asScalaIteratorConverter(columns.iterator())
                        .asScala()
                        .toSeq());
            } catch (IllegalArgumentException e) {
                log.error("Failed to map column to scala sequence: {}", e.getMessage());
                throw new MalformedException("Failed to map column to scala sequence: " + e.getMessage(), e);
            }
        }
        /* determine header order in dataset */
        if (dataset.schema().fields().length != columns.size()) {
            log.error("Failed to transform dataset: field length {} and header length arrays differ {}", dataset.schema().fields().length, columns.size());
            throw new MalformedException("Failed to transform dataset: field length and order length arrays differ");
        }
        /* reorder */
        final List<Column> columnOrder = columns.stream()
                .map(Column::new)
                .toList();
        try {
            return dataset.select(columnOrder.toArray(new Column[0]));
        } catch (Exception e) {
            if (e instanceof ExtendedAnalysisException exception) {
                log.atError()
                        .setMessage("Failed to resolve column from dataset in database")
                        .addKeyValue(Constants.S3_KEY, key)
                        .setCause(e)
                        .log();
                throw new TableMalformedException("Failed to resolve column from dataset in database: " + exception.getSimpleMessage());
            }
            log.atError()
                    .setMessage("Failed to select columns from dataset")
                    .addKeyValue(Constants.S3_KEY, key)
                    .setCause(e)
                    .log();
            throw new MalformedException("Failed to select columns from dataset: " + e.getMessage());
        }
    }

    @Override
    public Dataset<Row> getSubset(Database database, String query) throws QueryMalformedException,
            TableNotFoundException {
        final String key = getSubset(database, query, "jdbc");
        final String path = "s3a://" + s3Config.getS3Bucket() + "/" + key + "/";
        final Dataset<Row> dataset = (Dataset<Row>) sparkSession.read()
                .json(path);
        log.atDebug()
                .setMessage("get subset as json from path: " + path)
                .addKeyValue(Constants.FORMAT, "json")
                .addKeyValue(Constants.S3_KEY, key)
                .log();
        return dataset;
    }

    @Override
    public Dataset<Row> getSubsetAsJson(Database database, String query) throws QueryMalformedException,
            TableNotFoundException {
        final String key = getSubset(database, query, "json");
        final String path = "s3a://" + s3Config.getS3Bucket() + "/" + key;
        final Dataset<Row> dataset = (Dataset<Row>) sparkSession.read()
                .json(path);
        log.atDebug()
                .setMessage("get subset as json from path: " + path)
                .addKeyValue(Constants.FORMAT, "json")
                .addKeyValue(Constants.S3_KEY, key)
                .log();
        return dataset;
    }

    @Override
    public Dataset<Row> getSubsetAsCsv(Database database, String query) throws QueryMalformedException,
            TableNotFoundException {
        final String key = getSubset(database, query, "csv");
        final String path = "s3a://" + s3Config.getS3Bucket() + "/" + key;
        final Dataset<Row> dataset = (Dataset<Row>) sparkSession.read()
                .csv(path);
        log.atDebug()
                .setMessage("get subset as csv from path: " + path)
                .addKeyValue(Constants.FORMAT, "csv")
                .addKeyValue(Constants.S3_KEY, key)
                .log();
        return dataset;
    }

}
