package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.DataDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.DuckDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.DuckDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import lombok.extern.slf4j.Slf4j;
import org.duckdb.DuckDBResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AnalyseServiceDuckDbImpl extends DataConnector implements AnalyseService {

    private final S3Config s3Config;
    private final DataMapper dataMapper;
    private final DataDbConfig dataDbConfig;
    private final DuckDbConfig duckDbConfig;
    private final DuckDbMapper duckDbMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public AnalyseServiceDuckDbImpl(S3Config s3Config, DataMapper dataMapper, DataDbConfig dataDbConfig,
                                    DuckDbConfig duckDbConfig, DuckDbMapper duckDbMapper, MetadataMapper metadataMapper) {
        this.s3Config = s3Config;
        this.dataMapper = dataMapper;
        this.dataDbConfig = dataDbConfig;
        this.duckDbConfig = duckDbConfig;
        this.duckDbMapper = duckDbMapper;
        this.metadataMapper = metadataMapper;
    }

    public void setup(Connection connection) throws SQLException {
        connection.prepareStatement(duckDbMapper.queryToRawSetVariableQuery("extension_directory", duckDbConfig.getExtensionDirectory()))
                .execute();
        connection.prepareStatement(duckDbMapper.queryToRawLoadExtensionQuery("httpfs"))
                .execute();
        connection.prepareStatement(duckDbMapper.queryToRawLoadExtensionQuery("postgres"))
                .execute();
        connection.prepareStatement(duckDbMapper.queryToRawSetVariableQuery("s3_endpoint", s3Config.getS3Endpoint().replaceAll("https?://", "")))
                .execute();
        connection.prepareStatement(duckDbMapper.queryToRawSetVariableQuery("s3_use_ssl", duckDbConfig.getS3UseSsl()))
                .execute();
        connection.prepareStatement(duckDbMapper.queryToRawSetVariableQuery("s3_url_style", duckDbConfig.getS3UrlStyle()))
                .execute();
        /* https://duckdb.org/docs/stable/guides/performance/how_to_tune_workloads.html#larger-than-memory-workloads-out-of-core-processing */
        connection.prepareStatement(duckDbMapper.queryToRawSetVariableQuery("temp_directory", duckDbConfig.getTmpDirectory()))
                .execute();
        connection.prepareStatement(duckDbMapper.queryToRawSetS3SecretQuery(s3Config))
                .execute();
    }

    @Override
    public SchemaAnalysisResultDto determineS3CsvDataTypes(Image image, String key) throws AnalyseDataTypesException,
            DatabaseUnavailableException, StorageNotFoundException, ColumnNotFoundException, ImageInvalidException {
        /* download sample from storage service */
        try (Connection connection = getDuckDbConnection()) {
            setup(connection);
            final PreparedStatement statement1 = connection.prepareStatement(duckDbMapper.queryToRawDescribeCsvQuery(s3Config.getS3Bucket(), key));
            final DuckDBResultSet resultSet1 = (DuckDBResultSet) statement1.executeQuery();
            final SchemaAnalysisResultDto schema = dataMapper.resultSetToSchemaAnalysisResult(resultSet1);
            log.debug("determined csv schema of: s3://{}/{}", s3Config.getS3Bucket(), key);
            statement1.close();
            /* detect schema */
            final PreparedStatement statement2 = connection.prepareStatement(duckDbMapper.queryToRawDescribeS3TableQuery(s3Config.getS3Bucket(), key));
            final DuckDBResultSet resultSet2 = (DuckDBResultSet) statement2.executeQuery();
            log.debug("determined data types of s3://" + s3Config.getS3Bucket() + "/" + key);
            final Map<String, ColumnAnalysisResultDto> constraints = dataMapper.resultSetToConstraintResult(resultSet2);
            resultSet2.close();
            for (int i = 0; i < schema.getColumns().size(); i++) {
                final ColumnAnalysisResultDto column = schema.getColumns()
                        .get(i);
                final ColumnAnalysisResultDto analysis = constraints.get(column.getName());
                column.setPrimaryKey(analysis.getPrimaryKey());
                column.setNullAllowed(analysis.getNullAllowed());
                final DataType dataType = metadataMapper.imageDtoTypeNameToDataTypeDto(image, column.getDatatype().getType().toLowerCase());
                column.setD(dataType.getDDefault());
                column.setSize(dataType.getSizeDefault());
            }
            log.debug("Determined schema: {}", schema);
            log.info("Determined schema of {} column(s)", schema.getColumns());
            return schema;
        } catch (SQLException e) {
            if (e.getMessage().contains("404")) {
                log.error("Failed to determine data types: not found: {}", e.getMessage());
                throw new StorageNotFoundException("Failed to determine data types: the dataset " + key + " was not found", e);
            }
            if (e.getMessage().contains("not find column")) {
                log.error("Failed to determine data types: column not found: {}", e.getMessage());
                throw new ColumnNotFoundException("Failed to determine data types: column not found", e);
            }
            log.error("Failed to determine data types: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to determine data types: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, ColumnAnalysisResultDto> determineDataTypes(Database database, QueryDto subset)
            throws ColumnNotFoundException, DatabaseUnavailableException {
        return determineDataTypes(database, duckDbMapper.queryToRawDescribeQuery(subset.getQuery()));
    }

    @Override
    public Map<String, ColumnAnalysisResultDto> determineDataTypes(Database database, String statement)
            throws ColumnNotFoundException, DatabaseUnavailableException {
        try (Connection connection = getDuckDbConnection()) {
            final long start = System.currentTimeMillis();
            setup(connection);
            /* attach to mariadb in duckdb */
            final PreparedStatement statement1 = connection.prepareStatement(duckDbMapper.databaseDtoToRawAttachQuery(
                    database, dataDbConfig.getDefaultSchema()));
            statement1.executeUpdate();
            statement1.close();
            final PreparedStatement statement2 = connection.prepareStatement(statement);
            final DuckDBResultSet resultSet2 = (DuckDBResultSet) statement2.executeQuery();
            final Map<String, ColumnAnalysisResultDto> schema = dataMapper.resultSetToConstraintResult(resultSet2);
            statement2.close();
            log.atDebug()
                    .setMessage("determined schema data types for statement in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "determine_datatypes")
                    .log();
            return schema;
        } catch (SQLException e) {
            if (e.getMessage().contains("not find column")) {
                log.error("Failed to determine data types: column not found: {}", e.getMessage());
                throw new ColumnNotFoundException("Failed to determine data types: column not found", e);
            }
            log.error("Failed to determine data types: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to determine data types: " + e.getMessage(), e);
        }
    }

}
