package at.tuwien.mapper;

import at.tuwien.api.database.ViewDto;
import org.mapstruct.Mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface QueryMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QueryMapper.class);

    default String findAllViewsQuery() {
        return "SELECT TABLE_NAME as internal_name FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'fda' AND TABLE_TYPE = 'VIEW';";
    }

    default List<ViewDto> resultSetToViewDtoList(ResultSet result) throws SQLException {
        log.trace("mapping result list to view result, result={}", result);
        final List<ViewDto> views = new LinkedList<>();
        while (result.next()) {
            final ViewDto view = ViewDto.builder()
                    .internalName(result.getString("internal_name"))
                    .build();
            views.add(view);
        }
        log.trace("mapped result list {} to view list {}", result, views);
        return views;
    }

}
