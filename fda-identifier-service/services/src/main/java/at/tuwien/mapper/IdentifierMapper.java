package at.tuwien.mapper;

import at.tuwien.api.identifier.CreatorDto;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.VisibilityTypeDto;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.VisibilityType;
import org.mapstruct.Mapper;
import org.springframework.transaction.annotation.Transactional;

@Mapper(componentModel = "spring")
public interface IdentifierMapper {

    @Transactional(readOnly = true)
    IdentifierDto identifierToIdentifierDto(Identifier data);

    @Transactional(readOnly = true)
    Identifier identifierCreateDtoToIdentifier(IdentifierCreateDto data);

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    Creator creatorDtoToCreator(CreatorDto data);

    VisibilityType visibilityTypeDtoToVisibilityType(VisibilityTypeDto data);

}
