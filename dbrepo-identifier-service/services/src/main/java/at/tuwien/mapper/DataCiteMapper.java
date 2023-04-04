package at.tuwien.mapper;

import at.tuwien.datacite.doi.*;
import at.tuwien.entities.database.License;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.utils.EnumToStringConverter;
import org.mapstruct.*;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile("doi")
@Mapper(componentModel = "spring", uses = EnumToStringConverter.class)
public interface DataCiteMapper {

    default <T> List<T> list(T t) {
        if (t == null) return null;
        return List.of(t);
    }

    @Mappings({
            @Mapping(target = "titles", source = "."),
            @Mapping(target = "publisher", source = "publisher"),
            @Mapping(target = "publicationYear", source = "publicationYear"),
            @Mapping(target = "publicationMonth", source = "publicationMonth"),
            @Mapping(target = "publicationDay", source = "publicationDay"),
            @Mapping(target = "language", source = "language"),
            @Mapping(target = "rightsList",
                    expression = "java(list(licenseToDoiRights(identifier.getLicense())))"),
            @Mapping(target = "creators", source = "creators"),
            @Mapping(target = "relatedIdentifiers", source = "related"),
    })
    DataCiteCreateDoi identifierToDataCiteCreateDoi(Identifier identifier);

    default DataCiteCreateDoi identifierToDataCiteCreateDoi(Identifier identifier, String url, String prefix) {
        return addParametersToCreateDoi(
                identifierToDataCiteCreateDoi(identifier),
                url,
                prefix,
                DataCiteDoiTypes.DATASET,
                DataCiteDoiEvent.PUBLISH
        );
    }

    DataCiteCreateDoi addParametersToCreateDoi(@MappingTarget DataCiteCreateDoi target,
                                  String url,
                                  String prefix,
                                  DataCiteDoiTypes types,
                                  DataCiteDoiEvent event);

    default List<DataCiteDoiTitle> identifierToDoiTitles(Identifier identifier) {
        return List.of(
                DataCiteDoiTitle.builder().title(identifier.getTitle()).build(),
                DataCiteDoiTitle.builder()
                        .title(identifier.getDescription())
                        .titleType(DataCiteDoiTitle.Type.SUBTITLE)
                        .build()
        );
    }

    @Mappings({
            @Mapping(target = "rights", source = "identifier"),
            @Mapping(target = "rightsUri", source = "uri"),
    })
    DataCiteDoiRights licenseToDoiRights(License license);

    @Mappings({
            @Mapping(target = "name", expression = "java(license.getLastname() + \", \" + license.getFirstname())"),
            @Mapping(target = "givenName", source = "firstname"),
            @Mapping(target = "familyName", source = "lastname"),
            @Mapping(target = "affiliation",
                    expression = "java(list(affiliationStringToDoiCreatorAffiliation(license.getAffiliation())))"),
            @Mapping(target = "nameIdentifiers",
                    expression = "java(list(orcidStringToDoiCreatorNameIdentifier(license.getOrcid())))"),
    })
    DataCiteDoiCreator creatorToDoiCreator(Creator license);

    @Mappings({
            @Mapping(target = "name", constant = "affiliation"),
    })
    DataCiteDoiCreatorAffiliation affiliationStringToDoiCreatorAffiliation(String affiliation);

    @Mappings({
            @Mapping(target = "schemeUri", constant = "https://orcid.org"),
            @Mapping(target = "nameIdentifier", expression = "java(\"https://orcid.org/\" + orcid)"),
            @Mapping(target = "nameIdentifierScheme", constant = "ORCID"),
    })
    DataCiteDoiCreatorNameIdentifier orcidStringToDoiCreatorNameIdentifier(String orcid);

    @Mappings({
            @Mapping(target = "relatedIdentifier", source = "value"),
            @Mapping(target = "relatedIdentifierType", source = "type"),
            @Mapping(target = "relationType", source = "relation"),
    })
    DataCiteDoiRelatedIdentifier relatedIdentifierToDoiRelatedIdentifier(RelatedIdentifier relatedIdentifier);
}
