package at.tuwien.mapper;


import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.user.external.ExternalResultType;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.LinkedList;

@Mapper(componentModel = "spring", imports = {LinkedList.class, ExternalResultType.class})
public interface MetadataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataMapper.class);

    @Mappings({
            @Mapping(target = "attributes", ignore = true)
    })
    UserRepresentation userCreateDtoToUserRepresentation(UserCreateDto data);
}
