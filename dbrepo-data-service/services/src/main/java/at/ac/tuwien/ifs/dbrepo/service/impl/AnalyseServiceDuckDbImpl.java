package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.DuckDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AnalyseDataTypesException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import lombok.extern.slf4j.Slf4j;
import org.duckdb.DuckDBResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

@Slf4j
@Service
public class AnalyseServiceDuckDbImpl extends DataConnector implements AnalyseService {

    private final S3Config s3Config;
    private final DataMapper dataMapper;
    private final DuckDbConfig duckDbConfig;

    @Autowired
    public AnalyseServiceDuckDbImpl(S3Config s3Config, DataMapper dataMapper, DuckDbConfig duckDbConfig) {
        this.s3Config = s3Config;
        this.dataMapper = dataMapper;
        this.duckDbConfig = duckDbConfig;
    }

    @Override
    public void setup(Connection connection) throws SQLException {
        connection.prepareStatement("LOAD httpfs;")
                .execute();
        connection.prepareStatement("SET s3_endpoint = '" + s3Config.getS3Endpoint().replaceAll("https?://", "") + "';")
                .execute();
        connection.prepareStatement("SET s3_use_ssl = " + s3Config.getS3UseSsl() + ";")
                .execute();
        connection.prepareStatement("SET s3_url_style = '" + s3Config.getS3UrlStyle() + "';")
                .execute();
        /* https://duckdb.org/docs/stable/guides/performance/how_to_tune_workloads.html#larger-than-memory-workloads-out-of-core-processing */
        connection.prepareStatement("SET temp_directory = '" + duckDbConfig + "';")
                .execute();
        connection.prepareStatement("CREATE SECRET (TYPE s3, KEY_ID '" + s3Config.getS3AccessKeyId() + "', SECRET '" + s3Config.getS3SecretAccessKey() + "');")
                .execute();
    }

    @Override
    public SchemaAnalysisResultDto determineDataTypes(String key) throws AnalyseDataTypesException,
            DatabaseUnavailableException, StorageNotFoundException {
        /* download sample from storage service */
        try (Connection connection = getDuckDbConnection()) {
            setup(connection);
            long start = System.currentTimeMillis();
            final PreparedStatement statement1 = connection.prepareStatement("FROM sniff_csv('s3://" + s3Config.getS3Bucket() + "/" + key + "');");
            final DuckDBResultSet resultSet1 = (DuckDBResultSet) statement1.executeQuery();
            final SchemaAnalysisResultDto schema = dataMapper.resultSetToSchemaAnalysisResult(resultSet1);
            statement1.close();
            /* create schema */
            final PreparedStatement statement2 = connection.prepareStatement("CREATE TABLE _tmp AS FROM sniff_csv('s3://" + s3Config.getS3Bucket() + "/" + key + "');");
            statement2.execute();
            statement2.close();
            /* detect schema */
            final PreparedStatement statement3 = connection.prepareStatement("DESCRIBE _tmp;");
            final DuckDBResultSet resultSet3 = (DuckDBResultSet) statement3.executeQuery();
            log.atDebug()
                    .setMessage("determined data types of s3://" + s3Config.getS3Bucket() + "/" + key)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "determine_datatypes")
                    .log();
            final Map<String, ColumnAnalysisResultDto> constraints = dataMapper.resultSetToConstraintResult(resultSet3);
            statement3.close();
            schema.getColumns()
                    .forEach(column ->
                            column.setNullAllowed(constraints.getOrDefault(column.getName(), ColumnAnalysisResultDto.builder()
                                            .nullAllowed(true)
                                            .build())
                                    .getNullAllowed()));
            return schema;
        } catch (SQLException e) {
            if (e.getMessage().contains("404")) {
                log.error("Failed to determine data types: not found: {}", e.getMessage());
                throw new StorageNotFoundException("Failed to determine data types: the dataset " + key + " was not found", e);
            }
            log.error("Failed to determine data types: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to determine data types: " + e.getMessage(), e);
        }
    }

}
