package at.tuwien.mapper;

import at.tuwien.api.identifier.*;
import at.tuwien.entities.identifier.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IdentifierMapper {

    @Mappings({
            @Mapping(target = "database.identifier", ignore = true),
    })
    IdentifierDto identifierToIdentifierDto(Identifier data);

    @Mappings({
            @Mapping(target = "titles", ignore = true),
            @Mapping(target = "descriptions", ignore = true),
    })
    Identifier identifierCreateDtoToIdentifier(IdentifierSaveDto data);

    Identifier identifierUpdateDtoToIdentifier(IdentifierSaveDto data);

    IdentifierTitle identifierCreateTitleDtoToIdentifierTitle(IdentifierSaveTitleDto data);

    IdentifierDescription identifierCreateDescriptionDtoToIdentifierDescription(IdentifierSaveDescriptionDto data);

    IdentifierFunder identifierFunderSaveDtoToIdentifierFunder(IdentifierFunderSaveDto data);

    IdentifierSaveDto identifierUpdateDtoToIdentifierCreateDto(IdentifierSaveDto data);

    RelatedIdentifierDto relatedIdentifierToRelatedIdentifierDto(RelatedIdentifier data);

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    Creator creatorDtoToCreator(CreatorDto data);

    Creator creatorCreateDtoToCreator(CreatorSaveDto data);

    RelatedIdentifier relatedIdentifierCreateDtoToRelatedIdentifier(RelatedIdentifierSaveDto data);

    IdentifierType identifierTypeDtoToIdentifierType(IdentifierTypeDto data);

    default String identifierToLocationUrl(String baseUrl, Identifier data) {
        if (data.getType().equals(IdentifierType.SUBSET)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/query/" + data.getQueryId();
        } else if (data.getType().equals(IdentifierType.DATABASE)) {
            return baseUrl + "/database/" + data.getDatabase().getId();
        } else {
            return null;
        }
    }

}
