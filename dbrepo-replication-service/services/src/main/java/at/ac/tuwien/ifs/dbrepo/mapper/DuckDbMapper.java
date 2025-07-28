package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DuckDbMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DuckDbMapper.class);

    default String databaseDtoToRawAttachQuery(DatabaseDto data) {
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

    default String queryDtoToRawDescribeQuery(QueryDto data) {
        final StringBuilder statement = new StringBuilder("USE mysqldb; DESCRIBE (")
                .append(data.getQuery().replace("`", ""))
                .append(");");
        log.debug("mapped describe statement: {}", statement);
        return statement.toString();
    }

}
