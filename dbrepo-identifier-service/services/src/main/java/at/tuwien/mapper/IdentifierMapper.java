package at.tuwien.mapper;

import at.tuwien.api.identifier.*;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.identifier.RelatedIdentifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface IdentifierMapper {

    IdentifierDto identifierToIdentifierDto(Identifier data);

    @Mappings({
            @Mapping(target = "containerId", source = "cid"),
            @Mapping(target = "databaseId", source = "dbid"),
            @Mapping(target = "queryId", source = "qid"),
    })
    Identifier identifierCreateDtoToIdentifier(IdentifierCreateDto data);

    @Mappings({
            @Mapping(target = "containerId", source = "cid"),
            @Mapping(target = "databaseId", source = "dbid"),
            @Mapping(target = "queryId", source = "qid"),
    })
    Identifier identifierUpdateDtoToIdentifier(IdentifierUpdateDto data);

    /* keep */
    RelatedIdentifierDto relatedIdentifierToRelatedIdentifierDto(RelatedIdentifier data);

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    Creator creatorDtoToCreator(CreatorDto data);

    Creator creatorCreateDtoToCreator(CreatorCreateDto data);

    RelatedIdentifier relatedIdentifierCreateDtoToRelatedIdentifier(RelatedIdentifierCreateDto data);

    IdentifierType identifierTypeDtoToIdentifierType(IdentifierTypeDto data);

    default String identifierToLocationUrl(String baseUrl, Identifier data) {
        if (data.getType().equals(IdentifierType.SUBSET)) {
            return baseUrl + "/container/" + data.getContainerId() + "/database/" + data.getDatabaseId() + "/query/" + data.getQueryId();
        } else if (data.getType().equals(IdentifierType.DATABASE)) {
            return baseUrl + "/container/" + data.getContainerId() + "/database/" + data.getDatabaseId();
        } else {
            return null;
        }
    }

}
