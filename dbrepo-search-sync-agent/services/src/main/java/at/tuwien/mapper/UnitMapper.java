
package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UnitMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UnitMapper.class);

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);

}
