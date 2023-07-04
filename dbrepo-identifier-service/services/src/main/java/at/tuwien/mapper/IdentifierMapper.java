package at.tuwien.mapper;

import at.tuwien.api.identifier.*;
import at.tuwien.entities.identifier.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IdentifierMapper {

    IdentifierDto identifierToIdentifierDto(Identifier data);

    @Mappings({
            @Mapping(target = "queryId", source = "qid"),
            @Mapping(target = "titles", ignore = true),
            @Mapping(target = "descriptions", ignore = true),
    })
    Identifier identifierCreateDtoToIdentifier(IdentifierCreateDto data);

    @Mappings({
            @Mapping(target = "queryId", source = "qid"),
    })
    Identifier identifierUpdateDtoToIdentifier(IdentifierUpdateDto data);

    IdentifierTitle identifierCreateTitleDtoToIdentifierTitle(IdentifierCreateTitleDto data);

    IdentifierDescription identifierCreateDescriptionDtoToIdentifierDescription(IdentifierCreateDescriptionDto data);

    IdentifierCreateDto identifierUpdateDtoToIdentifierCreateDto(IdentifierUpdateDto data);

    RelatedIdentifierDto relatedIdentifierToRelatedIdentifierDto(RelatedIdentifier data);

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    Creator creatorDtoToCreator(CreatorDto data);

    Creator creatorCreateDtoToCreator(CreatorCreateDto data);

    RelatedIdentifier relatedIdentifierCreateDtoToRelatedIdentifier(RelatedIdentifierCreateDto data);

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
