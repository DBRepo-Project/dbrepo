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
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
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
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.OrcidAffiliationGroupDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.OrcidEmploymentSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedSourceTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.ror.RorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.ror.RorNameDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserAttributesDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalMetadataDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalResultType;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.affiliation.ExternalAffiliationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.DataType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ViewColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKeyReference;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ReferenceType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.primaryKey.PrimaryKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessage;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessageType;
import at.ac.tuwien.ifs.dbrepo.core.exception.ColumnNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageInvalidException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;
import org.apache.commons.lang3.RandomStringUtils;
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
        final Set<String> read = Arrays.stream(grantDefaultRead.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        final Set<String> write = Arrays.stream(grantDefaultWrite.split(","))
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
            @Mapping(target = "ownerUsername", source = "ownedBy")
    })
    CreateDashboardDto databaseToCreateDashboardDto(Database database);

    @Mappings({
            @Mapping(target = "id", ignore = true), /* id attribute is ignored by the library anyway, just making it explicit */
            @Mapping(target = "attributes", ignore = true)
    })
    UserRepresentation userCreateDtoToUserRepresentation(UserCreateDto data);

    DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")
            .withZone(ZoneId.of("UTC"));

    @Mappings({
            @Mapping(target = "id", source = "userId"),
            @Mapping(target = "username", source = "privilegedUsername"),
            @Mapping(target = "password", source = "privilegedPassword"),
    })
    User createDatabaseDtoToPrivilegedUser(at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto data);

    @Mappings({
            @Mapping(target = "id", source = "userId"),
    })
    User createDatabaseDtoToUser(at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto data);

    @Mappings({
            @Mapping(target = "username", source = "readonlyUsername"),
            @Mapping(target = "password", source = "readonlyPassword"),
    })
    User createDatabaseDtoToReadonlyUser(CreateDatabaseDto data);

    @Mappings({
            @Mapping(target = "ownedBy", source = "owner.username")
    })
    Subset queryDtoToSubset(QueryDto data);

    UserBriefDto userToUserBriefDto(User data);

    at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container containerDtoToContainer(ContainerDto data);

    Image imageDtoToImage(ImageDto data);

    @Mappings({
            @Mapping(target = "subsets", ignore = true),
            @Mapping(target = "ownedBy", source = "owner.username")
    })
    at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database databaseDtoToDatabase(DatabaseDto data);

    @Mappings({
            @Mapping(target = "owner.username", source = "ownedBy")
    })
    QueryDto subsetToQueryDto(Subset data);

    ColumnDto viewColumnDtoToColumnDto(ViewColumnDto data);

    ViewColumnDto columnDtoToViewColumnDto(ColumnDto data);

    User userDtoToUser(UserDto data);

    @Mappings({
            @Mapping(target = "accessToken", source = "token")
    })
    TokenDto accessTokenResponseToTokenDto(AccessTokenResponse data);

    UserBriefDto userDtoToUserBriefDto(UserDto data);

    TableBriefDto tableDtoToTableBriefDto(TableDto data);

    IdentifierBriefDto identifierDtoToIdentifierBriefDto(IdentifierDto data);

    default String metricToUri(String baseUrl, UUID databaseId, UUID tableId, UUID subsetId, UUID viewId) {
        final StringBuilder uri = new StringBuilder(baseUrl)
                .append("/database/")
                .append(databaseId);
        if (tableId != null) {
            uri.append("/table/")
                    .append(tableId);
        } else if (subsetId != null) {
            uri.append("/subset/")
                    .append(subsetId);
        } else if (viewId != null) {
            uri.append("/view/")
                    .append(viewId);
        }
        log.trace("count uri: {}", uri);
        return uri.toString();
    }

    ColumnBriefDto columnDtoToColumnBriefDto(ColumnDto data);

    default at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType imageDtoTypeNameToDataTypeDto(Image image, String columnType) throws ImageInvalidException {
        final Optional<at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType> optional = image.getDataTypes()
                .stream()
                .filter(t -> t.getValue().toLowerCase().equals(columnType))
                .findFirst();
        if (optional.isEmpty()) {
            final List<String> dataTypes = image.getDataTypes().stream().map(t -> t.getValue().toLowerCase()).toList();
            log.error("Failed to find data type {} in image datatypes: {}", columnType, dataTypes);
            throw new ImageInvalidException("Failed to find data type " + columnType + " in image datatypes: " + Arrays.toString(dataTypes.toArray()));
        }
        return optional.get();
    }

    default UserDto userRepresentationToUserDto(UserRepresentation data) {
        return UserDto.builder()
                .id(UUID.fromString(data.getId()))
                .username(data.getUsername())
                .firstname(data.getFirstName())
                .lastname(data.getLastName())
                .attributes(UserAttributesDto.builder()
                        .theme(attributeToValue("theme", data.getAttributes()))
                        .affiliation(attributeToValue("affiliation", data.getAttributes()))
                        .orcid(attributeToValue("orcid", data.getAttributes()))
                        .language(attributeToValue("language", data.getAttributes()))
                        .mariadbPassword(attributeToValue("mariadb_password", data.getAttributes()))
                        .build())
                .build();
    }

    default UserRepresentation userDtoToUserRepresentation(UserDto data) {
        final UserRepresentation user = new UserRepresentation();
        user.setId(String.valueOf(data.getId()));
        user.setUsername(data.getUsername());
        user.setFirstName(data.getFirstname());
        user.setLastName(data.getLastname());
        user.setRealmRoles(new LinkedList<>());
        user.setAttributes(new HashMap<>() {{
            if (data.getAttributes().getTheme() != null) {
                put("theme", new LinkedList<>(List.of(data.getAttributes().getTheme())));
            }
            if (data.getAttributes().getAffiliation() != null) {
                put("affiliation", new LinkedList<>(List.of(data.getAttributes().getAffiliation())));
            }
            if (data.getAttributes().getOrcid() != null) {
                put("orcid", new LinkedList<>(List.of(data.getAttributes().getOrcid())));
            }
            if (data.getAttributes().getLanguage() != null) {
                put("language", new LinkedList<>(List.of(data.getAttributes().getLanguage())));
            }
            if (data.getAttributes().getMariadbPassword() != null) {
                put("mariadb_password", new LinkedList<>(List.of(data.getAttributes().getMariadbPassword())));
            }
        }});
        return user;
    }

    default String attributeToValue(String key, Map<String, List<String>> attributes) {
        if (attributes == null || !attributes.containsKey(key)) {
            return null;
        }
        final List<String> values = attributes.get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() == 1) {
            return values.getFirst();
        }
        throw new IllegalArgumentException("Multiple values found for key: " + key);
    }

    default String databaseSuffix() {
        return "_" + RandomStringUtils.secure()
                .nextAlphanumeric(4)
                .toLowerCase();
    }

    BannerMessageDto bannerMessageToBannerMessageDto(BannerMessage data);

    BannerMessageBriefDto bannerMessageToBannerMessageBriefDto(BannerMessage data);

    ViewColumn viewColumnDtoToViewColumn(ViewColumnDto data);

    BannerMessage bannerMessageCreateDtoToBannerMessage(BannerMessageCreateDto data);

    BannerMessageType bannerMessageTypeDtoToBannerMessageType(BannerMessageTypeDto data);

    @Mappings({
            @Mapping(target = "internalName", source = "name", qualifiedByName = "internalMapping")
    })
    Container containerCreateRequestDtoToContainer(CreateContainerDto data);

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
            @Mapping(target = "previewImage", expression = "java(database.getImage() != null ? \"/api/v1/database/\" + database.getId() + \"/image\" : null)"),
            @Mapping(target = "accesses", expression = "java(database.getAccesses().stream().map(a -> databaseAccessToDatabaseAccessDto(a)).toList())"),
            @Mapping(target = "contact", expression = "java(UserBriefDto.builder().username(database.getContactPerson()).build())"),
            @Mapping(target = "owner", expression = "java(UserBriefDto.builder().username(database.getOwnedBy()).build())")
    })
    DatabaseDto databaseToDatabaseDto(Database database);

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
        String organizationName = null;
        if (data.getName() != null) {
            organizationName = data.getName();
            log.trace("mapped ror name to organization name: {}", organizationName);
        } else if (!data.getNames().isEmpty()) {
            final Optional<RorNameDto> optional = data.getNames()
                    .stream()
                    .filter(n -> Objects.nonNull(n.getLang()))
                    .filter(n -> n.getLang().equals("en"))
                    .findFirst();
            if (optional.isPresent()) {
                organizationName = optional.get()
                        .getValue();
                log.trace("mapped ror name list (en) to organization name: {}", organizationName);
            } else {
                organizationName = data.getNames()
                        .getFirst()
                        .getValue();
                log.trace("mapped ror name list (first element) to organization name: {}", organizationName);
            }
        }
        return ExternalMetadataDto.builder()
                .affiliations(new ExternalAffiliationDto[]{
                        ExternalAffiliationDto.builder()
                                .organizationName(organizationName)
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
            @Mapping(target = "owner", expression = "java(UserBriefDto.builder().username(data.getOwnedBy()).build())"),
    })
    IdentifierDto identifierToIdentifierDto(Identifier data);

    default LinksDto identifierToLinksDto(Identifier data) {
        final LinksDto links = LinksDto.builder()
                .self("/api/v1/identifier/" + data.getId())
                .selfHtml("/pid/" + data.getId())
                .build();
        switch (data.getType()) {
            case VIEW ->
                    links.setData("/api/v1/database/" + data.getDatabase().getId() + "/view/" + data.getViewId() + "/data");
            case TABLE ->
                    links.setData("/api/v1/database/" + data.getDatabase().getId() + "/table/" + data.getTableId() + "/data");
            case SUBSET ->
                    links.setData("/api/v1/database/" + data.getDatabase().getId() + "/subset/" + data.getQueryId() + "/data");
        }
        if (data.getDatabase().getIsDashboardEnabled()) {
            links.setDashboardHtml("/d/" + data.getDatabase().getDashboardUid());
        }
        return links;
    }

    @Mappings({
            @Mapping(target = "databaseId", source = "database.id"),
            @Mapping(target = "ownedBy", source = "ownedBy")
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

    default TableDto tableToTableDto(Table data) {
        final TableDto table = TableDto.builder()
                .id(data.getId())
                .name(data.getName())
                .internalName(data.getInternalName())
                .databaseId(data.getTdbid())
                .isPublic(data.getIsPublic())
                .isSchemaPublic(data.getIsSchemaPublic())
                .isVersioned(true)
                .description(data.getDescription())
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .constraints(constraintsToConstraintsDto(data.getConstraints()))
                .created(data.getCreated())
                .owner(UserBriefDto.builder()
                        .username(data.getOwnedBy())
                        .build())
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
            @Mapping(target = "ownedBy", source = "owner.username"),
            @Mapping(target = "tdbid", source = "databaseId"),
            @Mapping(target = "database", ignore = true)
    })
    Table tableDtoToTable(TableDto data);

    @Mappings({
            @Mapping(target = "subsets", ignore = true)
    })
    at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database databaseToDatabaseCache(Database data);

    @Mappings({
            @Mapping(target = "host", source = "host"),
            @Mapping(target = "port", source = "port"),
            @Mapping(target = "username", source = "privilegedUsername"),
            @Mapping(target = "password", source = "privilegedPassword")
    })
    at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container containerToContainerCache(Container data);

    @Mappings({
            @Mapping(target = "ownedBy", source = "owner.username")
    })
    at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table tableDtoToTableCache(TableDto data);

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
            @Mapping(target = "description", source = "description"),
            @Mapping(target = "unit", source = "unitUri")
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

    @Mappings({
            @Mapping(target = "databaseId", source = "database.id"),
            @Mapping(target = "owner", expression = "java(UserBriefDto.builder().username(data.getOwnedBy()).build())")
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
    })
    View viewDtoToView(ViewDto data);

    @Mappings({
            @Mapping(target = "ownedBy", source = "owner.username")
    })
    at.ac.tuwien.ifs.dbrepo.core.entity.cache.View viewDtoToViewCache(ViewDto data);

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

    @Mappings({
            @Mapping(target = "contactPerson", source = "contact"),
            @Mapping(target = "ownedBy", source = "owner.username")
    })
    DatabaseBriefDto databaseDtoToDatabaseBriefDto(DatabaseDto data);

    @Mappings({
            @Mapping(target = "contactPerson", expression = "java(UserBriefDto.builder().username(data.getContactPerson()).build())"),
    })
    DatabaseBriefDto databaseToDatabaseBriefDto(Database data);

    AccessType accessTypeDtoToAccessType(AccessTypeDto data);

    AccessTypeDto accessTypeToAccessTypeDto(AccessType data);

    @Mappings({
            @Mapping(target = "user", expression = "java(UserBriefDto.builder().username(data.getUsername()).build())")
    })
    DatabaseAccessDto databaseAccessToDatabaseAccessDto(DatabaseAccess data);

    default at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn columnIdToViewColumnDto(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database database, UUID columnId) throws ColumnNotFoundException {
        if (columnId == null) {
            return null;
        }
        final Optional<at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn> optional = database.getViews()
                .stream()
                .map(at.ac.tuwien.ifs.dbrepo.core.entity.cache.View::getColumns)
                .flatMap(List::stream)
                .filter(column -> column.getId().equals(columnId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column: {}", columnId);
            throw new ColumnNotFoundException("Failed to find column: " + columnId);
        }
        final at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn column = optional.get();
        log.trace("mapped column id {} to view column: {}", columnId, column.getInternalName());
        return column;
    }

    default Column columnIdToColumnDto(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database database, UUID columnId) throws ColumnNotFoundException {
        if (columnId == null) {
            return null;
        }
        final Optional<Column> optional = database.getTables()
                .stream()
                .map(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table::getColumns)
                .flatMap(List::stream)
                .filter(column -> column.getId().equals(columnId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find column: {}", columnId);
            throw new ColumnNotFoundException("Failed to find column: " + columnId);
        }
        final Column column = optional.get();
        log.trace("mapped column id {} to column: {}", columnId, column.getInternalName());
        return column;
    }

    default Operator operatorIdToOperatorDto(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database database, UUID operatorId) throws ImageNotFoundException {
        final Optional<Operator> optional = database.getContainer()
                .getImage()
                .getOperators()
                .stream()
                .filter(op -> op.getId().equals(operatorId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find operator: {}", operatorId);
            throw new ImageNotFoundException("Failed to find operator: " + operatorId);
        }
        return optional.get();
    }

}
