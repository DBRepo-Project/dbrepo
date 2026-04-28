package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DuckDbMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DuckDbMapper.class);

    String stars = "********";

    default String databaseDtoToRawAttachQuery(Database data, String schema) {
        final StringBuilder statement = new StringBuilder("ATTACH 'host=")
                .append(data.getContainer().getHost())
                .append(" user=")
                .append(data.getContainer().getUsername())
                .append(" password=")
                .append(data.getContainer().getPassword())
                .append(" port=")
                .append(data.getContainer().getPort())
                .append(" dbname=")
                .append(data.getInternalName())
                .append("' AS postgres_db (TYPE postgres, READ_ONLY, SCHEMA '")
                .append(schema)
                .append("');");
        log.debug("mapped attach postgres statement: {}", statement.toString().replaceAll(data.getContainer().getPassword(), stars));
        return statement.toString();
    }

    default String queryToRawDescribeQuery(String data) {
        final StringBuilder statement = new StringBuilder("USE postgres_db; DESCRIBE (")
                .append(data.replace("`", "\""))
                .append(");");
        log.debug("mapped describe statement: {}", statement);
        return statement.toString();
    }

    default String queryToRawDescribeCsvQuery(String bucket, String key) {
        final StringBuilder statement = new StringBuilder("FROM sniff_csv('s3://")
                .append(bucket)
                .append("/")
                .append(key)
                .append("', sample_size=20480);");
        log.debug("mapped describe csv statement: {}", statement);
        return statement.toString();
    }

    default String queryToRawDescribeS3TableQuery(String bucket, String key) {
        final StringBuilder statement = new StringBuilder("DESCRIBE TABLE 's3://")
                .append(bucket)
                .append("/")
                .append(key)
                .append("';");
        log.debug("mapped describe csv statement: {}", statement);
        return statement.toString();
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
        log.debug("mapped set variable statement: {}", statement);
        return statement.toString();
    }

    default String queryToRawSetS3SecretQuery(String accessKey, String secretKey) {
        final StringBuilder statement = new StringBuilder("CREATE SECRET (TYPE s3, KEY_ID '")
                .append(accessKey)
                .append("', SECRET '")
                .append(secretKey)
                .append("');");
        log.debug("mapped set s3 secret statement: {}", statement.toString().replace(accessKey, stars).replace(secretKey, stars));
        return statement.toString();
    }

}
