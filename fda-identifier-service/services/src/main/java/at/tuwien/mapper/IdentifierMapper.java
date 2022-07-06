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

    @Transactional
    IdentifierDto identifierToIdentifierDto(Identifier data);

    @Transactional
    Identifier identifierCreateDtoToIdentifier(IdentifierCreateDto data);

    @Transactional
    Identifier identifierDtoToIdentifier(IdentifierDto data);

    @Transactional
    Creator creatorDtoToCreator(CreatorDto data);

    @Transactional
    Creator creatorCreateDtoToCreator(CreatorCreateDto data);

    @Transactional
    RelatedIdentifier relatedIdentifierCreateDtoToRelatedIdentifier(RelatedIdentifierCreateDto data);

    @Transactional
    VisibilityType visibilityTypeDtoToVisibilityType(VisibilityTypeDto data);

}
