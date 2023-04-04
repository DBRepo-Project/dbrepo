package at.tuwien.mapper;

import at.tuwien.api.identifier.*;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.identifier.RelatedIdentifier;
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
