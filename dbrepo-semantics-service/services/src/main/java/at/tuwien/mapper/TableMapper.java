package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.entities.database.table.columns.TableColumn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface TableMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableMapper.class);

    @Mappings({
            @Mapping(target = "tableId", source = "table.id"),
    })
    ColumnBriefDto tableColumnToColumnBriefDto(TableColumn data);

    @Mappings({
            @Mapping(target = "tableId", source = "table.id"),
    })
    ColumnDto tableColumnToColumnDto(TableColumn data);
}
