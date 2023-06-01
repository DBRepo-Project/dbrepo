package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnKey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring")
public interface TableMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableMapper.class);

    @Mappings({
            @Mapping(source = "tid", target = "tableId"),
            @Mapping(source = "cdbid", target = "databaseId"),
    })
    ColumnBriefDto tableColumnToColumnBriefDto(TableColumn data);

    @Mappings({
            @Mapping(source = "tid", target = "tableId"),
            @Mapping(source = "cdbid", target = "databaseId"),
    })
    ColumnDto tableColumnToColumnDto(TableColumn data);

    default TableColumnKey toTableColumnKey(Long databaseId, Long tableId, Long columnId) {
        return TableColumnKey.builder()
                .cdbid(databaseId)
                .tid(tableId)
                .id(columnId)
                .build();
    }
}
