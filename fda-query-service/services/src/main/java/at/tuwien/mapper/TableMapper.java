package at.tuwien.mapper;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.entities.database.table.Table;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableMapper {

    TableDto tableToTableDto(Table data);

}
