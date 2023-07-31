package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.entities.database.Database;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface DatabaseMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseMapper.class);

    DatabaseDto databaseToDatabaseDto(Database data);

    @Mappings({
            @Mapping(target = "cid", source = "container.id"),
            @Mapping(target = "exchangeName", expression = "java(\"dbrepo.\" + data.getInternalName())")
    })
    Database databaseBriefDtoToDatabase(DatabaseBriefDto data);

}
