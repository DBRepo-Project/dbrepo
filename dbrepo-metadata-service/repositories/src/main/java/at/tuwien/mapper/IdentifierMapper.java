package at.tuwien.mapper;

import at.tuwien.api.identifier.*;
import at.tuwien.entities.identifier.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {DatabaseMapper.class})
public interface IdentifierMapper {

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    IdentifierBriefDto identifierToIdentifierBriefDto(Identifier data);

    @Mappings({
            @Mapping(target = "database.identifiers", ignore = true),
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

    Creator creatorDtoToCreator(CreatorDto data);

    @Mappings({
            @Mapping(target = "nameIdentifierSchemeUri", source = "nameIdentifierScheme", qualifiedByName = "nameSchemaMapper"),
            @Mapping(target = "affiliationIdentifierSchemeUri", source = "affiliationIdentifierScheme", qualifiedByName = "affiliationSchemaMapper"),
    })
    Creator creatorCreateDtoToCreator(CreatorSaveDto data);

    RelatedIdentifier relatedIdentifierCreateDtoToRelatedIdentifier(RelatedIdentifierSaveDto data);

    IdentifierType identifierTypeDtoToIdentifierType(IdentifierTypeDto data);

    default String identifierToLocationUrl(String baseUrl, Identifier data) {
        if (data.getType().equals(IdentifierType.SUBSET)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/query/" + data.getQueryId()+ "/info?pid=" + data.getId();
        } else if (data.getType().equals(IdentifierType.DATABASE)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/info?pid=" + data.getId();
        } else if (data.getType().equals(IdentifierType.VIEW)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/view/" + data.getViewId()+ "/info?pid=" + data.getId();
        } else if (data.getType().equals(IdentifierType.TABLE)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/table/" + data.getTableId()+ "/info?pid=" + data.getId();
        } else {
            return null;
        }
    }

    @Named("nameSchemaMapper")
    default String nameIdentifierSchemeToNameIdentifierSchemeUri(NameIdentifierSchemeTypeDto data) {
        if (data == null) {
            return null;
        }
        return switch (data) {
            case ROR -> "https://ror.org/";
            case ORCID -> "https://orcid.org/";
            case GRID -> "https://grid.ac/";
            case ISNI -> "https://grid.ac/institutes/";
        };
    }

    @Named("affiliationSchemaMapper")
    default String affiliationIdentifierSchemeTypeToAffiliationIdentifier(AffiliationIdentifierSchemeTypeDto data) {
        if (data == null) {
            return null;
        }
        return switch (data) {
            case ROR -> "https://ror.org/";
            case GRID -> "https://grid.ac/institutes/";
            case ISNI -> "https://isni.org/";
        };
    }

}
