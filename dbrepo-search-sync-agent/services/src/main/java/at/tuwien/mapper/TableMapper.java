package at.tuwien.mapper;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TableMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableMapper.class);

    @Mappings({
            @Mapping(target = "database.identifier", ignore = true)
    })
    TableDto tableToTableDto(Table data);

    ColumnDto tableColumnToColumnDto(TableColumn data);

    default List<ColumnDto> uniqueToColumnList(Unique unique) {
        return unique.getColumns().stream().map(this::tableColumnToColumnDto).collect(Collectors.toList());
    }

}