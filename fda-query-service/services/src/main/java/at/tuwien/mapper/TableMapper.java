package at.tuwien.mapper;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.entities.database.table.Table;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableMapper {

    /* keep */
    TableDto tableToTableDto(Table data);

}
