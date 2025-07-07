package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.DuckDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.DataTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
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
    public SchemaAnalysisResultDto determineDataTypes(ImageDto image, String key) throws AnalyseDataTypesException,
            DatabaseUnavailableException, StorageNotFoundException, ColumnNotFoundException, ImageInvalidException {
        /* download sample from storage service */
        try (Connection connection = getDuckDbConnection()) {
            setup(connection);
            long start = System.currentTimeMillis();
            final PreparedStatement statement1 = connection.prepareStatement("FROM sniff_csv('s3://" + s3Config.getS3Bucket() + "/" + key + "');");
            final DuckDBResultSet resultSet1 = (DuckDBResultSet) statement1.executeQuery();
            final SchemaAnalysisResultDto schema = dataMapper.resultSetToSchemaAnalysisResult(resultSet1);
            statement1.close();
            /* detect schema */
            final PreparedStatement statement2 = connection.prepareStatement("DESCRIBE TABLE 's3://" + s3Config.getS3Bucket() + "/" + key + "';");
            final DuckDBResultSet resultSet2 = (DuckDBResultSet) statement2.executeQuery();
            log.atDebug()
                    .setMessage("determined data types of s3://" + s3Config.getS3Bucket() + "/" + key)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "determine_datatypes")
                    .log();
            final Map<String, ColumnAnalysisResultDto> constraints = dataMapper.resultSetToConstraintResult(resultSet2);
            resultSet2.close();
            for (int i = 0; i < schema.getColumns().size(); i++) {
                final ColumnAnalysisResultDto column = schema.getColumns()
                        .get(i);
                final ColumnAnalysisResultDto analysis = constraints.get(column.getName());
                column.setPrimaryKey(analysis.getPrimaryKey());
                column.setNullAllowed(analysis.getNullAllowed());
                final DataTypeDto dataType = dataMapper.imageDtoTypeNameToDataTypeDto(image, column.getDatatype());
                column.setD(dataType.getDDefault());
                column.setSize(dataType.getSizeDefault());
            }
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

    public SchemaAnalysisResultDto determineDataTypes(ImageDto image, SubsetDto subset) {
        return null;
    }

}
