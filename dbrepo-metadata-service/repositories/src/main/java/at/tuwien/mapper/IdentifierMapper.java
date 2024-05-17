package at.tuwien.mapper;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.identifier.*;
import at.tuwien.api.identifier.ld.LdCreatorDto;
import at.tuwien.api.identifier.ld.LdDatasetDto;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.License;
import at.tuwien.entities.identifier.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring", uses = {DatabaseMapper.class})
public interface IdentifierMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IdentifierMapper.class);

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    @Mappings({
            @Mapping(target = "databaseId", source = "database.id"),
    })
    IdentifierDto identifierToIdentifierDto(Identifier data);

    IdentifierBriefDto identifierToIdentifierBriefDto(Identifier data);

    default IdentifierTitle identifierToIdentifierTitle(Identifier data, String lang) {
        final Optional<IdentifierTitle> optional = data.getTitles()
                .stream()
                .filter(t -> lang == null || t.getLanguage().getName().equals(lang))
                .findFirst();
        if (optional.isEmpty()) {
            log.warn("no title with language {} found", lang);
            return identifierToIdentifierTitle(data, "en");
        }
        return optional.get();
    }

    default IdentifierDescription identifierToIdentifierDescription(Identifier data, String lang) {
        final Optional<IdentifierDescription> optional = data.getDescriptions()
                .stream()
                .filter(t -> lang == null || t.getLanguage().getName().equals(lang))
                .findFirst();
        if (optional.isEmpty()) {
            log.warn("no description with language {} found", lang);
            return identifierToIdentifierDescription(data, "en");
        }
        return optional.get();
    }

    @Mappings({
            @Mapping(target = "givenName", source = "firstname"),
            @Mapping(target = "familyName", source = "lastname"),
            @Mapping(target = "type", expression = "java(data.getNameType().equals(NameType.PERSONAL) ? \"Person\" : \"Organization\")"),
            @Mapping(target = "sameAs", source = "nameIdentifier"),
            @Mapping(target = "name", source = "creatorName"),
    })
    LdCreatorDto creatorToLdCreatorDto(Creator data);

    default LdDatasetDto identifierToLdDatasetDto(Identifier data, String baseUrl) {
        return LdDatasetDto.builder()
                .context("https://schema.org/")
                .type("Dataset")
                .name(identifierToIdentifierTitle(data, null)
                        .getTitle())
                .description(identifierToIdentifierDescription(data, null)
                        .getDescription())
                .url(identifierToLocationUrl(baseUrl, data))
                .identifier(List.of())
                .creator(data.getCreators()
                        .stream()
                        .map(this::creatorToLdCreatorDto)
                        .toList())
                .citation(identifierToLocationUrl(baseUrl, data))
                .hasPart(List.of())
                .license(data.getLicenses().isEmpty() ? null : data.getLicenses().get(0).getUri())
                .temporalCoverage(null)
                .version(data.getCreated())
                .build();
    }

    Identifier identifierCreateDtoToIdentifier(IdentifierCreateDto data);

    Identifier identifierUpdateDtoToIdentifier(IdentifierSaveDto data);

    LanguageType languageTypeDtoToLanguageType(LanguageTypeDto data);

    License licenseDtoToLicense(LicenseDto data);

    IdentifierTitle identifierCreateTitleDtoToIdentifierTitle(IdentifierSaveTitleDto data);

    IdentifierDescription identifierCreateDescriptionDtoToIdentifierDescription(IdentifierSaveDescriptionDto data);

    IdentifierFunder identifierFunderSaveDtoToIdentifierFunder(IdentifierFunderSaveDto data);

    IdentifierSaveDto identifierCreateDtoToIdentifierSaveDto(IdentifierCreateDto data);

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
            return baseUrl + "/database/" + data.getDatabase().getId() + "/subset/" + data.getQueryId() + "/info?pid=" + data.getId();
        } else if (data.getType().equals(IdentifierType.DATABASE)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/info?pid=" + data.getId();
        } else if (data.getType().equals(IdentifierType.VIEW)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/view/" + data.getViewId() + "/info?pid=" + data.getId();
        } else if (data.getType().equals(IdentifierType.TABLE)) {
            return baseUrl + "/database/" + data.getDatabase().getId() + "/table/" + data.getTableId() + "/info?pid=" + data.getId();
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
