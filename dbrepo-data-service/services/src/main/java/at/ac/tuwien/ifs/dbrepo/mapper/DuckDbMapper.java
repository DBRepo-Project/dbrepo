package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DuckDbMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DuckDbMapper.class);

    String stars = "********";

    default String databaseDtoToRawAttachQuery(Database data) {
        final StringBuilder statement = new StringBuilder("ATTACH 'host=")
                .append(data.getContainer().getHost())
                .append(" user=")
                .append(data.getContainer().getUsername())
                .append(" password=")
                .append(data.getContainer().getPassword())
                .append(" port=")
                .append(data.getContainer().getPort())
                .append(" database=")
                .append(data.getInternalName())
                .append("' AS mysqldb (TYPE mysql);");
        log.debug("mapped database attachment for database {}", data.getId());
        return statement.toString();
    }

    default String queryToRawDescribeQuery(String data) {
        final StringBuilder statement = new StringBuilder("USE mysqldb; DESCRIBE (")
                .append(data.replace("`", "\""))
                .append(");");
        log.debug("mapped describe statement: {}", statement);
        return statement.toString();
    }

    default String queryToRawDescribeCsvQuery() {
        return "FROM sniff_csv(?, sample_size=20480);";
    }

    default String queryToRawDescribeS3TableQuery() {
        return "DESCRIBE SELECT * FROM read_csv_auto(?);";
    }

    default String queryToRawLoadExtensionQuery(String data) {
        final StringBuilder statement = new StringBuilder("LOAD '")
                .append(data)
                .append("';");
        log.debug("mapped load extension statement: {}", statement);
        return statement.toString();
    }

    default String queryToRawSetVariableQuery(String key, String value) {
        final StringBuilder statement = new StringBuilder("SET ")
                .append(key)
                .append(" = '")
                .append(value)
                .append("';");
        log.debug("mapped setting for variable {}", key);
        return statement.toString();
    }

    default String queryToRawSetS3SecretQuery(String accessKey, String secretKey) {
        final StringBuilder statement = new StringBuilder("CREATE SECRET (TYPE s3, KEY_ID '")
                .append(accessKey)
                .append("', SECRET '")
                .append(secretKey)
                .append("');");
        log.debug("mapped S3 credential setup");
        return statement.toString();
    }

}
