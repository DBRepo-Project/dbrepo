package at.tuwien.mapper;

import at.tuwien.api.identifier.*;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
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

    Creator creatorCreateDtoToCreator(CreatorCreateDto data);

    RelatedIdentifier relatedIdentifierCreateDtoToRelatedIdentifier(RelatedIdentifierCreateDto data);

    VisibilityType visibilityTypeDtoToVisibilityType(VisibilityTypeDto data);

}
