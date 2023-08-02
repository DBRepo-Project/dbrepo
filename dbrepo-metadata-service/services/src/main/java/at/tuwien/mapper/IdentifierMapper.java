package at.tuwien.mapper;

import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.entities.identifier.Identifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {DatabaseMapper.class})
public interface IdentifierMapper {

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    IdentifierBriefDto identifierToIdentifierBriefDto(Identifier data);

    @Mappings({
            @Mapping(target = "database.identifier", ignore = true)
    })
    IdentifierDto identifierToIdentifierDto(Identifier data);

}
