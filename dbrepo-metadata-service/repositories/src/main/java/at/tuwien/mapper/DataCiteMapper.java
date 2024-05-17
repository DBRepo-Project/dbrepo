package at.tuwien.mapper;

import at.tuwien.api.datacite.doi.*;
import at.tuwien.entities.database.License;
import at.tuwien.entities.identifier.*;
import at.tuwien.utils.EnumToStringConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.springframework.context.annotation.Profile;

import java.util.LinkedList;
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
            @Mapping(target = "creators", source = "creators"),
    })
    DataCiteCreateDoi identifierToDataCiteCreateDoi(Identifier identifier);

    default DataCiteCreateDoi identifierToDataCiteCreateDoi(Identifier identifier, String url, String prefix,
                                                            DataCiteDoiEvent event) {
        return addParametersToCreateDoi(
                identifierToDataCiteCreateDoi(identifier),
                url,
                prefix,
                DataCiteDoiTypes.DATASET,
                event
        );
    }

    DataCiteDoiTitle identifierTitleToDataCiteDoiTitle(IdentifierTitle data);

    default DataCiteDoiTitle.Type titleTypeToDataCiteDoiTitleType(TitleType data) {
        if (data == null) {
            return null;
        }
        return switch (data) {
            case OTHER -> DataCiteDoiTitle.Type.OTHER;
            case TRANSLATED_TITLE -> DataCiteDoiTitle.Type.TRANSLATED_TITLE;
            case SUBTITLE -> DataCiteDoiTitle.Type.SUBTITLE;
            case ALTERNATIVE_TITLE -> DataCiteDoiTitle.Type.ALTERNATIVE_TITLE;
        };
    }

    default List<DataCiteDoiTitle> identifierToDataCiteDoiTitleList(Identifier data) {
        if (data.getTitles() == null) {
            return new LinkedList<>();
        }
        return data.getTitles()
                .stream()
                .map(this::identifierTitleToDataCiteDoiTitle)
                .toList();
    }

    DataCiteCreateDoi addParametersToCreateDoi(@MappingTarget DataCiteCreateDoi target, String url, String prefix,
                                               DataCiteDoiTypes types, DataCiteDoiEvent event);

    @Mappings({
            @Mapping(target = "rights", source = "identifier"),
            @Mapping(target = "rightsUri", source = "uri"),
    })
    DataCiteDoiRights licenseToDoiRights(License license);

    @Mappings({
            @Mapping(target = "name", expression = "java(data.getLastname() + \", \" + data.getFirstname())"),
            @Mapping(target = "givenName", source = "firstname"),
            @Mapping(target = "familyName", source = "lastname"),
            @Mapping(target = "nameType", expression = "java(nameTypeToDataCiteNameType(data.getNameType()))"),
            @Mapping(target = "affiliation", expression = "java(list(creatorToDoiCreatorAffiliation(data)))"),
            @Mapping(target = "nameIdentifier", expression = "java(list(creatorToDataCiteDoiCreatorNameIdentifier(data)))"),
    })
    DataCiteDoiCreator creatorToDoiCreator(Creator data);

    DataCiteDoiCreatorNameIdentifier creatorToDataCiteDoiCreatorNameIdentifier(Creator data);

    /* keep */
    default String nameIdentifierSchemeTypeToUri(NameIdentifierSchemeType data) {
        switch (data) {
            case ROR -> {
                return "https://ror.org/";
            }
            case ORCID -> {
                return "https://orcid.org/";
            }
            case ISNI -> {
                return "https://isni.org/isni/";
            }
            case GRID -> {
                return "https://www.grid.ac/";
            }
        }
        return null;
    }

    /* keep */
    default DataCiteNameType nameTypeToDataCiteNameType(NameType data) {
        if (data == null) {
            return null;
        }
        return DataCiteNameType.valueOf(data.toString());
    }

    @Mappings({
            @Mapping(target = "name", source = "affiliation"),
            @Mapping(target = "affiliationIdentifier", source = "affiliationIdentifier"),
            @Mapping(target = "affiliationScheme", source = "affiliationIdentifierScheme"),
            @Mapping(target = "schemeUri", source = "affiliationIdentifierSchemeUri"),
    })
    DataCiteDoiCreatorAffiliation creatorToDoiCreatorAffiliation(Creator data);

    @Mappings({
            @Mapping(target = "relatedIdentifier", source = "value"),
            @Mapping(target = "relatedIdentifierType", source = "type"),
            @Mapping(target = "relationType", source = "relation"),
    })
    DataCiteDoiRelatedIdentifier relatedIdentifierToDoiRelatedIdentifier(RelatedIdentifier relatedIdentifier);
}
