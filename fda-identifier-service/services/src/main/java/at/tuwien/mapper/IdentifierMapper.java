package at.tuwien.mapper;

import at.tuwien.api.identifier.*;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.*;
import org.mapstruct.Mapper;
import org.springframework.transaction.annotation.Transactional;

@Mapper(componentModel = "spring")
public interface IdentifierMapper {

    @Transactional
    IdentifierDto identifierToIdentifierDto(Identifier data);

    @Transactional
    Identifier identifierCreateDtoToIdentifier(IdentifierCreateDto data);

    /* keep */
    @Transactional
    RelatedIdentifierDto relatedIdentifierToRelatedIdentifierDto(RelatedIdentifier data);

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

    IdentifierType identifierTypeDtoToIdentifierType(IdentifierTypeDto data);

    default VisibilityType databaseToVisibilityType(Database data) {
        if (data.getIsPublic()) {
            return VisibilityType.EVERYONE;
        }
        return VisibilityType.SELF;
    }

}
