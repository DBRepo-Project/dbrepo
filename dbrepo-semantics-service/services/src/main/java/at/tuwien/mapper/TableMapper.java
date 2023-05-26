package at.tuwien.mapper;

import at.tuwien.entities.database.table.columns.TableColumnKey;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface TableMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableMapper.class);

    default TableColumnKey toTableColumnKey(Long databaseId, Long tableId, Long columnId) {
        return TableColumnKey.builder()
                .cdbid(databaseId)
                .tid(tableId)
                .id(columnId)
                .build();
    }
}
