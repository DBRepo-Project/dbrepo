package at.tuwien.mapper;

import at.tuwien.entities.database.Database;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AmqpMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmqpMapper.class);

    default String databaseListToPermissionString(List<Database> databases) {
        final String permissions;
        if (databases.size() == 0) {
            permissions = "";
        } else {
            permissions = "^(" + databases.stream()
                    .map(Database::getExchangeName)
                    .collect(Collectors.joining("|")) + ")$";
        }
        log.trace("mapped databases {} to permissions '{}'", databases.stream().map(Database::getId).toList(), permissions);
        return permissions;
    }

}
