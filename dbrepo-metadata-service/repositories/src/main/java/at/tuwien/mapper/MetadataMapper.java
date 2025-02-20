package at.tuwien.mapper;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.CreateContainerDto;
import at.tuwien.api.container.image.DataTypeDto;
import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.api.database.*;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnBriefDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.CreateTableColumnDto;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.CreateTableConstraintsDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyBriefDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.tuwien.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.datacite.doi.*;
import at.tuwien.api.identifier.*;
import at.tuwien.api.identifier.ld.LdCreatorDto;
import at.tuwien.api.identifier.ld.LdDatasetDto;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.maintenance.BannerMessageBriefDto;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageDto;
import at.tuwien.api.maintenance.BannerMessageTypeDto;
import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.api.orcid.activities.employments.affiliation.OrcidAffiliationGroupDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.OrcidEmploymentSummaryDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedSourceTypeDto;
import at.tuwien.api.ror.RorDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.OntologyBriefDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.api.user.external.ExternalResultType;
import at.tuwien.api.user.external.affiliation.ExternalAffiliationDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.DataType;
import at.tuwien.entities.database.*;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKeyReference;
import at.tuwien.entities.database.table.constraints.foreignKey.ReferenceType;
import at.tuwien.entities.database.table.constraints.primaryKey.PrimaryKey;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import at.tuwien.entities.identifier.*;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.entities.maintenance.BannerMessageType;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.entities.user.User;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.*;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", imports = {LinkedList.class, ExternalResultType.class})
public interface MetadataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataMapper.class);

    @Mappings({
            @Mapping(target = "dMin", source = "DMin"),
            @Mapping(target = "dMax", source = "DMax"),
            @Mapping(target = "dDefault", source = "DDefault"),
            @Mapping(target = "dRequired", source = "DRequired")
    })
    DataTypeDto dataTypeToDataTypeDto(DataType data);

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
            @Mapping(target = "previewImage", expression = "java(database.getImage() != null ? \"/api/database/\" + database.getId() + \"/image\" : null)")
    })
    DatabaseDto databaseToDatabaseDto(Database database);

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

    default ExternalMetadataDto rorDtoToExternalMetadataDto(RorDto data) {
        return ExternalMetadataDto.builder()
                .affiliations(new ExternalAffiliationDto[]{
                        ExternalAffiliationDto.builder()
                                .organizationName(data.getName())
                                .build()})
                .type(ExternalResultType.ORGANIZATIONAL)
                .build();
    }

    default ExternalMetadataDto crossrefDtoToExternalMetadataDto(CrossrefDto data) {
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
    })
    IdentifierDto identifierToIdentifierDto(Identifier data);

    @Mappings({
            @Mapping(target = "databaseId", source = "database.id"),
            @Mapping(target = "ownedBy", source = "owner.id")
    })
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

    Identifier identifierCreateDtoToIdentifier(CreateIdentifierDto data);

    Identifier identifierUpdateDtoToIdentifier(IdentifierSaveDto data);

    License licenseDtoToLicense(LicenseDto data);

    IdentifierTitle identifierCreateTitleDtoToIdentifierTitle(SaveIdentifierTitleDto data);

    IdentifierDescription identifierCreateDescriptionDtoToIdentifierDescription(SaveIdentifierDescriptionDto data);

    IdentifierFunder identifierFunderSaveDtoToIdentifierFunder(SaveIdentifierFunderDto data);

    IdentifierSaveDto identifierCreateDtoToIdentifierSaveDto(CreateIdentifierDto data);

    RelatedIdentifierDto relatedIdentifierToRelatedIdentifierDto(RelatedIdentifier data);

    Creator creatorDtoToCreator(CreatorDto data);

    NameIdentifierSchemeTypeDto nameIdentifierSchemeTypeToNameIdentifierSchemeTypeDto(NameIdentifierSchemeType data);

    CreatorDto creatorToCreatorDto(Creator data);

    @Mappings({
            @Mapping(target = "nameIdentifierSchemeUri", source = "nameIdentifierScheme", qualifiedByName = "nameSchemaMapper"),
            @Mapping(target = "affiliationIdentifierSchemeUri", source = "affiliationIdentifierScheme", qualifiedByName = "affiliationSchemaMapper"),
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

    @Mappings({
            @Mapping(target = "isDefault", source = "isDefault")
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

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);

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
                .tdbid(data.getTdbid())
                .isPublic(data.getIsPublic())
                .isSchemaPublic(data.getIsSchemaPublic())
                .isVersioned(true)
                .description(data.getDescription())
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .database(databaseToDatabaseDto(data.getDatabase()
                        .toBuilder()
                        .tables(null)
                        .build()))
                .constraints(constraintsToConstraintsDto(data.getConstraints()))
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
                        fk.getTable().setDatabaseId(table.getTdbid());
                        fk.getReferencedTable().setDatabaseId(table.getTdbid());
                        fk.getReferences()
                                .forEach(ref -> {
                                    ref.setForeignKey(foreignKeyDtoToForeignKeyBriefDto(fk));
                                    ref.getColumn().setTableId(table.getId());
                                    ref.getColumn().setDatabaseId(table.getTdbid());
                                    ref.getReferencedColumn().setTableId(fk.getReferencedTable().getId());
                                    ref.getReferencedColumn().setDatabaseId(table.getTdbid());
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
            @Mapping(target = "ownedBy", source = "owner.id"),
            @Mapping(target = "database", ignore = true)
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
            @Mapping(target = "database.views", ignore = true),
            @Mapping(target = "database.tables", ignore = true)
    })
    ViewDto viewToViewDto(View data);

    @Mappings({
            @Mapping(target = "databaseId", source = "view.database.id"),
    })
    ViewColumnDto viewColumnToViewColumnDto(ViewColumn data);

    ViewBriefDto viewToViewBriefDto(View data);

    @Mappings({
            @Mapping(target = "database", ignore = true)
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
        final String name = slug.toLowerCase(Locale.ENGLISH)
                .replaceAll("-", "_");
        log.debug("mapping name {} to internal name {}", data, name);
        return name;
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
