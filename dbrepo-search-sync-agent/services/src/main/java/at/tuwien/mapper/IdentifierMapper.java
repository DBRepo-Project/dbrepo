package at.tuwien.mapper;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.entities.identifier.Identifier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IdentifierMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IdentifierMapper.class);

    IdentifierDto identifierToIdentifierDto(Identifier data);

}
