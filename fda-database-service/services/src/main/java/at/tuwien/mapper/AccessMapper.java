package at.tuwien.mapper;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.entities.database.AccessType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccessMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccessMapper.class);

    AccessTypeDto accessType(AccessType data);

    AccessType accessType(AccessTypeDto data);

}
