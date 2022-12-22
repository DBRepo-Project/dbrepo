package at.tuwien.mapper;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.entities.database.AccessType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccessMapper {

    AccessTypeDto accessType(AccessType data);

    AccessType accessType(AccessTypeDto data);

}
