package at.tuwien.mapper;

import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AmqpMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmqpMapper.class);

    default GrantVirtualHostPermissionsDto defaultVirtualHostUserPermissions() {
        return GrantVirtualHostPermissionsDto.builder()
                .configure("")
                .read("")
                .write("")
                .build();
    }

}
