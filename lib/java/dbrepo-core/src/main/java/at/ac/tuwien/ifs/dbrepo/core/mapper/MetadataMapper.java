package at.ac.tuwien.ifs.dbrepo.core.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.CreateContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.DataTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.crossref.CrossRefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary.PrimaryKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.*;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.PermissionTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld.LdCreatorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld.LdDatasetDto;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.UserCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.OrcidDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.OrcidActivitiesSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.OrcidEmploymentsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.OrcidAffiliationGroupDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.OrcidEmploymentSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.OrcidSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization.OrcidOrganizationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedSourceTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.OrcidPersonDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.name.OrcidNameDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.name.OrcidValueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.ror.RorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.EntityDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.OntologyBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.OntologyCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.OntologyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalMetadataDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalResultType;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.affiliation.ExternalAffiliationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.DataType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnConcept;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnUnit;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKeyReference;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ReferenceType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.primaryKey.PrimaryKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessage;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessageType;
import at.ac.tuwien.ifs.dbrepo.core.entity.semantics.Ontology;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {LinkedList.class, ExternalResultType.class, DataCiteDoiTypes.class},
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface MetadataMapper {

    Logger log = LoggerFactory.getLogger(MetadataMapper.class);

    @Mappings({
            @Mapping(target = "dMin", source = "DMin"),
            @Mapping(target = "dMax", source = "DMax"),
            @Mapping(target = "dDefault", source = "DDefault"),
            @Mapping(target = "dRequired", source = "DRequired")
    })
    DataTypeDto dataTypeToDataTypeDto(DataType data);

    default DatabaseGrantsDto grantsToDatabaseGrantDto(Set<String> grants, String grantDefaultRead,
                                                       String grantDefaultWrite) {
        final Set<String> read = Arrays.asList(grantDefaultRead.split(","))
                .stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        final Set<String> write = Arrays.asList(grantDefaultWrite.split(","))
                .stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        grants = grants.stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(g -> !g.equals("USAGE"))
                .collect(Collectors.toSet());
        final GrantTypeDto type;
        if (write.containsAll(grants) && grants.containsAll(write)) {
            type = GrantTypeDto.WRITE;
        } else if (read.containsAll(grants) && grants.containsAll(read)) {
            type = GrantTypeDto.READ;
        } else {
            type = null;
        }
        return DatabaseGrantsDto.builder()
                .grants(grants)
                .type(type)
                .build();
    }

    @Mappings({
            @Mapping(target = "databaseName", source = "internalName"),
            @Mapping(target = "ownerUsername", source = "owner.username")
    })
    CreateDashboardDto databaseToCreateDashboardDto(Database database);

    @Mappings({
            @Mapping(target = "id", ignore = true), /* id attribute is ignored by the library anyway, just making it explicit */
            @Mapping(target = "attributes", ignore = true)
    })
    UserRepresentation userCreateDtoToUserRepresentation(UserCreateDto data);

    @Mappings({
            @Mapping(target = "accessToken", source = "token")
    })
    TokenDto accessTokenResponseToTokenDto(AccessTokenResponse data);

    BannerMessageDto bannerMessageToBannerMessageDto(BannerMessage data);

    BannerMessageBriefDto bannerMessageToBannerMessageBriefDto(BannerMessage data);

    ViewColumn viewColumnDtoToViewColumn(ViewColumnDto data);

    BannerMessage bannerMessageCreateDtoToBannerMessage(BannerMessageCreateDto data);

    BannerMessageType bannerMessageTypeDtoToBannerMessageType(BannerMessageTypeDto data);

    @Mappings({
            @Mapping(target = "internalName", source = "name", qualifiedByName = "internalMapping")
    })
    Container containerCreateRequestDtoToContainer(CreateContainerDto data);

    UserUpdateDto userToUserUpdateDto(User data);

    default List<String> optionalValueToMap(String value) {
        final List<String> attr = new LinkedList<>();
        if (value != null) {
            attr.add(value);
        }
        return attr;
    }

    ContainerDto containerToContainerDto(Container data);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "count", expression = "java(data.getDatabases().size())"),
    })
    ContainerBriefDto containerToContainerBriefDto(Container data);

    @Mappings({
            @Mapping(target = "previewImage", expression = "java(database.getImage() != null ? \"/api/database/\" + database.getId() + \"/image\" : null)"),
            @Mapping(target = "accesses", expression = "java(database.getAccesses().stream().filter(a -> !a.getUser().getIsInternal()).map(a -> databaseAccessToDatabaseAccessDto(a)).toList())"),
            @Mapping(target = "replicaUrls", source = "replicaUrls", qualifiedByName = "replicaLocationListToMap")
    })
    DatabaseDto databaseToDatabaseDto(Database database);

    @Named("replicaLocationListToStringList")
    default List<String> replicaLocationListToStringList(List<ReplicaLocation> replicaLocations) {
        if (replicaLocations == null) {
            return new LinkedList<>();
        }
        return replicaLocations.stream()
                .map(ReplicaLocation::getUrl)
                .toList();
    }

    @Named("replicaLocationListToMap")
    default Map<String, UUID> replicaLocationListToMap(List<ReplicaLocation> replicaLocations) {
        if (replicaLocations == null) {
            return new LinkedHashMap<>();
        }
        return replicaLocations.stream()
                .filter(rl -> rl.getUrl() != null)
                .collect(
                        java.util.HashMap::new,
                        (m, rl) -> m.put(rl.getUrl(), rl.getReplicaDatabaseId()),
                        Map::putAll
                );
    }

    @Mappings({
            @Mapping(target = "types", expression = "java(DataCiteDoiTypes.DATASET)")
    })
    DataCiteCreateDoi identifierToDataCiteCreateDoi(Identifier identifier);

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

    @Mappings({
            @Mapping(target = "rights", source = "identifier"),
            @Mapping(target = "rightsUri", source = "uri"),
    })
    DataCiteDoiRights licenseToDoiRights(License license);

    default <T> List<T> list(T t) {
        if (t == null) return null;
        return List.of(t);
    }

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
        if (data == null) {
            return null;
        }
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
            @Mapping(target = "relatedIdentifierType", expression = "java(relatedIdentifier.getType().toString())"),
            @Mapping(target = "relationType", expression = "java(relatedIdentifier.getRelation().toString())")
    })
    DataCiteDoiRelatedIdentifier relatedIdentifierToDoiRelatedIdentifier(RelatedIdentifier relatedIdentifier);

    @Mappings({
            @Mapping(target = "givenNames", source = "person.name.givenNames.value"),
            @Mapping(target = "familyName", source = "person.name.familyName.value"),
            @Mapping(target = "type", expression = "java(ExternalResultType.PERSONAL)"),
            @Mapping(target = "affiliations", source = "activitiesSummary.employments.affiliationGroup"),
    })
    ExternalMetadataDto orcidDtoToExternalMetadataDto(OrcidDto data);

    default OrcidDto userToOrcidDto(User data) {
        return OrcidDto.builder()
                .person(OrcidPersonDto.builder()
                        .name(OrcidNameDto.builder()
                                .givenNames(OrcidValueDto.builder()
                                        .value(data.getFirstname())
                                        .build())
                                .familyName(OrcidValueDto.builder()
                                        .value(data.getLastname())
                                        .build())
                                .build())
                        .build())
                .activitiesSummary(OrcidActivitiesSummaryDto.builder()
                        .employments(OrcidEmploymentsDto.builder()
                                .affiliationGroup(new OrcidAffiliationGroupDto[]{
                                        OrcidAffiliationGroupDto.builder()
                                                .summaries(new OrcidEmploymentSummaryDto[]{
                                                        OrcidEmploymentSummaryDto.builder()
                                                                .employmentSummary(OrcidSummaryDto.builder()
                                                                        .organization(OrcidOrganizationDto.builder()
                                                                                .name(data.getAffiliation())
                                                                                .build())
                                                                        .build())
                                                                .build()
                                                })
                                                .build()
                                })
                                .build())
                        .build())
                .build();
    }

    @Mappings({
            @Mapping(target = "organizationName", source = "employmentSummary.organization.name"),
            @Mapping(target = "ringgoldId", expression = "java(disambiguatedOrganizationToRinggoldId(data.getEmploymentSummary().getOrganization().getDisambiguatedOrganization()))"),
    })
    ExternalAffiliationDto orcidEmploymentSummaryDtoToExternalAffiliationDto(OrcidEmploymentSummaryDto data);

    default ExternalAffiliationDto orcidAffiliationGroupDtoToExternalAffiliationDto(OrcidAffiliationGroupDto data) {
        if (data == null || data.getSummaries() == null || data.getSummaries().length == 0) {
            return null;
        }
        return ExternalAffiliationDto.builder()
                .organizationName(data.getSummaries()[0].getEmploymentSummary().getOrganization().getName())
                .build();
    }

    default Long disambiguatedOrganizationToRinggoldId(OrcidDisambiguatedDto data) {
        if (data.getSource().equals(OrcidDisambiguatedSourceTypeDto.RINGGOLD)) {
            return Long.parseLong(data.getIdentifier());
        }
        return null;
    }

    default PermissionTypeDto accessTypeDtoToPermissionTypeDto(AccessTypeDto data) {
        if (data == null) {
            return PermissionTypeDto.NONE;
        }
        return switch (data) {
            case READ -> PermissionTypeDto.VIEW;
            case WRITE_OWN, WRITE_ALL -> PermissionTypeDto.EDITOR;
        };
    }

    default ExternalMetadataDto rorDtoToExternalMetadataDto(RorDto data) {
        return ExternalMetadataDto.builder()
                .affiliations(new ExternalAffiliationDto[]{
                        ExternalAffiliationDto.builder()
                                .organizationName(data.getName())
                                .build()})
                .type(ExternalResultType.ORGANIZATIONAL)
                .build();
    }

    default ExternalMetadataDto crossrefDtoToExternalMetadataDto(CrossRefDto data) {
        return ExternalMetadataDto.builder()
                .affiliations(new ExternalAffiliationDto[]{
                        ExternalAffiliationDto.builder()
                                .crossrefFunderId(data.getId())
                                .organizationName(data.getPrefLabel().getLabel().getLiteralForm().getContent())
                                .build()})
                .type(ExternalResultType.ORGANIZATIONAL)
                .build();
    }

    @Mappings({
            @Mapping(target = "organizationName", source = "name"),
    })
    ExternalAffiliationDto rorDtoToExternalAffiliationDto(RorDto data);

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    @Mappings({
            @Mapping(target = "databaseId", source = "database.id"),
            @Mapping(target = "links", expression = "java(identifierToLinksDto(data))"),
    })
    IdentifierDto identifierToIdentifierDto(Identifier data);

    default LinksDto identifierToLinksDto(Identifier data) {
        final LinksDto links = LinksDto.builder()
                .self("/api/identifier/" + data.getId())
                .selfHtml("/pid/" + data.getId())
                .build();
        switch (data.getType()) {
            case VIEW ->
                    links.setData("/api/database/" + data.getDatabase().getId() + "/view/" + data.getViewId() + "/data");
            case TABLE ->
                    links.setData("/api/database/" + data.getDatabase().getId() + "/table/" + data.getTableId() + "/data");
            case SUBSET ->
                    links.setData("/api/database/" + data.getDatabase().getId() + "/subset/" + data.getQueryId() + "/data");
        }
        if (data.getDatabase().getIsDashboardEnabled()) {
            links.setDashboardHtml("/d/" + data.getDatabase().getDashboardUid());
        }
        return links;
    }

    @Mappings({
            @Mapping(target = "databaseId", source = "database.id"),
            @Mapping(target = "ownedBy", source = "owner.id")
    })
    IdentifierBriefDto identifierToIdentifierBriefDto(Identifier data);

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
                .name(data.getTitles().isEmpty() ? null : data.getTitles().get(0).getTitle())
                .description(data.getDescriptions().isEmpty() ? null : data.getDescriptions().get(0).getDescription())
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

    @Mappings({
            @Mapping(target = "nameIdentifierSchemeUri", source = "nameIdentifier", qualifiedByName = "identifierSchemeUriMapper"),
            @Mapping(target = "nameIdentifierScheme", source = "nameIdentifier", qualifiedByName = "nameIdentifierSchemeMapper"),
            @Mapping(target = "affiliationIdentifierSchemeUri", source = "affiliationIdentifier", qualifiedByName = "identifierSchemeUriMapper"),
            @Mapping(target = "affiliationIdentifierScheme", source = "affiliationIdentifier", qualifiedByName = "affiliationIdentifierSchemeMapper"),
    })
    Creator saveIdentifierCreatorDtoToCreator(SaveIdentifierCreatorDto data);

    IdentifierTitle saveIdentifierTitleDtoToIdentifierTitle(SaveIdentifierTitleDto data);

    IdentifierDescription saveIdentifierDescriptionDtoToIdentifierDescription(SaveIdentifierDescriptionDto data);

    IdentifierFunder saveIdentifierFunderDtoToIdentifierFunder(SaveIdentifierFunderDto data);

    RelatedIdentifier saveRelatedIdentifierDtoToRelatedIdentifier(SaveRelatedIdentifierDto data);

    default Identifier identifierSaveDtoToIdentifier(IdentifierSaveDto data) {
        if (data == null) {
            return null;
        }
        final Identifier identifier = Identifier.builder()
                .id(data.getId())
                .queryId(data.getQueryId())
                .tableId(data.getTableId())
                .viewId(data.getViewId())
                .creators(new LinkedList<>(data.getCreators()
                        .stream()
                        .map(this::saveIdentifierCreatorDtoToCreator)
                        .toList()))
                .publisher(data.getPublisher())
                .language(languageTypeDtoToLanguageType(data.getLanguage()))
                .titles(new LinkedList<>(data.getTitles()
                        .stream()
                        .map(this::saveIdentifierTitleDtoToIdentifierTitle)
                        .toList()))
                .descriptions(new LinkedList<>(data.getDescriptions()
                        .stream()
                        .map(this::saveIdentifierDescriptionDtoToIdentifierDescription)
                        .toList()))
                .funders(new LinkedList<>(data.getFunders()
                        .stream()
                        .map(this::saveIdentifierFunderDtoToIdentifierFunder)
                        .toList()))
                .licenses(new LinkedList<>(data.getLicenses()
                        .stream()
                        .map(this::licenseDtoToLicense)
                        .toList()))
                .type(identifierTypeDtoToIdentifierType(data.getType()))
                .publicationDay(data.getPublicationDay())
                .publicationMonth(data.getPublicationMonth())
                .publicationYear(data.getPublicationYear())
                .relatedIdentifiers(new LinkedList<>(data.getRelatedIdentifiers()
                        .stream()
                        .map(this::saveRelatedIdentifierDtoToRelatedIdentifier)
                        .toList()))
                .doi(data.getDoi())
                .build();
        final int[] idx = new int[]{0};
        identifier.getCreators()
                .forEach(c -> {
                    c.setOrdinalPosition(idx[0]++);
                    c.setIdentifier(identifier);
                });
        log.trace("mapped {} creator(s)", identifier.getCreators().size());
        idx[0] = 0;
        identifier.getTitles()
                .forEach(t -> {
                    t.setOrdinalPosition(idx[0]++);
                    t.setIdentifier(identifier);
                });
        log.trace("mapped {} title(s)", identifier.getTitles().size());
        idx[0] = 0;
        identifier.getDescriptions()
                .forEach(d -> {
                    d.setOrdinalPosition(idx[0]++);
                    d.setIdentifier(identifier);
                });
        log.trace("mapped {} description(s)", identifier.getDescriptions().size());
        idx[0] = 0;
        identifier.getRelatedIdentifiers()
                .forEach(r -> {
                    r.setOrdinalPosition(idx[0]++);
                    r.setIdentifier(identifier);
                });
        log.trace("mapped {} related identifier(s)", identifier.getRelatedIdentifiers().size());
        idx[0] = 0;
        identifier.getFunders()
                .forEach(f -> {
                    f.setOrdinalPosition(idx[0]++);
                    f.setIdentifier(identifier);
                });
        log.trace("mapped {} funder(s)", identifier.getDescriptions().size());
        return identifier;
    }

    IdentifierSaveDto identifierToIdentifierSaveDto(Identifier data);

    CreateIdentifierDto identifierToCreateIdentifierDto(Identifier data);

    License licenseDtoToLicense(LicenseDto data);

    ImageCreateDto containerImageToImageCreateDto(ContainerImage data);

    IdentifierTitle identifierCreateTitleDtoToIdentifierTitle(SaveIdentifierTitleDto data);

    IdentifierDescription identifierCreateDescriptionDtoToIdentifierDescription(SaveIdentifierDescriptionDto data);

    IdentifierFunder identifierFunderSaveDtoToIdentifierFunder(SaveIdentifierFunderDto data);

    IdentifierSaveDto createIdentifierDtoToIdentifierSaveDto(CreateIdentifierDto data);

    RelatedIdentifierDto relatedIdentifierToRelatedIdentifierDto(RelatedIdentifier data);

    Creator creatorDtoToCreator(CreatorDto data);

    NameIdentifierSchemeTypeDto nameIdentifierSchemeTypeToNameIdentifierSchemeTypeDto(NameIdentifierSchemeType data);

    @Mappings({
            @Mapping(target = "nameIdentifierSchemeUri", source = "nameIdentifier", qualifiedByName = "identifierSchemeUriMapper"),
            @Mapping(target = "nameIdentifierScheme", source = "nameIdentifier", qualifiedByName = "nameIdentifierSchemeDtoMapper"),
            @Mapping(target = "affiliationIdentifierSchemeUri", source = "affiliationIdentifier", qualifiedByName = "identifierSchemeUriMapper"),
            @Mapping(target = "affiliationIdentifierScheme", source = "affiliationIdentifier", qualifiedByName = "affiliationIdentifierSchemeDtoMapper"),
    })
    CreatorDto creatorToCreatorDto(Creator data);

    @Mappings({
            @Mapping(target = "nameIdentifierSchemeUri", source = "nameIdentifier", qualifiedByName = "identifierSchemeUriMapper"),
            @Mapping(target = "nameIdentifierScheme", source = "nameIdentifier", qualifiedByName = "nameIdentifierSchemeMapper"),
            @Mapping(target = "affiliationIdentifierSchemeUri", source = "affiliationIdentifier", qualifiedByName = "identifierSchemeUriMapper"),
            @Mapping(target = "affiliationIdentifierScheme", source = "affiliationIdentifier", qualifiedByName = "affiliationIdentifierSchemeMapper"),
    })
    Creator creatorCreateDtoToCreator(SaveIdentifierCreatorDto data);

    RelatedIdentifier relatedIdentifierCreateDtoToRelatedIdentifier(SaveRelatedIdentifierDto data);

    IdentifierType identifierTypeDtoToIdentifierType(IdentifierTypeDto data);

    IdentifierStatusType identifierStatusTypeDtoToIdentifierStatusType(IdentifierStatusTypeDto data);

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

    @Named("identifierSchemeUriMapper")
    default String identifierToIdentifierSchemeUri(String data) {
        if (data == null) {
            return null;
        }
        if (data.contains("ror.org")) {
            return "https://ror.org/";
        }
        if (data.contains("orcid.org")) {
            return "https://orcid.org/";
        }
        if (data.contains("grid.ac")) {
            return "https://grid.ac/";
        }
        if (data.contains("isni.org")) {
            return "https://isni.org/";
        }
        return null;
    }

    @Named("nameIdentifierSchemeMapper")
    default NameIdentifierSchemeType identifierToNameIdentifierSchemeType(String data) {
        if (data == null) {
            return null;
        }
        if (data.contains("ror.org")) {
            return NameIdentifierSchemeType.ROR;
        }
        if (data.contains("orcid.org")) {
            return NameIdentifierSchemeType.ORCID;
        }
        if (data.contains("grid.ac")) {
            return NameIdentifierSchemeType.GRID;
        }
        if (data.contains("isni.org")) {
            return NameIdentifierSchemeType.ISNI;
        }
        return null;
    }

    @Named("nameIdentifierSchemeDtoMapper")
    default NameIdentifierSchemeTypeDto identifierToNameIdentifierSchemeTypeDto(String data) {
        if (data == null) {
            return null;
        }
        if (data.contains("ror.org")) {
            return NameIdentifierSchemeTypeDto.ROR;
        }
        if (data.contains("orcid.org")) {
            return NameIdentifierSchemeTypeDto.ORCID;
        }
        if (data.contains("grid.ac")) {
            return NameIdentifierSchemeTypeDto.GRID;
        }
        if (data.contains("isni.org")) {
            return NameIdentifierSchemeTypeDto.ISNI;
        }
        return null;
    }

    @Named("affiliationIdentifierSchemeMapper")
    default AffiliationIdentifierSchemeType identifierToAffiliationIdentifierSchemeType(String data) {
        if (data == null) {
            return null;
        }
        if (data.contains("ror.org")) {
            return AffiliationIdentifierSchemeType.ROR;
        }
        if (data.contains("grid.ac")) {
            return AffiliationIdentifierSchemeType.GRID;
        }
        if (data.contains("isni.org")) {
            return AffiliationIdentifierSchemeType.ISNI;
        }
        return null;
    }

    @Named("affiliationIdentifierSchemeDtoMapper")
    default AffiliationIdentifierSchemeTypeDto identifierToAffiliationIdentifierSchemeTypeDto(String data) {
        if (data == null) {
            return null;
        }
        if (data.contains("ror.org")) {
            return AffiliationIdentifierSchemeTypeDto.ROR;
        }
        if (data.contains("grid.ac")) {
            return AffiliationIdentifierSchemeTypeDto.GRID;
        }
        if (data.contains("isni.org")) {
            return AffiliationIdentifierSchemeTypeDto.ISNI;
        }
        return null;
    }

    @Mappings({
            @Mapping(target = "isDefault", expression = "java(false)")
    })
    ContainerImage createImageDtoToContainerImage(ImageCreateDto data);

    ImageBriefDto containerImageToImageBriefDto(ContainerImage data);

    ImageDto containerImageToImageDto(ContainerImage data);

    default Instant dateToInstant(String date) {
        return Instant.parse(date);
    }

    LicenseDto licenseToLicenseDto(License data);

    default String instantToDatestamp(Instant data) {
        final String datestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.systemDefault())
                .format(data);
        log.trace("mapped instant {} to string {}", data, datestamp);
        return datestamp;
    }

    @Mappings({
            @Mapping(target = "rdf", expression = "java(data.getRdfPath() != null)"),
            @Mapping(target = "sparql", expression = "java(data.getSparqlEndpoint() != null)")
    })
    OntologyDto ontologyToOntologyDto(Ontology data);

    @Mappings({
            @Mapping(target = "rdf", expression = "java(data.getRdfPath() != null)"),
            @Mapping(target = "sparql", expression = "java(data.getSparqlEndpoint() != null)")
    })
    OntologyBriefDto ontologyToOntologyBriefDto(Ontology data);

    Ontology ontologyCreateDtoToOntology(OntologyCreateDto data);

    ConceptDto tableColumnConceptToConceptDto(TableColumnConcept data);

    ConceptBriefDto tableColumnConceptToConceptBriefDto(TableColumnConcept data);

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);

    UnitBriefDto tableColumnUnitToUnitBriefDto(TableColumnUnit data);

    TableColumnUnit unitSaveDtoToTableColumnUnit(UnitSaveDto data);

    TableColumnUnit entityDtoToTableColumnUnit(EntityDto data);

    TableColumnConcept entityDtoToTableColumnConcept(EntityDto data);

    TableColumnConcept conceptSaveDtoToTableColumnConcept(ConceptSaveDto data);

    @Mappings({
            @Mapping(target = "databaseId", source = "tdbid"),
    })
    TableBriefDto tableToTableBriefDto(Table data);

    default UniqueDto uniqueToUniqueDto(Unique data) {
        return UniqueDto.builder()
                .id(data.getId())
                .name(data.getName())
                .columns(data.getColumns()
                        .stream()
                        .map(this::tableColumnToColumnBriefDto)
                        .toList())
                .table(tableToTableBriefDto(data.getTable()))
                .build();
    }

    ForeignKeyDto foreignKeyToForeignKeyDto(ForeignKey data);

    ForeignKeyBriefDto foreignKeyDtoToForeignKeyBriefDto(ForeignKeyDto data);

    default ConstraintsDto constraintsToConstraintsDto(Constraints data) {
        if (data == null) {
            return null;
        }
        return ConstraintsDto.builder()
                .checks(data.getChecks())
                .uniques(data.getUniques()
                        .stream()
                        .map(this::uniqueToUniqueDto)
                        .toList())
                .foreignKeys(data.getForeignKeys()
                        .stream()
                        .map(this::foreignKeyToForeignKeyDto)
                        .toList())
                .primaryKey(data.getPrimaryKey()
                        .stream()
                        .map(this::primaryKeyToPrimaryKeyDto)
                        .collect(Collectors.toSet()))
                .build();
    }

    default DatabaseAccess userToWriteAllAccess(Database database, User user) {
        return DatabaseAccess.builder()
                .type(AccessType.WRITE_ALL)
                .hdbid(database.getId())
                .database(database)
                .huserid(user.getId())
                .user(user)
                .build();
    }

    default TableDto tableToTableDto(Table data) {
        final TableDto table = TableDto.builder()
                .id(data.getId())
                .name(data.getName())
                .internalName(data.getInternalName())
                .owner(userToUserBriefDto(data.getOwner()))
                .databaseId(data.getTdbid())
                .isPublic(data.getIsPublic())
                .isSchemaPublic(data.getIsSchemaPublic())
                .isVersioned(true)
                .description(data.getDescription())
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .constraints(constraintsToConstraintsDto(data.getConstraints()))
                .created(data.getCreated())
                .build();
        if (data.getIdentifiers() != null) {
            table.setIdentifiers(new LinkedList<>(data.getIdentifiers()
                    .stream()
                    .map(this::identifierToIdentifierDto)
                    .toList()));
        }
        table.setQueueName(data.getQueueName());
        table.setQueueType("quorum");
        table.setRoutingKey("dbrepo." + data.getTdbid() + "." + data.getId());
        table.setAvgRowLength(data.getAvgRowLength());
        table.setMaxDataLength(data.getMaxDataLength());
        table.setDataLength(data.getDataLength());
        table.setNumRows(data.getNumRows());
        table.setCreationLocation(data.getCreationLocation());
        table.setReplicaUrls(replicaTableLocationListToMap(data.getReplicaUrls()));
        if (table.getConstraints() != null) {
            table.getConstraints()
                    .getPrimaryKey()
                    .forEach(pk -> {
                        pk.getTable().setDatabaseId(data.getDatabase().getId());
                        pk.getColumn().setTableId(data.getId());
                        pk.getColumn().setDatabaseId(data.getDatabase().getId());
                    });
            table.getConstraints()
                    .getForeignKeys()
                    .forEach(fk -> {
                        fk.getTable().setDatabaseId(table.getDatabaseId());
                        fk.getReferencedTable().setDatabaseId(table.getDatabaseId());
                        fk.getReferences()
                                .forEach(ref -> {
                                    ref.setForeignKey(foreignKeyDtoToForeignKeyBriefDto(fk));
                                    ref.getColumn().setTableId(table.getId());
                                    ref.getColumn().setDatabaseId(table.getDatabaseId());
                                    ref.getReferencedColumn().setTableId(fk.getReferencedTable().getId());
                                    ref.getReferencedColumn().setDatabaseId(table.getDatabaseId());
                                });
                    });
            table.getConstraints()
                    .getUniques()
                    .forEach(uk -> {
                        uk.getTable().setDatabaseId(data.getDatabase().getId());
                        uk.getColumns()
                                .forEach(column -> {
                                    column.setTableId(data.getId());
                                    column.setDatabaseId(data.getDatabase().getId());
                                });
                    });
            if (data.getConstraints().getChecks() == null || data.getConstraints().getChecks().isEmpty()) {
                table.getConstraints().setChecks(new LinkedHashSet<>());
            }
        }
        if (data.getColumns() != null) {
            table.setColumns(new LinkedList<>(data.getColumns()
                    .stream()
                    .map(this::tableColumnToColumnDto)
                    .toList()));
        }
        return table;
    }

    @Named("replicaTableLocationListToMap")
    default Map<String, UUID> replicaTableLocationListToMap(List<ReplicaTableLocation> replicaLocations) {
        if (replicaLocations == null) {
            return new LinkedHashMap<>();
        }
        return replicaLocations.stream()
                .filter(rl -> rl.getUrl() != null)
                .collect(
                        java.util.HashMap::new,
                        (m, rl) -> m.put(rl.getUrl(), rl.getReplicaTableId()),
                        Map::putAll
                );
    }

    @Named("mapToReplicaTableLocationList")
    default List<ReplicaTableLocation> mapToReplicaTableLocationList(Map<String, UUID> replicaUrlToId) {
        if (replicaUrlToId == null) {
            return new LinkedList<>();
        }
        return replicaUrlToId.entrySet()
                .stream()
                .map(e -> ReplicaTableLocation.builder()
                        .url(e.getKey())
                        .replicaTableId(e.getValue())
                        .build())
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Mappings({
            @Mapping(target = "foreignKey", ignore = true),
    })
    ForeignKeyReferenceDto foreignKeyReferenceToForeignKeyReferenceDto(ForeignKeyReference foreignKeyReference);

    @Mappings({
            @Mapping(target = "table", ignore = true)
    })
    TableColumn columnDtoToTableColumn(ColumnDto columnDto);

    @Mappings({
            @Mapping(target = "table", ignore = true)
    })
    Unique uniqueDtoToUnique(UniqueDto data);

    @Mappings({
            @Mapping(target = "ownedBy", source = "owner.id"),
            @Mapping(target = "tdbid", source = "databaseId"),
            @Mapping(target = "database", ignore = true),
            @Mapping(target = "replicaUrls", source = "replicaUrls", qualifiedByName = "mapToReplicaTableLocationList")
    })
    Table tableDtoToTable(TableDto data);

    PrimaryKeyDto primaryKeyToPrimaryKeyDto(PrimaryKey data);

    ReferenceType referenceTypeDtoToReferenceType(ReferenceTypeDto data);

    /* keep */
    default Constraints constraintsCreateDtoToConstraints(CreateTableConstraintsDto data, Database database, Table table) {
        final int[] idx = new int[]{0, 0};
        final Constraints constrains = Constraints.builder()
                .checks(data.getChecks())
                .uniques(data.getUniques()
                        .stream()
                        .map(uniqueList -> Unique.builder()
                                .name("uk_" + table.getInternalName() + "_" + idx[0]++)
                                .table(table)
                                .columns(table.getColumns()
                                        .stream()
                                        .filter(ukColumn -> uniqueList.stream().map(this::nameToInternalName).toList().contains(nameToInternalName(ukColumn.getInternalName())))
                                        .toList())
                                .build())
                        .toList())
                .foreignKeys(data.getForeignKeys()
                        .stream()
                        .map(fk -> {
                            final Optional<Table> optional = database.getTables()
                                    .stream()
                                    .filter(t -> t.getInternalName().equals(fk.getReferencedTable()))
                                    .findFirst();
                            if (optional.isEmpty()) {
                                log.error("Failed to find foreign key referenced table {} in tables: {}", fk.getReferencedTable(), database.getTables().stream().map(Table::getInternalName).toList());
                                throw new IllegalArgumentException("Failed to find foreign key referenced table");
                            }
                            final List<ForeignKeyReference> references = new LinkedList<>();
                            for (int i = 0; i < fk.getColumns().size(); i++) {
                                final int k = i;
                                final Optional<TableColumn> column = table.getColumns()
                                        .stream()
                                        .filter(cc -> cc.getInternalName().equals(fk.getColumns().get(k)))
                                        .findFirst();
                                if (column.isEmpty()) {
                                    log.error("Failed to find foreign key column {}.{} in columns: {}", table.getInternalName(), fk.getColumns().get(k), optional.get().getColumns().stream().map(TableColumn::getInternalName).toList());
                                    throw new IllegalArgumentException("Failed to find foreign key column");
                                }
                                final Optional<TableColumn> referencedColumn = optional.get()
                                        .getColumns()
                                        .stream()
                                        .filter(cc -> cc.getInternalName().equals(fk.getReferencedColumns().get(k)))
                                        .findFirst();
                                if (referencedColumn.isEmpty()) {
                                    log.error("Failed to find foreign key referenced column {} in referenced columns: {}", fk.getReferencedColumns().get(k), database.getTables().stream().filter(t -> t.getInternalName().equals(fk.getReferencedTable())).map(Table::getColumns).flatMap(List::stream).map(TableColumn::getInternalName).toList());
                                    throw new IllegalArgumentException("Failed to find foreign key referenced column");
                                }
                                references.add(ForeignKeyReference.builder()
                                        .column(column.get())
                                        .referencedColumn(referencedColumn.get())
                                        .foreignKey(null) // set at the end
                                        .build());
                            }
                            return ForeignKey.builder()
                                    .name("fk_" + table.getInternalName() + "_" + idx[1]++)
                                    .table(table)
                                    .referencedTable(optional.get())
                                    .references(references)
                                    .onDelete(referenceTypeDtoToReferenceType(fk.getOnDelete()))
                                    .onUpdate(referenceTypeDtoToReferenceType(fk.getOnUpdate()))
                                    .build();
                        })
                        .toList())
                .primaryKey(data.getPrimaryKey()
                        .stream()
                        .map(pk -> {
                            final Optional<TableColumn> optional = table.getColumns()
                                    .stream()
                                    .filter(c -> c.getInternalName().equals(nameToInternalName(pk)))
                                    .findFirst();
                            if (optional.isEmpty()) {
                                log.error("Failed to find primary key column '{}' in columns: {}", pk, table.getColumns().stream().map(TableColumn::getInternalName).toList());
                                throw new IllegalArgumentException("Failed to find primary key column");
                            }
                            return PrimaryKey.builder()
                                    .table(table)
                                    .column(optional.get())
                                    .build();
                        })
                        .toList())
                .build();
        constrains.getForeignKeys()
                .forEach(fk -> fk.getReferences()
                        .forEach(r -> r.setForeignKey(fk)));
        return constrains;
    }

    /* keep */
    @Mappings({
            @Mapping(target = "tableId", source = "table.id"),
            @Mapping(target = "databaseId", source = "table.database.id"),
            @Mapping(target = "description", source = "description")
    })
    ColumnDto tableColumnToColumnDto(TableColumn data);

    @Mappings({
            @Mapping(target = "tableId", source = "table.id"),
            @Mapping(target = "databaseId", source = "table.database.id")
    })
    ColumnBriefDto tableColumnToColumnBriefDto(TableColumn data);

    @Mappings({
            @Mapping(target = "id", expression = "java(null)"),
            @Mapping(target = "columnType", source = "data.type"),
            @Mapping(target = "isNullAllowed", source = "data.nullAllowed"),
            @Mapping(target = "name", source = "data.name"),
            @Mapping(target = "internalName", expression = "java(nameToInternalName(data.getName()))"),
            @Mapping(target = "enums", ignore = true),
            @Mapping(target = "sets", ignore = true),
    })
    TableColumn columnCreateDtoToTableColumn(CreateTableColumnDto data, ContainerImage image);

    /* keep */
    @Mappings({
            @Mapping(target = "name", expression = "java(userToFullName(data))"),
            @Mapping(target = "qualifiedName", expression = "java(userToQualifiedName(data))"),
    })
    UserBriefDto userToUserBriefDto(User data);

    /* keep */
    @Mappings({
            @Mapping(target = "attributes.language", source = "language"),
            @Mapping(target = "attributes.orcid", source = "orcid"),
            @Mapping(target = "attributes.affiliation", source = "affiliation"),
            @Mapping(target = "attributes.theme", source = "theme"),
            @Mapping(target = "attributes.mariadbPassword", source = "mariadbPassword"),
            @Mapping(target = "name", expression = "java(userToFullName(data))"),
            @Mapping(target = "qualifiedName", expression = "java(userToQualifiedName(data))"),
    })
    UserDto userToUserDto(User data);

    /* keep */
    @Named("userToFullName")
    default String userToFullName(User data) {
        final StringBuilder name = new StringBuilder();
        if (data.getFirstname() != null) {
            name.append(data.getFirstname());
        }
        if (data.getLastname() != null) {
            name.append(!name.isEmpty() ? " " : null)
                    .append(data.getLastname());
        }
        return name.isEmpty() ? null : name.toString()
                .trim();
    }

    /* keep */
    @Named("userToQualifiedName")
    default String userToQualifiedName(User data) {
        final String fullname = userToFullName(data);
        final StringBuilder name = new StringBuilder();
        if (fullname != null) {
            name.append(fullname);
        }
        if (!name.isEmpty()) {
            name.append(" — ");
        }
        name.append("@")
                .append(data.getUsername());
        return name.toString()
                .trim();
    }

    @Mappings({
            @Mapping(target = "databaseId", source = "database.id")
    })
    ViewDto viewToViewDto(View data);

    @Mappings({
            @Mapping(target = "databaseId", source = "view.database.id"),
    })
    ViewColumnDto viewColumnToViewColumnDto(ViewColumn data);

    @Mappings({
            @Mapping(target = "vdbid", source = "database.id")
    })
    ViewBriefDto viewToViewBriefDto(View data);

    @Mappings({
            @Mapping(target = "database", ignore = true),
            @Mapping(target = "ownedBy", source = "owner.id"),
    })
    View viewDtoToView(ViewDto data);

    /* keep */
    @Named("internalMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(data).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("_");
        return slug.toLowerCase(Locale.ENGLISH)
                .replaceAll("-", "_");
    }

    LanguageType languageTypeDtoToLanguageType(LanguageTypeDto data);

    DatabaseBriefDto databaseDtoToDatabaseBriefDto(DatabaseDto data);

    @Mappings({
            @Mapping(target = "ownerId", source = "owner.id")
    })
    DatabaseBriefDto databaseToDatabaseBriefDto(Database data);

    AccessType accessTypeDtoToAccessType(AccessTypeDto data);

    AccessTypeDto accessTypeToAccessTypeDto(AccessType data);

    DatabaseAccessDto databaseAccessToDatabaseAccessDto(DatabaseAccess data);

}
