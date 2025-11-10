package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DuckDbMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DuckDbMapper.class);

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
        log.debug("mapped attach mysql statement: {}", statement);
        return statement.toString();
    }

    default String queryToRawDescribeQuery(String data) {
        final StringBuilder statement = new StringBuilder("USE mysqldb; DESCRIBE (")
                .append(data.replace("`", "\""))
                .append(");");
        log.debug("mapped describe statement: {}", statement);
        return statement.toString();
    }

}
