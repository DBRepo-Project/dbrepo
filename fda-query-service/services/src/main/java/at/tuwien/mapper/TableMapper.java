package at.tuwien.mapper;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.entities.database.table.Table;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TableMapper {

    /* keep */
    @Mapping(target = "constraints", ignore = true)
    TableDto tableToTableDto(Table data);

}
