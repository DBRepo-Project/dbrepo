package at.tuwien.mapper;

import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.entities.database.Database;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AmqpMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmqpMapper.class);

    default GrantVirtualHostPermissionsDto databasesToGrantVirtualHostPermissionsDto(List<Database> databases) {
        final String permissions;
        if (databases.size() == 0) {
            permissions = "";
        } else {
            permissions = "^(" + databases.stream()
                    .map(Database::getExchange)
                    .collect(Collectors.joining("|")) + ")$";
        }
        log.trace("mapped database count {} to permissions '{}'", databases.size(), permissions);
        return GrantVirtualHostPermissionsDto.builder()
                .configure("")
                .write(permissions)
                .read(permissions)
                .build();
    }

}
