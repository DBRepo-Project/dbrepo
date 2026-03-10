package at.ac.tuwien.ifs.dbrepo.core.test;

import at.ac.tuwien.ifs.dbrepo.core.api.ExportResourceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.CreateVirtualHostDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.ExchangeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.GrantVirtualHostPermissionsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.amqp.QueueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary.PrimaryKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.DataCiteBody;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.DataCiteData;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoi;
import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoiRelatedIdentifier;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.*;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.OrcidDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.OrcidActivitiesSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.OrcidEmploymentsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.OrcidAffiliationGroupDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.OrcidEmploymentSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.OrcidSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization.OrcidOrganizationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.OrcidPersonDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.name.OrcidNameDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.name.OrcidValueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.*;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserAttributesDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.DataType;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.Operator;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ViewColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ForeignKeyReference;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.foreignKey.ReferenceType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.primaryKey.PrimaryKey;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessage;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessageType;
import at.ac.tuwien.ifs.dbrepo.core.test.utils.ArrayUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Database 1 "weather" (Private Data, Private Schema, User 1) -> Container 1
 * <ul>
 * <li>Table 1 (Private Data, Private Schema)</li>
 * <li>Table 2 (Private Data, Public Schema)</li>
 * <li>Table 3 (Private Data, Private Schema)</li>
 * <li>Table 4 (Public Data, Private Schema)</li>
 * <li>Query 1</li>
 * <li>View 1 (Private Data, Private Schema)</li>
 * <li>View 2 (Public Data, Public Schema)</li>
 * <li>View 3 (Public Data, Private Schema)</li>
 * <li>Identifier 1 (Title=en, Description=en, type=database)</li>
 * <li>Identifier 2 (Title=en, Description=en, type=subset, queryId=1)</li>
 * <li>Identifier 3 (Title=en, Description=en, type=view, viewId=1)</li>
 * <li>Identifier 4 (Title=en, Description=en, type=table, tableId=1)</li>
 * </ul>
 * <p>
 * Database 2 "zoo" (Private Data, Public Schema, User 2) -> Container 1
 * <ul>
 * <li>Table 5 (Public Data, Public Schema)</li>
 * <li>Table 6 (Public Data, Private Schema)</li>
 * <li>Table 7 (Public Data, Public Schema)</li>
 * <li>Query 2</li>
 * <li>Query 6</li>
 * <li>View 4 (Public Data, Private Schema)</li>
 * <li>Identifier 5 (Title=de, Description=de)</li>
 * </ul>
 * <p>
 * Database 3 (Public Data, Private Schema, User 3) -> Container 1
 * <ul>
 * <li>Table 8 (Private Data, Private Schema)</li>
 * <li>Query 3</li>
 * <li>Query 4</li>
 * <li>Query 5</li>
 * <li>View 5 (Public Data, Public Schema)</li>
 * <li>Identifier 6 (Title=en, Description=en, Query=3)</li>
 * </ul>
 * <p>
 * Database 4 (Public Data, Public Schema, User 4) -> Container 4
 * <ul>
 * <li>Table 9</li>
 * <li>Identifier 7</li>
 * <li>Query 7</li>
 * </ul>
 */
@TestPropertySource(locations = "classpath:application.properties")
public class BaseTest {

    public final static SimpleDateFormat MARIADB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public final static String MINIO_IMAGE = "minio/minio:RELEASE.2024-06-06T09-36-42Z";
    @Deprecated
    public final static String MARIADB_IMAGE = "bitnamilegacy/mariadb:11.3.2";
    public final static String POSTGRESQL_IMAGE = "postgres:18-alpine";
    public final static String RABBITMQ_IMAGE = "rabbitmq:3.13.7";
    public final static String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.4.4";
    public final static String REDIS_IMAGE = "docker.io/redis:5.0.3-alpine";

    public final static String REALM_DBREPO_NAME = "dbrepo";

    public final static String[] DEFAULT_SEMANTICS_HANDLING = new String[]{"default-semantics-handling",
            "create-semantic-unit", "execute-semantic-query", "table-semantic-analyse", "create-semantic-concept"};

    public final static String[] DEFAULT_VIEW_HANDLING = new String[]{"update-database-view", "create-database-view",
            "delete-database-view", "list-database-views", "modify-view-visibility", "find-database-view"};

    public final static String[] ESCALATED_SEMANTICS_HANDLING = new String[]{"escalated-semantics-handling",
            "update-semantic-concept", "modify-foreign-table-column-semantics", "delete-ontology", "list-ontologies",
            "update-semantic-unit", "create-ontology", "update-ontology"};

    public final static String[] DEFAULT_CONTAINER_HANDLING = new String[]{"default-container-handling",
            "create-container", "list-containers", "modify-container-state"};

    public final static String[] ESCALATED_CONTAINER_HANDLING = new String[]{"escalated-container-handling",
            "modify-foreign-container-state", "delete-container"};

    public final static String[] DEFAULT_DATABASE_HANDLING = new String[]{"default-database-handling",
            "update-database-access", "modify-database-visibility", "create-database", "modify-database-owner",
            "delete-database-access", "check-database-access", "list-databases", "modify-database-image",
            "create-database-access", "find-database", "import-database-data", "analyse-datatypes"};

    public final static String[] ESCALATED_DATABASE_HANDLING = new String[]{"escalated-database-handling",
            "delete-database"};

    public final static String[] DEFAULT_IDENTIFIER_HANDLING = new String[]{"default-identifier-handling",
            "create-identifier", "find-identifier", "list-identifiers", "publish-identifier", "delete-identifier"};

    public final static String[] ESCALATED_IDENTIFIER_HANDLING = new String[]{"escalated-identifier-handling",
            "modify-identifier-metadata", "update-foreign-identifier", "create-foreign-identifier"};

    public final static String[] DEFAULT_QUERY_HANDLING = new String[]{"default-query-handling", "view-table-data",
            "execute-query", "view-table-history", "list-database-views", "export-query-data", "create-database-view",
            "delete-database-view", "delete-table-data", "export-table-data", "persist-query", "re-execute-query",
            "insert-table-data", "find-database-view"};

    public final static String[] ESCALATED_QUERY_HANDLING = new String[]{"escalated-query-handling"};

    public final static String[] DEFAULT_TABLE_HANDLING = new String[]{"default-table-handling", "list-tables",
            "create-table", "modify-table-column-semantics", "find-table", "delete-table", "update-table-statistic",
            "update-table"};

    public final static String[] ESCALATED_TABLE_HANDLING = new String[]{"escalated-table-handling",
            "delete-foreign-table"};

    public final static String[] DEFAULT_USER_HANDLING = new String[]{"default-user-handling", "modify-user-theme",
            "modify-user-information"};

    public final static String[] ESCALATED_USER_HANDLING = new String[]{"escalated-user-handling", "find-user"};

    public final static String[] DEFAULT_RESEARCHER_ROLES = ArrayUtils.merge(List.of(
            new String[]{"default-researcher-roles"}, DEFAULT_CONTAINER_HANDLING, DEFAULT_DATABASE_HANDLING,
            DEFAULT_IDENTIFIER_HANDLING, DEFAULT_QUERY_HANDLING, DEFAULT_TABLE_HANDLING, DEFAULT_USER_HANDLING,
            DEFAULT_SEMANTICS_HANDLING, DEFAULT_VIEW_HANDLING));

    public final static String[] DEFAULT_DEVELOPER_ROLES = ArrayUtils.merge(List.of(
            new String[]{"default-developer-roles"}, DEFAULT_CONTAINER_HANDLING, DEFAULT_DATABASE_HANDLING,
            DEFAULT_IDENTIFIER_HANDLING, DEFAULT_QUERY_HANDLING, DEFAULT_TABLE_HANDLING, DEFAULT_USER_HANDLING,
            ESCALATED_USER_HANDLING, ESCALATED_CONTAINER_HANDLING, ESCALATED_DATABASE_HANDLING,
            ESCALATED_IDENTIFIER_HANDLING, ESCALATED_QUERY_HANDLING, ESCALATED_TABLE_HANDLING, DEFAULT_VIEW_HANDLING));

    public final static String[] DEFAULT_DATA_STEWARD_ROLES = ArrayUtils.merge(List.of(
            new String[]{"default-data-steward-roles"}, ESCALATED_IDENTIFIER_HANDLING, DEFAULT_SEMANTICS_HANDLING,
            ESCALATED_SEMANTICS_HANDLING, DEFAULT_VIEW_HANDLING));

    public final static String[] DEFAULT_LOCAL_ADMIN_ROLES = ArrayUtils.merge(List.of(new String[]{"system"},
            DEFAULT_RESEARCHER_ROLES));

    public final List<GrantedAuthorityDto> AUTHORITY_LOCAL_ADMIN_ROLES =
            Arrays.stream(DEFAULT_LOCAL_ADMIN_ROLES)
                    .map(GrantedAuthorityDto::new)
                    .toList();

    public final List<GrantedAuthorityDto> AUTHORITY_DEFAULT_RESEARCHER_ROLES =
            Arrays.stream(DEFAULT_RESEARCHER_ROLES)
                    .map(GrantedAuthorityDto::new)
                    .toList();

    public final List<GrantedAuthorityDto> AUTHORITY_DEFAULT_DEVELOPER_ROLES =
            Arrays.stream(DEFAULT_DEVELOPER_ROLES)
                    .map(GrantedAuthorityDto::new)
                    .toList();

    public final List<GrantedAuthorityDto> AUTHORITY_DEFAULT_DATA_STEWARD_ROLES =
            Arrays.stream(DEFAULT_DATA_STEWARD_ROLES)
                    .map(GrantedAuthorityDto::new)
                    .toList();

    public final List<GrantedAuthority> AUTHORITY_DEFAULT_LOCAL_ADMIN_AUTHORITIES =
            AUTHORITY_LOCAL_ADMIN_ROLES
                    .stream()
                    .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a.getAuthority()))
                    .toList();

    public final List<GrantedAuthority> AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES =
            AUTHORITY_DEFAULT_RESEARCHER_ROLES
                    .stream()
                    .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a.getAuthority()))
                    .toList();

    public final List<GrantedAuthority> AUTHORITY_DEFAULT_DEVELOPER_AUTHORITIES =
            AUTHORITY_DEFAULT_DEVELOPER_ROLES
                    .stream()
                    .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a.getAuthority()))
                    .toList();

    public final List<GrantedAuthority> AUTHORITY_DEFAULT_DATA_STEWARD_AUTHORITIES =
            AUTHORITY_DEFAULT_DATA_STEWARD_ROLES
                    .stream()
                    .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a.getAuthority()))
                    .toList();

    public final CreateAccessDto UPDATE_DATABASE_ACCESS_READ_DTO = CreateAccessDto.builder()
            .type(AccessTypeDto.READ)
            .build();

    public final CreateAccessDto UPDATE_DATABASE_ACCESS_WRITE_OWN_DTO = CreateAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .build();

    public final CreateAccessDto UPDATE_DATABASE_ACCESS_WRITE_ALL_DTO = CreateAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .build();

    @SuppressWarnings("java:S6418")
    public final static String TOKEN_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ0Mk9DZUNoZUo5dXdvQmJOUWpHX25ONldLaUxjY2VUSUFabWlUYkdPREZNIn0.eyJleHAiOjE3NDQxMTI1MzksImlhdCI6MTc0NDExMTYzOSwiYXV0aF90aW1lIjoxNzQ0MDkzNTMwLCJqdGkiOiI2MWNlODZjNi1kOTYzLTQxOTUtODE2NS00MTdiNDBkZjNhMmUiLCJpc3MiOiJodHRwczovL2RicmVwbzEuZWMudHV3aWVuLmFjLmF0L3JlYWxtcy9kYnJlcG8iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiN2JkMWE2MDEtZjYyOS00MjA3LWEyMDEtNTY3MDRiYzI5ZTVlIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZGJyZXBvLWNsaWVudCIsInNpZCI6ImQ5Y2FjN2NiLTc2OTctNGM3OS1iODRhLWViN2ViYzgzNDFhZCIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJkZWxldGUtZGF0YWJhc2UtdmlldyIsImV4cG9ydC1xdWVyeS1kYXRhIiwiZXhlY3V0ZS1xdWVyeSIsImRlZmF1bHQtdXNlci1oYW5kbGluZyIsImRlbGV0ZS10YWJsZS1kYXRhIiwiZmluZC1xdWVyeSIsImxpc3QtZGF0YWJhc2Utdmlld3MiLCJwZXJzaXN0LXF1ZXJ5IiwiZGVsZXRlLWRhdGFiYXNlLWFjY2VzcyIsInZpZXctdGFibGUtaGlzdG9yeSIsIm1vZGlmeS11c2VyLXRoZW1lIiwibW9kaWZ5LXZpZXctdmlzaWJpbGl0eSIsImNyZWF0ZS1zZW1hbnRpYy1jb25jZXB0IiwiZGVmYXVsdC1jb250YWluZXItaGFuZGxpbmciLCJjcmVhdGUtdGFibGUiLCJkZWZhdWx0LWJyb2tlci1oYW5kbGluZyIsImV4ZWN1dGUtc2VtYW50aWMtcXVlcnkiLCJ0YWJsZS1zZW1hbnRpYy1hbmFseXNlIiwiY2hlY2stZGF0YWJhc2UtYWNjZXNzIiwiZGVmYXVsdC12aWV3LWhhbmRsaW5nIiwiZGVsZXRlLWlkZW50aWZpZXIiLCJtb2RpZnktZGF0YWJhc2Utb3duZXIiLCJsaXN0LXRhYmxlcyIsImV4cG9ydC10YWJsZS1kYXRhIiwiY3JlYXRlLWRhdGFiYXNlLWFjY2VzcyIsInJlLWV4ZWN1dGUtcXVlcnkiLCJjcmVhdGUtc2VtYW50aWMtdW5pdCIsInVwZGF0ZS10YWJsZS1zdGF0aXN0aWMiLCJkZWZhdWx0LWRhdGFiYXNlLWhhbmRsaW5nIiwiZmluZC1kYXRhYmFzZSIsImZpbmQtZGF0YWJhc2UtdmlldyIsImltcG9ydC1kYXRhYmFzZS1kYXRhIiwicHVibGlzaC1pZGVudGlmaWVyIiwidXBkYXRlLWRhdGFiYXNlLXZpZXciLCJkZWZhdWx0LXJvbGVzLWRicmVwbyIsImNyZWF0ZS1kYXRhYmFzZSIsImRlZmF1bHQtcmVzZWFyY2hlci1yb2xlcyIsImRlZmF1bHQtaWRlbnRpZmllci1oYW5kbGluZyIsIm1vZGlmeS11c2VyLWluZm9ybWF0aW9uIiwiY3JlYXRlLWRhdGFiYXNlLXZpZXciLCJmaW5kLWNvbnRhaW5lciIsImluc2VydC10YWJsZS1kYXRhIiwidXBkYXRlLXRhYmxlIiwibW9kaWZ5LWRhdGFiYXNlLWltYWdlIiwibW9kaWZ5LXRhYmxlLWNvbHVtbi1zZW1hbnRpY3MiLCJkZWZhdWx0LXNlbWFudGljcy1oYW5kbGluZyIsInVwZGF0ZS1kYXRhYmFzZS1hY2Nlc3MiLCJkZWZhdWx0LXF1ZXJ5LWhhbmRsaW5nIiwiZmluZC10YWJsZSIsImxpc3QtcXVlcmllcyIsImNyZWF0ZS1pZGVudGlmaWVyIiwiZmluZC1pZGVudGlmaWVyIiwidmlldy10YWJsZS1kYXRhIiwiZGVmYXVsdC1zdG9yYWdlLXJvbGVzIiwiZGVmYXVsdC10YWJsZS1oYW5kbGluZyIsImxpc3QtaWRlbnRpZmllcnMiLCJsaXN0LWRhdGFiYXNlcyIsIm1vZGlmeS1kYXRhYmFzZS12aXNpYmlsaXR5IiwidXBsb2FkLWZpbGUiLCJkZWxldGUtdGFibGUiXX0sInNjb3BlIjoib3BlbmlkIiwidWlkIjoiNmY1YTc0MzQtYTQwOS0xMDNmLTk2NmItMTFiNjU4OGRkOTEzIiwiaWRlbnRpdHlfcHJvdmlkZXIiOiJzYW1sIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiMjg3NzIyIiwiZ2l2ZW5fbmFtZSI6Ik1hcnRpbiIsImZhbWlseV9uYW1lIjoiV2Vpc2UifQ.Nfp0MIKuqjrEZqQXjPNRU2MuYyIJXhQVjdg7XY5_oqkYIngCoQ0y3ioBhMGT2XHd8kufk7FEP6Kme9Ihvm1Qx6rAejcSLaA6xnhQDrX6SGGQ9Kfm_9Ewv6IHoX--Yt3aKLu_YQ4eiDdxxEP4jbl-H6hM4_vwaJYe7vcfSE1lkewno_yYrhW6btPRfrbLy4_57vBK6MLN1h8A-ePx1037KnIXIRDOu0hZwidz4mVZjQ6x3arBYT9iFQmIkgucLMriuRPF_PoEHkUajJ06Y9xQuSa9MNtr_ALkUEbGnzBCAeNwIChavxdz7Be_x1qRTeOsdVD0mHJf_ePeXjmDUtV45w";
    public final static String TOKEN_ACCESS_SCOPE = "openid";

    public final TokenDto TOKEN_DTO = TokenDto.builder()
            .accessToken(TOKEN_ACCESS_TOKEN)
            .scope(TOKEN_ACCESS_SCOPE)
            .build();

    public final Token TOKEN_LOCAL_ADMIN_CACHE = Token.builder()
            .token(TOKEN_ACCESS_TOKEN)
            .username(USER_LOCAL_ADMIN_USERNAME)
            .build();

    public final static UUID CONCEPT_1_ID = UUID.fromString("8cabc011-4bdf-44d4-9d33-b2648e2ddbf1");
    public final static String CONCEPT_1_NAME = "precipitation";
    public final static String CONCEPT_1_URI = "http://www.wikidata.org/entity/Q25257";
    public final static String CONCEPT_1_DESCRIPTION = null;
    public final static Instant CONCEPT_1_CREATED = Instant.ofEpochSecond(1701976048L) /* 2023-12-07 19:07:27 (UTC) */;

    public final static UUID CONCEPT_2_ID = UUID.fromString("c5cf9914-15c1-4813-af11-eb2a070d59a9");
    public final static String CONCEPT_2_NAME = "FAIR data";
    public final static String CONCEPT_2_URI = "http://www.wikidata.org/entity/Q29032648";
    public final static String CONCEPT_2_DESCRIPTION = "data compliant with the terms of the FAIR Data Principles";
    public final static Instant CONCEPT_2_CREATED = Instant.ofEpochSecond(1701976049L) /* 2023-12-07 19:07:28 (UTC) */;

    public final static UUID UNIT_1_ID = UUID.fromString("1fee60e4-42f8-4883-85a8-e282fddf6a62");
    public final static String UNIT_1_NAME = "millimetre";
    public final static String UNIT_1_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/millimetre";
    public final static String UNIT_1_DESCRIPTION = "The millimetre is a unit of length defined as 1.0e-3 metre.";
    public final static Instant UNIT_1_CREATED = Instant.ofEpochSecond(1701976282L) /* 2023-12-07 19:11:22 */;

    public final static UUID UNIT_2_ID = UUID.fromString("d88591a9-5171-4b12-8381-bcff1cfe7442");
    public final static String UNIT_2_NAME = "tonne";
    public final static String UNIT_2_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/tonne";
    public final static String UNIT_2_DESCRIPTION = "The tonne is a unit of mass defined as 1000 kilogram.";
    public final static Instant UNIT_2_CREATED = Instant.ofEpochSecond(1701976462L) /* 2023-12-07 19:14:22 */;

    public final static String USER_BROKER_USERNAME = "guest";
    @SuppressWarnings("java:S2068")
    public final static String USER_BROKER_PASSWORD = "guest";

    public final static UUID USER_LOCAL_ADMIN_ID = UUID.fromString("a54dcb2e-a644-4e82-87e7-05a96413983d");
    public final static String USER_LOCAL_ADMIN_USERNAME = "admin";
    @SuppressWarnings("java:S2068")
    public final static String USER_LOCAL_ADMIN_PASSWORD = "admin";
    public final static String USER_LOCAL_ADMIN_THEME = "dark";
    public final static String USER_LOCAL_ADMIN_LANGUAGE = "en";
    public final static Boolean USER_LOCAL_ADMIN_ENABLED = true;
    @SuppressWarnings("java:S2068")
    public final static String USER_LOCAL_ADMIN_MARIADB_PASSWORD = "s3cr3t1nf0rm4t10n";

    public final UserAttributesDto USER_LOCAL_ADMIN_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_LOCAL_ADMIN_THEME)
            .postgresPassword(USER_LOCAL_ADMIN_MARIADB_PASSWORD)
            .language(USER_LOCAL_ADMIN_LANGUAGE)
            .build();

    public final UserDetails USER_LOCAL_ADMIN_DETAILS = org.springframework.security.core.userdetails.User.builder()
            .username(USER_LOCAL_ADMIN_USERNAME)
            .password(USER_LOCAL_ADMIN_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_LOCAL_ADMIN_AUTHORITIES)
            .build();

    public final UserDto USER_LOCAL_DTO = UserDto.builder()
            .id(USER_LOCAL_ADMIN_ID)
            .username(USER_LOCAL_ADMIN_USERNAME)
            .attributes(USER_LOCAL_ADMIN_ATTRIBUTES_DTO)
            .build();

    public final Principal USER_LOCAL_ADMIN_PRINCIPAL = new UsernamePasswordAuthenticationToken(
            USER_LOCAL_ADMIN_DETAILS, USER_LOCAL_ADMIN_PASSWORD, USER_LOCAL_ADMIN_DETAILS.getAuthorities());

    public final static UUID USER_1_ID = UUID.fromString("cd5bab0d-7799-4069-85fb-c5d738572a0b");
    public final static String USER_1_USERNAME = "junit1";
    @SuppressWarnings("java:S2068")
    public final static String USER_1_PASSWORD = "712!1e7021c4d077662543620bbC5";
    @SuppressWarnings("java:S2068")
    public final static String USER_1_DATABASE_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_1_FIRSTNAME = "John";
    public final static String USER_1_LASTNAME = "Doe";
    public final static String USER_1_QUALIFIED_NAME = USER_1_FIRSTNAME + " " + USER_1_LASTNAME + " — @" + USER_1_USERNAME;
    public final static String USER_1_NAME = "John Doe";
    public final static String USER_1_AFFILIATION = "TU Graz";
    public final static String USER_1_ORCID_URL = "https://orcid.org/0000-0003-4216-302X";
    public final static Boolean USER_1_ENABLED = true;
    public final static Boolean USER_1_IS_INTERNAL = false;
    public final static String USER_1_THEME = "light";
    public final static String USER_1_LANGUAGE = "en";
    public final static Instant USER_1_CREATED = Instant.ofEpochSecond(1677399441L) /* 2023-02-26 08:17:21 (UTC) */;

    public final UpdateUserPasswordDto USER_1_UPDATE_PASSWORD_DTO = UpdateUserPasswordDto.builder()
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .build();

    public final UserAttributesDto USER_1_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_1_THEME)
            .orcid(USER_1_ORCID_URL)
            .affiliation(USER_1_AFFILIATION)
            .postgresPassword(USER_1_DATABASE_PASSWORD)
            .language(USER_1_LANGUAGE)
            .build();

    public final CredentialDto USER_1_KEYCLOAK_CREDENTIAL_1 = CredentialDto.builder()
            .type(CredentialTypeDto.PASSWORD)
            .temporary(false)
            .value(USER_1_PASSWORD)
            .build();

    public final CredentialDto USER_LOCAL_KEYCLOAK_CREDENTIAL_1 = CredentialDto.builder()
            .type(CredentialTypeDto.PASSWORD)
            .temporary(false)
            .value(USER_LOCAL_ADMIN_PASSWORD)
            .build();

    public final UserCreateDto USER_1_KEYCLOAK_SIGNUP_REQUEST = UserCreateDto.builder()
            .username(USER_1_USERNAME)
            .enabled(USER_1_ENABLED)
            .credentials(new LinkedList<>(List.of(USER_1_KEYCLOAK_CREDENTIAL_1)))
            .attributes(UserCreateAttributesDto.builder()
                    .ldapId(String.valueOf(USER_1_ID))
                    .build())
            .build();

    public final UserCreateDto USER_LOCAL_KEYCLOAK_SIGNUP_REQUEST = UserCreateDto.builder()
            .username(USER_LOCAL_ADMIN_USERNAME)
            .enabled(USER_LOCAL_ADMIN_ENABLED)
            .credentials(new LinkedList<>(List.of(USER_LOCAL_KEYCLOAK_CREDENTIAL_1)))
            .groups(new LinkedList<>(List.of("system")))
            .attributes(UserCreateAttributesDto.builder()
                    .ldapId(String.valueOf(USER_LOCAL_ADMIN_ID))
                    .build())
            .build();

    public final UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .attributes(USER_1_ATTRIBUTES_DTO)
            .name(USER_1_NAME)
            .qualifiedName(USER_1_QUALIFIED_NAME)
            .password(USER_1_DATABASE_PASSWORD)
            .build();

    public final User USER_1_CACHE = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .password(USER_1_DATABASE_PASSWORD)
            .build();

    public final UserUpdateDto USER_1_UPDATE_DTO = UserUpdateDto.builder()
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .affiliation(USER_1_AFFILIATION)
            .orcid(USER_1_ORCID_URL)
            .theme(USER_1_THEME)
            .language(USER_1_LANGUAGE)
            .build();

    public final UserPasswordDto USER_1_PASSWORD_DTO = UserPasswordDto.builder()
            .password(USER_1_PASSWORD)
            .build();

    public final UserBriefDto USER_1_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .name(USER_1_NAME)
            .qualifiedName(USER_1_QUALIFIED_NAME)
            .orcid(USER_1_ORCID_URL)
            .build();

    public final UserBriefDto USER_1_MINIMAL_DTO = UserBriefDto.builder()
            .username(USER_1_USERNAME)
            .build();

    public final UserDetails USER_1_DETAILS = org.springframework.security.core.userdetails.User.builder()
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final Principal USER_1_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_1_DETAILS,
            USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());

    public final static UUID USER_2_ID = UUID.fromString("eeb9a51b-4cd8-4039-90bf-e24f17372f7c");
    public final static String USER_2_USERNAME = "junit2";
    public final static String USER_2_FIRSTNAME = "Jane";
    public final static String USER_2_LASTNAME = "Doe";
    public final static String USER_2_NAME = "Jane Doe";
    public final static String USER_2_AFFILIATION = "TU Wien";
    public final static String USER_2_ORCID_URL = "https://orcid.org/0000-0002-9272-6225";
    @SuppressWarnings("java:S2068")
    public final static String USER_2_PASSWORD = "3B4^30099d6e27b4715ba003a9d3b";
    @SuppressWarnings("java:S2068")
    public final static String USER_2_DATABASE_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_2_QUALIFIED_NAME = USER_2_FIRSTNAME + " " + USER_2_LASTNAME + " — @" + USER_2_USERNAME;
    public final static Boolean USER_2_IS_INTERNAL = false;
    public final static String USER_2_THEME = "light";
    public final static String USER_2_LANGUAGE = "de";

    public final UserAttributesDto USER_2_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_2_THEME)
            .orcid(USER_2_ORCID_URL)
            .affiliation(USER_2_AFFILIATION)
            .postgresPassword(USER_2_DATABASE_PASSWORD)
            .language(USER_2_LANGUAGE)
            .build();

    public final UserDto USER_2_DTO = UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .name(USER_2_NAME)
            .qualifiedName(USER_2_QUALIFIED_NAME)
            .attributes(USER_2_ATTRIBUTES_DTO)
            .password(USER_2_DATABASE_PASSWORD)
            .build();

    public final User USER_2_CACHE = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .password(USER_2_DATABASE_PASSWORD)
            .build();

    public final UserBriefDto USER_2_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .name(USER_2_NAME)
            .orcid(USER_2_ORCID_URL)
            .qualifiedName(USER_2_QUALIFIED_NAME)
            .build();

    public final UserBriefDto USER_2_MINIMAL_DTO = UserBriefDto.builder()
            .username(USER_2_USERNAME)
            .build();

    public final UserDetails USER_2_DETAILS = org.springframework.security.core.userdetails.User.builder()
            .username(USER_2_USERNAME)
            .password(USER_2_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final Principal USER_2_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_2_DETAILS,
            USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());

    public final static UUID USER_3_ID = UUID.fromString("7b080e33-d8db-4276-9d53-47208e657006");
    public final static String USER_3_USERNAME = "junit3";
    public final static String USER_3_FIRSTNAME = "System";
    public final static String USER_3_LASTNAME = "System";
    public final static String USER_3_NAME = "System System";
    public final static String USER_3_AFFILIATION = "TU Wien";
    public final static String USER_3_ORCID_URL = null;
    @SuppressWarnings("java:S2068")
    public final static String USER_3_PASSWORD = "c6b@74Ea27a52820570c739e3c022";
    @SuppressWarnings("java:S2068")
    public final static String USER_3_DATABASE_PASSWORD = "*3cr3t1nf0rm4t10n";
    public final static String USER_3_QUALIFIED_NAME = USER_3_FIRSTNAME + " " + USER_3_LASTNAME + " — @" + USER_3_USERNAME;
    public final static Boolean USER_3_IS_INTERNAL = false;
    public final static String USER_3_THEME = "light";

    public final UserAttributesDto USER_3_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_3_THEME)
            .orcid(USER_3_ORCID_URL)
            .affiliation(USER_3_AFFILIATION)
            .postgresPassword(USER_3_DATABASE_PASSWORD)
            .build();

    public final UserDto USER_3_DTO = UserDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .name(USER_3_NAME)
            .qualifiedName(USER_3_QUALIFIED_NAME)
            .attributes(USER_3_ATTRIBUTES_DTO)
            .password(USER_3_DATABASE_PASSWORD)
            .build();

    public final User USER_3_CACHE = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .password(USER_3_DATABASE_PASSWORD)
            .build();

    public final UserBriefDto USER_3_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .name(USER_3_NAME)
            .qualifiedName(USER_3_QUALIFIED_NAME)
            .build();

    public final UserBriefDto USER_3_MINIMAL_DTO = UserBriefDto.builder()
            .username(USER_3_USERNAME)
            .build();

    public final UserDetails USER_3_DETAILS = org.springframework.security.core.userdetails.User.builder()
            .username(USER_3_USERNAME)
            .password(USER_3_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final Principal USER_3_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_3_DETAILS,
            USER_3_PASSWORD, USER_3_DETAILS.getAuthorities());

    public final static UUID USER_4_ID = UUID.fromString("791d58c5-bfab-4520-b4fc-b44d4ab9feb0");
    public final static String USER_4_USERNAME = "junit4";
    public final static String USER_4_FIRSTNAME = "JUnit";
    public final static String USER_4_LASTNAME = "4";
    public final static String USER_4_NAME = "JUnit 4";
    public final static String USER_4_AFFILIATION = "TU Wien";
    public final static String USER_4_ORCID_URL = null;
    @SuppressWarnings("java:S2068")
    public final static String USER_4_PASSWORD = "deb&6E361784ae1cbebbc3bf5fd50";
    @SuppressWarnings("java:S2068")
    public final static String USER_4_DATABASE_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_4_QUALIFIED_NAME = USER_4_FIRSTNAME + " " + USER_4_LASTNAME + " — @" + USER_4_USERNAME;
    public final static Boolean USER_4_IS_INTERNAL = false;
    public final static String USER_4_THEME = "light";

    public final UserAttributesDto USER_4_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_4_THEME)
            .orcid(USER_4_ORCID_URL)
            .affiliation(USER_4_AFFILIATION)
            .postgresPassword(USER_4_DATABASE_PASSWORD)
            .build();

    public final UserDto USER_4_DTO = UserDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .name(USER_4_NAME)
            .attributes(USER_4_ATTRIBUTES_DTO)
            .qualifiedName(USER_4_QUALIFIED_NAME)
            .password(USER_4_DATABASE_PASSWORD)
            .build();

    public final User USER_4_CACHE = User.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .password(USER_4_DATABASE_PASSWORD)
            .build();

    public final UserBriefDto USER_4_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .name(USER_4_NAME)
            .qualifiedName(USER_4_QUALIFIED_NAME)
            .build();

    public final UserBriefDto USER_4_MINIMAL_DTO = UserBriefDto.builder()
            .username(USER_4_USERNAME)
            .build();

    public final UserDetails USER_4_DETAILS = org.springframework.security.core.userdetails.User.builder()
            .username(USER_4_USERNAME)
            .password(USER_4_PASSWORD)
            .build();

    public final Principal USER_4_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_4_DETAILS,
            USER_4_PASSWORD, USER_4_DETAILS.getAuthorities());

    public final static UUID USER_5_ID = UUID.fromString("28ff851d-d7bc-4422-959c-edd7a5b15630");
    public final static String USER_5_USERNAME = "nobody";
    public final static String USER_5_FIRSTNAME = "No";
    public final static String USER_5_LASTNAME = "Body";
    public final static String USER_5_NAME = "No Body";
    public final static String USER_5_AFFILIATION = "TU Wien";
    @SuppressWarnings("java:S2068")
    public final static String USER_5_PASSWORD = "24d$fec836B956ada9c722f3cd403";
    @SuppressWarnings("java:S2068")
    public final static String USER_5_DATABASE_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_5_QUALIFIED_NAME = USER_5_FIRSTNAME + " " + USER_5_LASTNAME + " — @" + USER_5_USERNAME;
    public final static Boolean USER_5_IS_INTERNAL = false;
    public final static String USER_5_THEME = "dark";

    public final UserAttributesDto USER_5_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_5_THEME)
            .affiliation(USER_5_AFFILIATION)
            .postgresPassword(USER_5_DATABASE_PASSWORD)
            .build();

    public final UserDto USER_5_DTO = UserDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .name(USER_5_NAME)
            .qualifiedName(USER_5_QUALIFIED_NAME)
            .attributes(USER_5_ATTRIBUTES_DTO)
            .password(USER_5_DATABASE_PASSWORD)
            .build();

    public final User USER_5_CACHE = User.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .password(USER_5_DATABASE_PASSWORD)
            .build();

    public final UserBriefDto USER_5_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .qualifiedName(USER_5_QUALIFIED_NAME)
            .build();

    public final UserBriefDto USER_5_MINIMAL_DTO = UserBriefDto.builder()
            .username(USER_5_USERNAME)
            .build();

    public final UserDetails USER_5_DETAILS = org.springframework.security.core.userdetails.User.builder()
            .username(USER_5_USERNAME)
            .password(USER_5_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_DEVELOPER_AUTHORITIES)
            .build();

    public final Principal USER_5_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_5_DETAILS,
            USER_5_PASSWORD, USER_5_DETAILS.getAuthorities());

    public final static UUID USER_6_ID = UUID.fromString("28ff851d-d7bc-4422-959c-edd7a5b15630");
    public final static String USER_6_USERNAME = "system";
    public final static String USER_6_FIRSTNAME = "System";
    public final static String USER_6_LASTNAME = "System";
    public final static String USER_6_NAME = "System System";
    public final static String USER_6_AFFILIATION = "TU Wien";
    public final static String USER_6_ORCID = null;
    @SuppressWarnings("java:S2068")
    public final static String USER_6_PASSWORD = "006%381aBa58e072ef21244c0e3d9";
    @SuppressWarnings("java:S2068")
    public final static String USER_6_DATABASE_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static Boolean USER_6_VERIFIED = true;
    public final static Boolean USER_6_ENABLED = true;
    public final static Boolean USER_6_IS_INTERNAL = false;
    public final static String USER_6_THEME = "light";
    public final static Instant USER_6_CREATED = Instant.ofEpochSecond(1677399592L) /* 2023-02-26 08:19:52 (UTC) */;

    public final UserAttributesDto USER_6_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_6_THEME)
            .affiliation(USER_6_AFFILIATION)
            .postgresPassword(USER_6_DATABASE_PASSWORD)
            .build();

    public final UserDto USER_6_DTO = UserDto.builder()
            .id(USER_6_ID)
            .username(USER_6_USERNAME)
            .firstname(USER_6_FIRSTNAME)
            .lastname(USER_6_LASTNAME)
            .password(USER_6_DATABASE_PASSWORD)
            .attributes(USER_6_ATTRIBUTES_DTO)
            .build();

    public final UserDetails USER_6_DETAILS = org.springframework.security.core.userdetails.User.builder()
            .username(USER_6_USERNAME)
            .password(USER_6_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final Principal USER_6_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_6_DETAILS,
            USER_6_PASSWORD, USER_6_DETAILS.getAuthorities());

    public final static UUID IMAGE_1_ID = UUID.fromString("bcb85554-4087-4d38-9604-ae89eaccb72f");
    public final static String IMAGE_1_REGISTRY = "docker.io";
    public final static String IMAGE_1_NAME = "postgres";
    public final static String IMAGE_1_VERSION = "18-alpine";
    public final static String IMAGE_1_DIALECT = "org.hibernate.dialect.PostgreSQLDialect";
    public final static String IMAGE_1_DRIVER = "org.postgresql.Driver";
    public final static String IMAGE_1_JDBC_METHOD = "postgresql";
    public final static Integer IMAGE_1_DEFAULT_PORT = 5432;
    public final static Boolean IMAGE_1_IS_DEFAULT = true;

    public final ImageCreateDto IMAGE_1_CREATE_DTO = ImageCreateDto.builder()
            .registry(IMAGE_1_REGISTRY)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC_METHOD)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_DEFAULT_PORT)
            .build();

    public final ImageChangeDto IMAGE_1_CHANGE_DTO = ImageChangeDto.builder()
            .registry(IMAGE_1_REGISTRY)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC_METHOD)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_DEFAULT_PORT)
            .build();

    public final ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .registry(IMAGE_1_REGISTRY)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC_METHOD)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_DEFAULT_PORT)
            .isDefault(IMAGE_1_IS_DEFAULT)
            .operators(null /* IMAGE_1_OPERATORS */)
            .dataTypes(null /* IMAGE_1_DATA_TYPES */)
            .build();

    public final ImageDto IMAGE_1_DTO = ImageDto.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .registry(IMAGE_1_REGISTRY)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC_METHOD)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_DEFAULT_PORT)
            .isDefault(IMAGE_1_IS_DEFAULT)
            .operators(null /* IMAGE_1_OPERATORS_DTO */)
            .dataTypes(null /* IMAGE_1_DATA_TYPES_DTO */)
            .build();

    public final Image IMAGE_1_CACHE = Image.builder()
            .id(IMAGE_1_ID)
            .jdbcMethod(IMAGE_1_JDBC_METHOD)
            .operators(null /* IMAGE_1_OPERATORS_CACHE */)
            .dataTypes(null /* IMAGE_1_DATA_TYPES_CACHE */)
            .build();

    public final ImageBriefDto IMAGE_1_BRIEF_DTO = ImageBriefDto.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .isDefault(IMAGE_1_IS_DEFAULT)
            .build();

    public final static UUID IMAGE_1_OPERATORS_1_ID = UUID.fromString("42a56348-38bd-4aba-b0f2-ac813d5d2da1");
    public final static String IMAGE_1_OPERATORS_1_DISPLAY_NAME = "XOR";
    public final static String IMAGE_1_OPERATORS_1_VALUE = "XOR";
    public final static String IMAGE_1_OPERATORS_1_DOCUMENTATION = "https://mariadb.com/kb/en/xor/";
    public final static UUID IMAGE_1_OPERATORS_2_ID = UUID.fromString("42a56348-38bd-4aba-b0f2-ac813d5d2da2");
    public final static String IMAGE_1_OPERATORS_2_DISPLAY_NAME = "=";
    public final static String IMAGE_1_OPERATORS_2_VALUE = "=";
    public final static String IMAGE_1_OPERATORS_2_DOCUMENTATION = "https://mariadb.com/kb/en/equal/";

    public final List<Operator> IMAGE_1_OPERATORS = new LinkedList<>(List.of(
            Operator.builder()
                    .id(IMAGE_1_OPERATORS_1_ID)
                    .image(IMAGE_1)
                    .displayName(IMAGE_1_OPERATORS_1_DISPLAY_NAME)
                    .value(IMAGE_1_OPERATORS_1_VALUE)
                    .documentation(IMAGE_1_OPERATORS_1_DOCUMENTATION)
                    .build(),
            Operator.builder()
                    .id(IMAGE_1_OPERATORS_2_ID)
                    .image(IMAGE_1)
                    .displayName(IMAGE_1_OPERATORS_2_DISPLAY_NAME)
                    .value(IMAGE_1_OPERATORS_2_VALUE)
                    .documentation(IMAGE_1_OPERATORS_2_DOCUMENTATION)
                    .build()));

    public final List<OperatorDto> IMAGE_1_OPERATORS_DTO = new LinkedList<>(List.of(
            OperatorDto.builder()
                    .id(IMAGE_1_OPERATORS_1_ID)
                    .displayName(IMAGE_1_OPERATORS_1_DISPLAY_NAME)
                    .value(IMAGE_1_OPERATORS_1_VALUE)
                    .documentation(IMAGE_1_OPERATORS_1_DOCUMENTATION)
                    .build(),
            OperatorDto.builder()
                    .id(IMAGE_1_OPERATORS_2_ID)
                    .displayName(IMAGE_1_OPERATORS_2_DISPLAY_NAME)
                    .value(IMAGE_1_OPERATORS_2_VALUE)
                    .documentation(IMAGE_1_OPERATORS_2_DOCUMENTATION)
                    .build()));

    public final List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.Operator> IMAGE_1_OPERATORS_CACHE = new LinkedList<>(List.of(
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.Operator.builder()
                    .id(IMAGE_1_OPERATORS_1_ID)
                    .value(IMAGE_1_OPERATORS_1_VALUE)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.Operator.builder()
                    .id(IMAGE_1_OPERATORS_2_ID)
                    .value(IMAGE_1_OPERATORS_2_VALUE)
                    .build()));

    public final static UUID IMAGE_1_DATA_TYPE_1_ID = UUID.fromString("d79cb089-363c-488b-9717-649e44d8fcc5");
    public final static String IMAGE_1_DATA_TYPE_1_DISPLAY_NAME = "BIGINT(size)";
    public final static String IMAGE_1_DATA_TYPE_1_VALUE = "bigint";
    public final static Integer IMAGE_1_DATA_TYPE_1_SIZE_MIN = 0;
    public final static Boolean IMAGE_1_DATA_TYPE_1_SIZE_REQUIRED = false;
    public final static Integer IMAGE_1_DATA_TYPE_1_SIZE_STEP = 1;
    public final static Boolean IMAGE_1_DATA_TYPE_1_D_REQUIRED = false;
    public final static String IMAGE_1_DATA_TYPE_1_DOCUMENTATION = "https://mariadb.com/kb/en/bigint/";
    public final static Boolean IMAGE_1_DATA_TYPE_1_IS_QUOTED = false;
    public final static Boolean IMAGE_1_DATA_TYPE_1_IS_BUILDABLE = false;

    public final static UUID IMAGE_1_DATA_TYPE_2_ID = UUID.fromString("dd577ef5-803d-4a14-8687-8f2b362bf523");
    public final static String IMAGE_1_DATA_TYPE_2_VALUE = "date";

    public final static UUID IMAGE_1_DATA_TYPE_3_ID = UUID.fromString("74161156-9b14-412b-a7b2-cb953681c023");
    public final static String IMAGE_1_DATA_TYPE_3_VALUE = "varchar";

    public final List<DataType> IMAGE_1_DATA_TYPES = new LinkedList<>(List.of(
            DataType.builder()
                    .id(IMAGE_1_DATA_TYPE_1_ID)
                    .displayName(IMAGE_1_DATA_TYPE_1_DISPLAY_NAME)
                    .value(IMAGE_1_DATA_TYPE_1_VALUE)
                    .sizeMin(IMAGE_1_DATA_TYPE_1_SIZE_MIN)
                    .sizeRequired(IMAGE_1_DATA_TYPE_1_SIZE_REQUIRED)
                    .sizeStep(IMAGE_1_DATA_TYPE_1_SIZE_STEP)
                    .dRequired(IMAGE_1_DATA_TYPE_1_D_REQUIRED)
                    .documentation(IMAGE_1_DATA_TYPE_1_DOCUMENTATION)
                    .quoted(IMAGE_1_DATA_TYPE_1_IS_QUOTED)
                    .buildable(IMAGE_1_DATA_TYPE_1_IS_BUILDABLE)
                    .build()
    ));

    public final List<DataTypeDto> IMAGE_1_DATA_TYPES_DTO = new LinkedList<>(List.of(
            DataTypeDto.builder()
                    .id(IMAGE_1_DATA_TYPE_1_ID)
                    .displayName(IMAGE_1_DATA_TYPE_1_DISPLAY_NAME)
                    .value(IMAGE_1_DATA_TYPE_1_VALUE)
                    .sizeMin(IMAGE_1_DATA_TYPE_1_SIZE_MIN)
                    .sizeRequired(IMAGE_1_DATA_TYPE_1_SIZE_REQUIRED)
                    .sizeStep(IMAGE_1_DATA_TYPE_1_SIZE_STEP)
                    .dRequired(IMAGE_1_DATA_TYPE_1_D_REQUIRED)
                    .documentation(IMAGE_1_DATA_TYPE_1_DOCUMENTATION)
                    .quoted(IMAGE_1_DATA_TYPE_1_IS_QUOTED)
                    .buildable(IMAGE_1_DATA_TYPE_1_IS_BUILDABLE)
                    .build()
    ));

    public final List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType> IMAGE_1_DATA_TYPES_CACHE = new LinkedList<>(List.of(
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType.builder()
                    .id(IMAGE_1_DATA_TYPE_1_ID)
                    .value(IMAGE_1_DATA_TYPE_1_VALUE)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType.builder()
                    .id(IMAGE_1_DATA_TYPE_2_ID)
                    .value(IMAGE_1_DATA_TYPE_2_VALUE)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType.builder()
                    .id(IMAGE_1_DATA_TYPE_3_ID)
                    .value(IMAGE_1_DATA_TYPE_3_VALUE)
                    .build()
    ));

    public final static UUID CONTAINER_1_ID = UUID.fromString("7ddb7e87-b965-43a2-9a24-4fa406d998f4");
    public final static String CONTAINER_1_NAME = "u01";
    public final static String CONTAINER_1_INTERNAL_NAME = "dbrepo-userdb-u01";
    public final static String CONTAINER_1_UI_HOST = "localhost";
    public final static Integer CONTAINER_1_UI_PORT = 3306;
    public final static String CONTAINER_1_UI_ADDITIONAL_FLAGS = "?sslMode=disable";
    public final static Integer CONTAINER_1_QUOTA = 4;
    public final static Integer CONTAINER_1_COUNT = 3;
    public final static String CONTAINER_1_HOST = "localhost";
    public final static Integer CONTAINER_1_PORT = 3308;
    public final static String CONTAINER_1_PRIVILEGED_USERNAME = "root";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_1_PRIVILEGED_PASSWORD = "dbrepo";
    public final static String CONTAINER_1_READONLY_USERNAME = "readonly";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_1_READONLY_PASSWORD = "readonly";
    public final static String CONTAINER_1_READONLY_HASHED_PASSWORD = "*440BA4FD1A87A0999647DB67C0EE258198B247BA";
    public final static Instant CONTAINER_1_CREATED = Instant.ofEpochSecond(1677399629L) /* 2023-02-26 08:20:29 (UTC) */;

    public final Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNAL_NAME)
            .image(IMAGE_1)
            .created(CONTAINER_1_CREATED)
            .host(CONTAINER_1_HOST)
            .port(CONTAINER_1_PORT)
            .uiHost(CONTAINER_1_UI_HOST)
            .uiPort(CONTAINER_1_UI_PORT)
            .quota(CONTAINER_1_QUOTA)
            .uiAdditionalFlags(CONTAINER_1_UI_ADDITIONAL_FLAGS)
            .privilegedUsername(CONTAINER_1_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_1_PRIVILEGED_PASSWORD)
            .databases(new LinkedList<>())
            .build();

    public final ContainerDto CONTAINER_1_DTO = ContainerDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNAL_NAME)
            .image(IMAGE_1_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container CONTAINER_1_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container.builder()
            .id(CONTAINER_1_ID)
            .internalName(CONTAINER_1_INTERNAL_NAME)
            .image(IMAGE_1_CACHE)
            .host(CONTAINER_1_HOST)
            .port(CONTAINER_1_PORT)
            .username(CONTAINER_1_PRIVILEGED_USERNAME)
            .password(CONTAINER_1_PRIVILEGED_PASSWORD)
            .build();

    public final ContainerBriefDto CONTAINER_1_BRIEF_DTO = ContainerBriefDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNAL_NAME)
            .quota(CONTAINER_1_QUOTA)
            .count(CONTAINER_1_COUNT)
            .image(IMAGE_1_BRIEF_DTO)
            .build();

    public final ContainerDto CONTAINER_1_PRIVILEGED_DTO = ContainerDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNAL_NAME)
            .image(IMAGE_1_DTO)
            .build();

    public final static UUID CONTAINER_2_ID = UUID.fromString("c2ec601e-2bfb-4be8-8891-0cb804a08d4a");
    public final static String CONTAINER_2_NAME = "u02";
    public final static String CONTAINER_2_INTERNAL_NAME = "dbrepo-userdb-u02";
    public final static String CONTAINER_2_HOST = "localhost";
    public final static Integer CONTAINER_2_PORT = 3308;
    public final static Integer CONTAINER_2_QUOTA = 3;
    public final static Integer CONTAINER_2_COUNT = 3;
    public final static String CONTAINER_2_PRIVILEGED_USERNAME = "root";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_2_PRIVILEGED_PASSWORD = "dbrepo";
    public final static Instant CONTAINER_2_CREATED = Instant.ofEpochSecond(1677399655L) /* 2023-02-26 08:20:55 (UTC) */;

    public final Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNAL_NAME)
            .image(IMAGE_1)
            .created(CONTAINER_2_CREATED)
            .host(CONTAINER_2_HOST)
            .port(CONTAINER_2_PORT)
            .quota(CONTAINER_2_QUOTA)
            .databases(new LinkedList<>(List.of()))
            .privilegedUsername(CONTAINER_2_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_2_PRIVILEGED_PASSWORD)
            .build();

    public final ContainerDto CONTAINER_2_DTO = ContainerDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNAL_NAME)
            .image(IMAGE_1_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container CONTAINER_2_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container.builder()
            .id(CONTAINER_2_ID)
            .internalName(CONTAINER_2_INTERNAL_NAME)
            .image(IMAGE_1_CACHE)
            .host(CONTAINER_2_HOST)
            .port(CONTAINER_2_PORT)
            .username(CONTAINER_2_PRIVILEGED_USERNAME)
            .password(CONTAINER_2_PRIVILEGED_PASSWORD)
            .build();

    public final ContainerBriefDto CONTAINER_2_DTO_BRIEF = ContainerBriefDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNAL_NAME)
            .quota(CONTAINER_2_QUOTA)
            .build();

    public final ContainerDto CONTAINER_2_PRIVILEGED_DTO = ContainerDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNAL_NAME)
            .image(IMAGE_1_DTO)
            .build();

    public final static UUID CONTAINER_3_ID = UUID.fromString("1731c7d2-8bd1-4392-85bc-18a3be99e01d");
    public final static String CONTAINER_3_NAME = "u03";
    public final static String CONTAINER_3_INTERNAL_NAME = "dbrepo-userdb-u03";
    public final static String CONTAINER_3_HOST = "localhost";
    public final static Integer CONTAINER_3_PORT = 3308;
    public final static Integer CONTAINER_3_QUOTA = 20;
    public final static String CONTAINER_3_PRIVILEGED_USERNAME = "root";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_3_PRIVILEGED_PASSWORD = "dbrepo";
    public final static Instant CONTAINER_3_CREATED = Instant.ofEpochSecond(1677399672L) /* 2023-02-26 08:21:12 (UTC) */;

    public final Container CONTAINER_3 = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .internalName(CONTAINER_3_INTERNAL_NAME)
            .image(IMAGE_1)
            .created(CONTAINER_3_CREATED)
            .host(CONTAINER_3_HOST)
            .port(CONTAINER_3_PORT)
            .quota(CONTAINER_3_QUOTA)
            .databases(new LinkedList<>())
            .privilegedUsername(CONTAINER_3_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_3_PRIVILEGED_PASSWORD)
            .build();

    public final static UUID CONTAINER_4_ID = UUID.fromString("67aee75c-791c-410b-abbb-175c11ddd252");
    public final static String CONTAINER_4_NAME = "u04";
    public final static String CONTAINER_4_INTERNAL_NAME = "dbrepo-userdb-u04";
    public final static String CONTAINER_4_HOST = "localhost";
    public final static Integer CONTAINER_4_PORT = 3308;
    public final static Integer CONTAINER_4_QUOTA = 0;
    public final static String CONTAINER_4_PRIVILEGED_USERNAME = "root";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_4_PRIVILEGED_PASSWORD = "dbrepo";
    public final static Instant CONTAINER_4_CREATED = Instant.ofEpochSecond(1677399688L) /* 2023-02-26 08:21:28 (UTC) */;

    public final Container CONTAINER_4 = Container.builder()
            .id(CONTAINER_4_ID)
            .name(CONTAINER_4_NAME)
            .internalName(CONTAINER_4_INTERNAL_NAME)
            .image(IMAGE_1)
            .created(CONTAINER_4_CREATED)
            .host(CONTAINER_4_HOST)
            .port(CONTAINER_4_PORT)
            .quota(CONTAINER_4_QUOTA)
            .privilegedUsername(CONTAINER_4_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_4_PRIVILEGED_PASSWORD)
            .databases(null) /* DATABASE_4 */
            .build();

    public final ContainerDto CONTAINER_4_DTO = ContainerDto.builder()
            .id(CONTAINER_4_ID)
            .name(CONTAINER_4_NAME)
            .internalName(CONTAINER_4_INTERNAL_NAME)
            .image(IMAGE_1_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container CONTAINER_4_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container.builder()
            .id(CONTAINER_4_ID)
            .internalName(CONTAINER_4_INTERNAL_NAME)
            .image(IMAGE_1_CACHE)
            .host(CONTAINER_4_HOST)
            .port(CONTAINER_4_PORT)
            .username(CONTAINER_4_PRIVILEGED_USERNAME)
            .password(CONTAINER_4_PRIVILEGED_PASSWORD)
            .build();

    public final static String EXCHANGE_DBREPO_NAME = "dbrepo";
    public final static Boolean EXCHANGE_DBREPO_AUTO_DELETE = true;
    public final static Boolean EXCHANGE_DBREPO_DURABLE = true;
    public final static Boolean EXCHANGE_DBREPO_INTERNAL = true;
    public final static String EXCHANGE_DBREPO_TYPE = "topic";
    public final static String EXCHANGE_DBREPO_VHOST = "dbrepo";

    public final ExchangeDto EXCHANGE_DBREPO_DTO = ExchangeDto.builder()
            .autoDelete(EXCHANGE_DBREPO_AUTO_DELETE)
            .type(EXCHANGE_DBREPO_TYPE)
            .name(EXCHANGE_DBREPO_NAME)
            .durable(EXCHANGE_DBREPO_DURABLE)
            .vhost(EXCHANGE_DBREPO_VHOST)
            .internal(EXCHANGE_DBREPO_INTERNAL)
            .build();

    public final static UUID DATABASE_1_ID = UUID.fromString("b3bcb5bf-4f88-40e2-9726-9b0d2ee2b425");
    public final static String DATABASE_1_NAME = "Weather";
    public final static String DATABASE_1_DESCRIPTION = "Weather in Australia";
    public final static String DATABASE_1_INTERNAL_NAME = "weather";
    public final static Boolean DATABASE_1_PUBLIC = false;
    public final static Boolean DATABASE_1_SCHEMA_PUBLIC = false;
    public final static Boolean DATABASE_1_DASHBOARD_ENABLED = false;
    public final static String DATABASE_1_DASHBOARD_UID = "730f0bdde6cf1b";
    public final static String DATABASE_1_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_1_CREATED = Instant.ofEpochSecond(1677399741L) /* 2023-02-26 08:22:21 (UTC) */;
    public final static Instant DATABASE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399741L) /* 2023-02-26 08:22:21 (UTC) */;

    public final CreateDatabaseDto DATABASE_1_CREATE = CreateDatabaseDto.builder()
            .name(DATABASE_1_NAME)
            .isPublic(DATABASE_1_PUBLIC)
            .cid(CONTAINER_1_ID)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto DATABASE_1_CREATE_INTERNAL = at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto.builder()
            .internalName(DATABASE_1_INTERNAL_NAME)
            .containerId(CONTAINER_1_ID)
            .username(USER_1_USERNAME)
            .build();

    public final static UUID DATABASE_2_ID = UUID.fromString("dd9dfee2-9fbd-46b0-92d5-98f0f8866ffe");
    public final static String DATABASE_2_NAME = "Zoo";
    public final static String DATABASE_2_DESCRIPTION = "Zoo data";
    public final static String DATABASE_2_INTERNAL_NAME = "zoo";
    public final static Boolean DATABASE_2_PUBLIC = false;
    public final static Boolean DATABASE_2_SCHEMA_PUBLIC = true;
    public final static Boolean DATABASE_2_DASHBOARD_ENABLED = true;
    public final static String DATABASE_2_DASHBOARD_UID = "c6ab10f377148c";
    public final static String DATABASE_2_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_2_CREATED = Instant.ofEpochSecond(1677399772L) /* 2023-02-26 08:22:52 (UTC) */;
    public final static Instant DATABASE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677399772L) /* 2023-02-26 08:22:52 (UTC) */;

    public final CreateDatabaseDto DATABASE_2_CREATE = CreateDatabaseDto.builder()
            .name(DATABASE_2_NAME)
            .isPublic(DATABASE_2_PUBLIC)
            .cid(CONTAINER_1_ID)
            .build();

    public final static UUID DATABASE_3_ID = UUID.fromString("9d8cb9a9-9468-4801-a2e0-2dac8bc67c31");
    public final static String DATABASE_3_NAME = "Musicology";
    public final static String DATABASE_3_DESCRIPTION = "Musicology data";
    public final static String DATABASE_3_INTERNAL_NAME = "musicology";
    public final static Boolean DATABASE_3_PUBLIC = true;
    public final static Boolean DATABASE_3_SCHEMA_PUBLIC = false;
    public final static String DATABASE_3_DASHBOARD_UID = "96ef37d5d1b0d1";
    public final static Boolean DATABASE_3_DASHBOARD_ENABLED = true;
    public final static String DATABASE_3_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_3_CREATED = Instant.ofEpochSecond(1677399792L) /* 2023-02-26 08:23:12 (UTC) */;
    public final static Instant DATABASE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677399792L) /* 2023-02-26 08:23:12 (UTC) */;

    public final DatabaseDto DATABASE_3_DTO = DatabaseDto.builder()
            .id(DATABASE_3_ID)
            .isPublic(DATABASE_3_PUBLIC)
            .isSchemaPublic(DATABASE_3_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_3_DASHBOARD_ENABLED)
            .dashboardUid(DATABASE_3_DASHBOARD_UID)
            .name(DATABASE_3_NAME)
            .internalName(DATABASE_3_INTERNAL_NAME)
            .owner(USER_3_BRIEF_DTO)
            .container(CONTAINER_1_DTO)
            .exchangeName(DATABASE_3_EXCHANGE)
            .tables(new LinkedList<>()) /* TABLE_8_DTO */
            .views(new LinkedList<>()) /* VIEW_5_DTO */
            .identifiers(new LinkedList<>()) /* IDENTIFIER_6_DTO */
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database DATABASE_3_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database.builder()
            .id(DATABASE_3_ID)
            .isPublic(DATABASE_3_PUBLIC)
            .isSchemaPublic(DATABASE_3_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_3_DASHBOARD_ENABLED)
            .internalName(DATABASE_3_INTERNAL_NAME)
            .ownedBy(USER_3_USERNAME)
            .container(CONTAINER_1_CACHE)
            .tables(new LinkedList<>()) /* TABLE_8_CACHE */
            .views(new LinkedList<>()) /* VIEW_5_CACHE */
            .build();

    public final DatabaseBriefDto DATABASE_3_BRIEF_DTO = DatabaseBriefDto.builder()
            .id(DATABASE_3_ID)
            .isPublic(DATABASE_3_PUBLIC)
            .isSchemaPublic(DATABASE_3_SCHEMA_PUBLIC)
            .name(DATABASE_3_NAME)
            .internalName(DATABASE_3_INTERNAL_NAME)
            .ownedBy(USER_3_USERNAME)
            .identifiers(new LinkedList<>())
            .build();

    public final CreateDatabaseDto DATABASE_3_CREATE = CreateDatabaseDto.builder()
            .name(DATABASE_3_NAME)
            .isPublic(DATABASE_3_PUBLIC)
            .cid(CONTAINER_1_ID)
            .build();

    public final static UUID DATABASE_4_ID = UUID.fromString("c503d7f3-5952-4d97-b26a-da86bea4c20d");
    public final static String DATABASE_4_NAME = "Weather AT";
    public final static String DATABASE_4_DESCRIPTION = "Weather data";
    public final static Boolean DATABASE_4_PUBLIC = true;
    public final static Boolean DATABASE_4_SCHEMA_PUBLIC = true;
    public final static Boolean DATABASE_4_DASHBOARD_ENABLED = true;
    public final static String DATABASE_4_DASHBOARD_UID = "045e44890411ef";
    public final static String DATABASE_4_INTERNAL_NAME = "weather_at";
    public final static String DATABASE_4_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_4_CREATED = Instant.ofEpochSecond(1677399813L) /* 2023-02-26 08:23:33 (UTC) */;
    public final static Instant DATABASE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677399813L) /* 2023-02-26 08:23:33 (UTC) */;

    public final DatabaseBriefDto DATABASE_4_BRIEF_DTO = DatabaseBriefDto.builder()
            .id(DATABASE_4_ID)
            .isPublic(DATABASE_4_PUBLIC)
            .isSchemaPublic(DATABASE_4_SCHEMA_PUBLIC)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .internalName(DATABASE_4_INTERNAL_NAME)
            .ownedBy(USER_4_USERNAME)
            .identifiers(new LinkedList<>())
            .build();

    public final DatabaseDto DATABASE_4_DTO = DatabaseDto.builder()
            .id(DATABASE_4_ID)
            .isPublic(DATABASE_4_PUBLIC)
            .isSchemaPublic(DATABASE_4_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_4_DASHBOARD_ENABLED)
            .name(DATABASE_4_NAME)
            .container(CONTAINER_2_DTO)
            .description(DATABASE_4_DESCRIPTION)
            .internalName(DATABASE_4_INTERNAL_NAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .owner(USER_4_BRIEF_DTO)
            .tables(new LinkedList<>()) /* TABLE_9_DTO */
            .views(new LinkedList<>())
            .identifiers(new LinkedList<>()) /* IDENTIFIER_7_DTO */
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database DATABASE_4_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database.builder()
            .id(DATABASE_4_ID)
            .isPublic(DATABASE_4_PUBLIC)
            .isSchemaPublic(DATABASE_4_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_4_DASHBOARD_ENABLED)
            .container(CONTAINER_2_CACHE)
            .internalName(DATABASE_4_INTERNAL_NAME)
            .ownedBy(USER_4_USERNAME)
            .tables(new LinkedList<>()) /* TABLE_9_DTO */
            .views(new LinkedList<>())
            .build();

    public final CreateTableConstraintsDto TABLE_1_CREATE_CONSTRAINTS_DTO = CreateTableConstraintsDto.builder()
            .uniques(new LinkedList<>())
            .foreignKeys(new LinkedList<>())
            .build();

    public final CreateTableDto TABLE_0_CREATE_DTO = CreateTableDto.builder()
            .name("full")
            .description("full example")
            .constraints(TABLE_1_CREATE_CONSTRAINTS_DTO)
            .columns(List.of(CreateTableColumnDto.builder()
                            .name("col1a")
                            .type(ColumnTypeDto.CHAR)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col1b")
                            .type(ColumnTypeDto.CHAR)
                            .nullAllowed(true)
                            .size(50L)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col2a")
                            .type(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col2b")
                            .type(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .size(1024L)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col3")
                            .type(ColumnTypeDto.BINARY)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col4")
                            .type(ColumnTypeDto.VARBINARY)
                            .nullAllowed(true)
                            .size(200L)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col5")
                            .type(ColumnTypeDto.TINYBLOB)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col6")
                            .type(ColumnTypeDto.TINYTEXT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col7")
                            .type(ColumnTypeDto.TEXT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col8")
                            .type(ColumnTypeDto.BLOB)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col9")
                            .type(ColumnTypeDto.MEDIUMTEXT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col10")
                            .type(ColumnTypeDto.MEDIUMBLOB)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col11")
                            .type(ColumnTypeDto.LONGTEXT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col12")
                            .type(ColumnTypeDto.LONGBLOB)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col13")
                            .type(ColumnTypeDto.ENUM)
                            .nullAllowed(true)
                            .enums(new LinkedList<>(List.of("val1", "val2")))
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col14")
                            .type(ColumnTypeDto.SET)
                            .nullAllowed(true)
                            .sets(new LinkedList<>(List.of("val1", "val2")))
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col15")
                            .type(ColumnTypeDto.BIT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col16")
                            .type(ColumnTypeDto.TINYINT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col17")
                            .type(ColumnTypeDto.BOOL)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col18")
                            .type(ColumnTypeDto.SMALLINT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col19")
                            .type(ColumnTypeDto.MEDIUMINT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col20")
                            .type(ColumnTypeDto.INT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col21")
                            .type(ColumnTypeDto.BIGINT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col22")
                            .type(ColumnTypeDto.FLOAT)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col23")
                            .type(ColumnTypeDto.DOUBLE)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col24")
                            .type(ColumnTypeDto.DECIMAL)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col25")
                            .type(ColumnTypeDto.DATE)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col26")
                            .type(ColumnTypeDto.DATETIME)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col27")
                            .type(ColumnTypeDto.TIMESTAMP)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col28")
                            .type(ColumnTypeDto.TIME)
                            .nullAllowed(true)
                            .build(),
                    CreateTableColumnDto.builder()
                            .name("col29")
                            .type(ColumnTypeDto.YEAR)
                            .nullAllowed(true)
                            .build()))
            .build();

    public final static UUID TABLE_1_ID = UUID.fromString("666d0b6b-f017-4f7c-80d8-a47174d8b539");
    public final static String TABLE_1_NAME = "Weather AUS";
    public final static String TABLE_1_INTERNAL_NAME = "weather_aus";
    public final static Boolean TABLE_1_VERSIONED = true;
    public final static Boolean TABLE_1_IS_PUBLIC = false;
    public final static Boolean TABLE_1_SCHEMA_PUBLIC = false;
    public final static String TABLE_1_DESCRIPTION = "Weather in Australia";
    public final static String TABLE_1_QUEUE_NAME = TABLE_1_INTERNAL_NAME;
    public final static String TABLE_1_ROUTING_KEY = "dbrepo." + DATABASE_1_ID + "." + TABLE_1_ID;
    public final static Long TABLE_1_AVG_ROW_LENGTH = 3L;
    public final static Long TABLE_1_NUM_ROWS = 3L;
    public final static Long TABLE_1_DATA_LENGTH = 2000L;
    public final static Long TABLE_1_MAX_DATA_LENGTH = Long.MAX_VALUE;
    public final static Instant TABLE_1_CREATED = Instant.ofEpochSecond(1677399975L) /* 2023-02-26 08:26:15 (UTC) */;
    public final static Instant TABLE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399975L) /* 2023-02-26 08:26:15 (UTC) */;

    public final Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .tdbid(DATABASE_1_ID)
            .database(null /* DATABASE_1 */)
            .created(TABLE_1_CREATED)
            .internalName(TABLE_1_INTERNAL_NAME)
            .isVersioned(TABLE_1_VERSIONED)
            .isPublic(TABLE_1_IS_PUBLIC)
            .isSchemaPublic(TABLE_1_SCHEMA_PUBLIC)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .identifiers(new LinkedList<>())
            .columns(new LinkedList<>()) /* TABLE_1_COLUMNS */
            .constraints(null) /* TABLE_1_CONSTRAINTS */
            .ownedBy(USER_1_USERNAME)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .avgRowLength(TABLE_1_AVG_ROW_LENGTH)
            .numRows(TABLE_1_NUM_ROWS)
            .dataLength(TABLE_1_DATA_LENGTH)
            .maxDataLength(TABLE_1_MAX_DATA_LENGTH)
            .build();

    public final static UUID COLUMN_1_1_ID = UUID.fromString("377c0a6e-938e-458c-ad2b-bbbd75d46412");
    public final static UUID COLUMN_1_2_ID = UUID.fromString("dbca4821-3023-479b-a25a-c08eb0ec02ce");
    public final static UUID COLUMN_1_3_ID = UUID.fromString("8ff0351e-4882-4948-94af-598e4b264b25");
    public final static UUID COLUMN_1_4_ID = UUID.fromString("9ab256eb-3324-4e76-af3b-e3e2a58ce161");
    public final static UUID COLUMN_1_5_ID = UUID.fromString("619e9355-51aa-438f-8579-80cec30f35cb");

    public final List<Column> TABLE_1_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_1_1_ID)
                    .internalName("id")
                    .columnType(ColumnType.SERIAL)
                    .build(),
            Column.builder()
                    .id(COLUMN_1_2_ID)
                    .internalName("date")
                    .columnType(ColumnType.DATE)
                    .build(),
            Column.builder()
                    .id(COLUMN_1_3_ID)
                    .internalName("location")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            Column.builder()
                    .id(COLUMN_1_4_ID)
                    .internalName("mintemp")
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            Column.builder()
                    .id(COLUMN_1_5_ID)
                    .internalName("rainfall")
                    .columnType(ColumnType.DECIMAL)
                    .build());

    public final List<ColumnDto> TABLE_1_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_1_1_ID)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .name("id")
                    .internalName("id")
                    .columnType(ColumnTypeDto.SERIAL)
                    .isNullAllowed(false)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_1_2_ID)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .name("Date")
                    .internalName("date")
                    .columnType(ColumnTypeDto.DATE)
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_1_3_ID)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .name("Location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_1_4_ID)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_1_5_ID)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(4)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .conceptUri(CONCEPT_1_URI)
                    .unit(UNIT_1_URI)
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build());

    public final CreateTableConstraintsDto TABLE_1_CREATE_TABLE_CONSTRAINTS_DTO =
            CreateTableConstraintsDto.builder()
                    .checks(new LinkedHashSet<>())
                    .primaryKey(new LinkedHashSet<>(List.of("id")))
                    .foreignKeys(new LinkedList<>())
                    .uniques(new LinkedList<>(List.of(List.of("date"))))
                    .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_1_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_1_ID)
            .internalName(TABLE_1_INTERNAL_NAME)
            .isPublic(TABLE_1_IS_PUBLIC)
            .isSchemaPublic(TABLE_1_SCHEMA_PUBLIC)
            .columns(new LinkedList<>()) /* TABLE_1_COLUMNS_CACHE */
            .ownedBy(USER_1_USERNAME)
            .avgRowLength(TABLE_1_AVG_ROW_LENGTH)
            .numRows(TABLE_1_NUM_ROWS)
            .dataLength(TABLE_1_DATA_LENGTH)
            .maxDataLength(TABLE_1_MAX_DATA_LENGTH)
            .build();

    public final TableDto TABLE_1_DTO = TableDto.builder()
            .id(TABLE_1_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_1_INTERNAL_NAME)
            .isVersioned(TABLE_1_VERSIONED)
            .isPublic(TABLE_1_IS_PUBLIC)
            .isSchemaPublic(TABLE_1_SCHEMA_PUBLIC)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .identifiers(new LinkedList<>())
            .columns(new LinkedList<>()) /* TABLE_1_COLUMNS_DTO */
            .constraints(null)
            .owner(USER_1_BRIEF_DTO)
            .avgRowLength(TABLE_1_AVG_ROW_LENGTH)
            .numRows(TABLE_1_NUM_ROWS)
            .dataLength(TABLE_1_DATA_LENGTH)
            .maxDataLength(TABLE_1_MAX_DATA_LENGTH)
            .build();

    public final TableBriefDto TABLE_1_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_1_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_1_INTERNAL_NAME)
            .isVersioned(TABLE_1_VERSIONED)
            .isPublic(TABLE_1_IS_PUBLIC)
            .isSchemaPublic(TABLE_1_SCHEMA_PUBLIC)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static Long TABLE_1_DATA_COUNT = 3L;

    @SuppressWarnings("java:S3599")
    public final List<Map<String, Object>> TABLE_1_DATA_DTO = new LinkedList<>(List.of(
            new HashMap<>() {{
                put("id", BigInteger.valueOf(1L));
                put("date", LocalDate.of(2008, 12, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
                put("location", "Albury");
                put("mintemp", 13.4);
                put("rainfall", 0.6);
            }},
            new HashMap<>() {{
                put("id", BigInteger.valueOf(2L));
                put("date", LocalDate.of(2008, 12, 2).atStartOfDay().toInstant(ZoneOffset.UTC));
                put("location", "Albury");
                put("mintemp", 7.4);
                put("rainfall", 0);
            }},
            new HashMap<>() {{
                put("id", BigInteger.valueOf(3L));
                put("date", LocalDate.of(2008, 12, 3).atStartOfDay().toInstant(ZoneOffset.UTC));
                put("location", "Albury");
                put("mintemp", 12.9);
                put("rainfall", 0);
            }}
    ));

    public final static UUID TABLE_2_ID = UUID.fromString("0cc067b6-4e81-4871-b47e-17a38228a574");
    public final static String TABLE_2_NAME = "Weather Location";
    public final static String TABLE_2_INTERNAL_NAME = "weather_location";
    public final static Boolean TABLE_2_VERSIONED = true;
    public final static Boolean TABLE_2_IS_PUBLIC = false;
    public final static Boolean TABLE_2_SCHEMA_PUBLIC = true;
    public final static String TABLE_2_DESCRIPTION = "Weather location";
    public final static String TABLE_2_QUEUE_NAME = TABLE_2_INTERNAL_NAME;
    public final static String TABLE_2_ROUTING_KEY = "dbrepo." + DATABASE_1_ID + "." + TABLE_2_ID;
    public final static Instant TABLE_2_CREATED = Instant.ofEpochSecond(1677400007L) /* 2023-02-26 08:26:47 (UTC) */;
    public final static Instant TABLE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677400007L) /* 2023-02-26 08:26:47 (UTC) */;
    public final static Long TABLE_2_AVG_ROW_LENGTH = 3L;
    public final static Long TABLE_2_NUM_ROWS = 3L;
    public final static Long TABLE_2_DATA_LENGTH = 2000L;
    public final static Long TABLE_2_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final Table TABLE_2 = Table.builder()
            .id(TABLE_2_ID)
            .tdbid(DATABASE_1_ID)
            .database(null /* DATABASE_1 */)
            .created(TABLE_2_CREATED)
            .internalName(TABLE_2_INTERNAL_NAME)
            .isVersioned(TABLE_2_VERSIONED)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .queueName(TABLE_2_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_2_COLUMNS */
            .constraints(null) /* TABLE_2_CONSTRAINTS */
            .ownedBy(USER_2_USERNAME)
            .avgRowLength(TABLE_2_AVG_ROW_LENGTH)
            .numRows(TABLE_2_NUM_ROWS)
            .dataLength(TABLE_2_DATA_LENGTH)
            .maxDataLength(TABLE_2_MAX_DATA_LENGTH)
            .build();

    public final TableBriefDto TABLE_2_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_2_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_2_INTERNAL_NAME)
            .isVersioned(TABLE_2_VERSIONED)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .ownedBy(USER_2_USERNAME)
            .build();

    public final static UUID COLUMN_2_1_ID = UUID.fromString("795faa78-7ebb-4dd5-9eb1-e54a9192d0b5");
    public final static UUID COLUMN_2_2_ID = UUID.fromString("f316ced5-7774-4656-aa7f-a874622d99b3");
    public final static UUID COLUMN_2_3_ID = UUID.fromString("11cb1aa2-8582-45ef-a3bb-7056aa94cdf1");

    public final ColumnBriefDto TABLE_1_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_1_1_ID)
            .name("id")
            .internalName("id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final List<ColumnDto> TABLE_2_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_2_1_ID)
                    .tableId(TABLE_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_2_2_ID)
                    .tableId(TABLE_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .name("lat")
                    .internalName("lat")
                    .columnType(ColumnTypeDto.DOUBLE)
                    .size(22L)
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_2_3_ID)
                    .tableId(TABLE_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(ColumnTypeDto.DOUBLE)
                    .size(22L)
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build());

    public final List<Column> TABLE_2_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_2_1_ID)
                    .internalName("location")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            Column.builder()
                    .id(COLUMN_2_2_ID)
                    .internalName("lat")
                    .columnType(ColumnType.DOUBLE)
                    .build(),
            Column.builder()
                    .id(COLUMN_2_3_ID)
                    .internalName("lng")
                    .columnType(ColumnType.DOUBLE)
                    .build());

    public final ColumnBriefDto TABLE_2_COLUMNS_BRIEF_2_DTO = ColumnBriefDto.builder()
            .id(COLUMN_2_3_ID)
            .name("lng")
            .internalName("lng")
            .columnType(ColumnTypeDto.DECIMAL)
            .build();

    public final List<ColumnBriefDto> TABLE_2_COLUMNS_BRIEF_DTO = List.of(ColumnBriefDto.builder()
                    .id(COLUMN_2_1_ID)
                    .tableId(TABLE_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .build(),
            ColumnBriefDto.builder()
                    .id(COLUMN_2_2_ID)
                    .tableId(TABLE_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .name("lat")
                    .internalName("lat")
                    .columnType(ColumnTypeDto.DOUBLE)
                    .build(),
            ColumnBriefDto.builder()
                    .id(COLUMN_2_3_ID)
                    .tableId(TABLE_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .name("lng")
                    .internalName("lng")
                    .columnType(ColumnTypeDto.DOUBLE)
                    .build());

    public final ColumnBriefDto TABLE_2_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_2_1_ID)
            .name("location")
            .internalName("location")
            .columnType(ColumnTypeDto.VARCHAR)
            .build();

    public final TableDto TABLE_2_DTO = TableDto.builder()
            .id(TABLE_2_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_2_INTERNAL_NAME)
            .isVersioned(TABLE_2_VERSIONED)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .columns(new LinkedList<>())
            .constraints(ConstraintsDto.builder()
                    .checks(new LinkedHashSet<>(List.of("`mintemp` > 0")))
                    .foreignKeys(new LinkedList<>(List.of(ForeignKeyDto.builder()
                            .id(UUID.fromString("ca833111-1e9a-48a3-bb16-ad6f90196f96"))
                            .name("fk_location")
                            .onDelete(ReferenceTypeDto.NO_ACTION)
                            .references(new LinkedList<>(List.of(ForeignKeyReferenceDto.builder()
                                    .id(UUID.fromString("8552f282-0403-424d-b2ba-4ed0f760197c"))
                                    .column(TABLE_2_COLUMNS_BRIEF_2_DTO)
                                    .referencedColumn(TABLE_1_COLUMNS_BRIEF_0_DTO)
                                    .foreignKey(null)
                                    .build())))
                            .table(TABLE_1_BRIEF_DTO)
                            .referencedTable(TABLE_2_BRIEF_DTO)
                            .onUpdate(ReferenceTypeDto.NO_ACTION)
                            .build())))
                    .uniques(new LinkedList<>(List.of(UniqueDto.builder()
                            .id(UUID.fromString("b9aba807-dd9c-43a3-9614-2493cb4b26bd"))
                            .table(TABLE_2_BRIEF_DTO)
                            .name("uk_1")
                            .columns(new LinkedList<>(List.of(TABLE_2_COLUMNS_BRIEF_DTO.get(1))))
                            .build())))
                    .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                            .table(TABLE_2_BRIEF_DTO)
                            .column(TABLE_2_COLUMNS_BRIEF_0_DTO)
                            .id(COLUMN_2_1_ID)
                            .build())))
                    .build())
            .owner(USER_2_BRIEF_DTO)
            .avgRowLength(TABLE_2_AVG_ROW_LENGTH)
            .numRows(TABLE_2_NUM_ROWS)
            .dataLength(TABLE_2_DATA_LENGTH)
            .maxDataLength(TABLE_2_MAX_DATA_LENGTH)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_2_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_2_ID)
            .internalName(TABLE_2_INTERNAL_NAME)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .columns(new LinkedList<>()) /* TABLE_2_COLUMNS_CACHE */
            .ownedBy(USER_2_USERNAME)
            .avgRowLength(TABLE_2_AVG_ROW_LENGTH)
            .numRows(TABLE_2_NUM_ROWS)
            .dataLength(TABLE_2_DATA_LENGTH)
            .maxDataLength(TABLE_2_MAX_DATA_LENGTH)
            .build();

    public final static UUID TABLE_3_ID = UUID.fromString("a94ee518-c235-496b-8613-b0c643bc1b11");
    public final static String TABLE_3_NAME = "Sensor";
    public final static String TABLE_3_INTERNAL_NAME = "sensor";
    public final static Boolean TABLE_3_VERSIONED = true;
    public final static Boolean TABLE_3_IS_PUBLIC = false;
    public final static Boolean TABLE_3_SCHEMA_PUBLIC = false;
    public final static String TABLE_3_DESCRIPTION = "Some sensor data";
    public final static String TABLE_3_QUEUE_NAME = TABLE_3_INTERNAL_NAME;
    public final static String TABLE_3_ROUTING_KEY = "dbrepo." + DATABASE_1_ID + "." + TABLE_3_ID;
    public final static Instant TABLE_3_CREATED = Instant.ofEpochSecond(1677400031L) /* 2023-02-26 08:27:11 (UTC) */;
    public final static Instant TABLE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677400031L) /* 2023-02-26 08:27:11 (UTC) */;
    public final static Long TABLE_3_AVG_ROW_LENGTH = 6L;
    public final static Long TABLE_3_NUM_ROWS = 6L;
    public final static Long TABLE_3_DATA_LENGTH = 1800L;
    public final static Long TABLE_3_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final Table TABLE_3 = Table.builder()
            .id(TABLE_3_ID)
            .tdbid(DATABASE_1_ID)
            .database(null) /* DATABASE_1 */
            .created(TABLE_3_CREATED)
            .internalName(TABLE_3_INTERNAL_NAME)
            .isVersioned(TABLE_3_VERSIONED)
            .isPublic(TABLE_3_IS_PUBLIC)
            .isSchemaPublic(TABLE_3_SCHEMA_PUBLIC)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .queueName(TABLE_3_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_3_COLUMNS */
            .constraints(null) /* TABLE_3_CONSTRAINTS */
            .ownedBy(USER_3_USERNAME)
            .avgRowLength(TABLE_3_AVG_ROW_LENGTH)
            .numRows(TABLE_3_NUM_ROWS)
            .dataLength(TABLE_3_DATA_LENGTH)
            .maxDataLength(TABLE_3_MAX_DATA_LENGTH)
            .build();

    public final TableDto TABLE_3_DTO = TableDto.builder()
            .id(TABLE_3_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_3_INTERNAL_NAME)
            .isVersioned(TABLE_3_VERSIONED)
            .isPublic(TABLE_3_IS_PUBLIC)
            .isSchemaPublic(TABLE_3_SCHEMA_PUBLIC)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .queueName(TABLE_3_QUEUE_NAME)
            .routingKey(TABLE_3_ROUTING_KEY)
            .columns(new LinkedList<>() /* TABLE_3_COLUMNS_DTO */)
            .constraints(null) /* TABLE_3_CONSTRAINTS_DTO */
            .owner(USER_3_BRIEF_DTO)
            .avgRowLength(TABLE_3_AVG_ROW_LENGTH)
            .numRows(TABLE_3_NUM_ROWS)
            .dataLength(TABLE_3_DATA_LENGTH)
            .maxDataLength(TABLE_3_MAX_DATA_LENGTH)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_3_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_3_ID)
            .internalName(TABLE_3_INTERNAL_NAME)
            .isPublic(TABLE_3_IS_PUBLIC)
            .isSchemaPublic(TABLE_3_SCHEMA_PUBLIC)
            .columns(new LinkedList<>() /* TABLE_3_COLUMNS_CACHE */)
            .ownedBy(USER_3_USERNAME)
            .avgRowLength(TABLE_3_AVG_ROW_LENGTH)
            .numRows(TABLE_3_NUM_ROWS)
            .dataLength(TABLE_3_DATA_LENGTH)
            .maxDataLength(TABLE_3_MAX_DATA_LENGTH)
            .build();

    public final TableBriefDto TABLE_3_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_3_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_3_INTERNAL_NAME)
            .isVersioned(TABLE_3_VERSIONED)
            .isPublic(TABLE_3_IS_PUBLIC)
            .isSchemaPublic(TABLE_3_SCHEMA_PUBLIC)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .ownedBy(USER_3_USERNAME)
            .build();

    public final CreateTableConstraintsDto TABLE_3_CONSTRAINTS_CREATE_DTO = CreateTableConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .build();

    public final CreateTableConstraintsDto TABLE_3_CONSTRAINTS_INVALID_CREATE_DTO = CreateTableConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>()) // <<<<
            .uniques(new LinkedList<>())
            .foreignKeys(List.of(CreateForeignKeyDto.builder()
                    .referencedTable("weather_location")
                    .columns(new LinkedList<>(List.of("fahrzeug")))
                    .referencedColumns(new LinkedList<>(List.of("doesnotexist")))
                    .build()))
            .build();

    public final CreateTableDto TABLE_3_CREATE_DTO = CreateTableDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(new LinkedList<>())
            .constraints(TABLE_3_CONSTRAINTS_CREATE_DTO)
            .build();

    public final CreateTableDto TABLE_3_INVALID_CREATE_DTO = CreateTableDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(new LinkedList<>())
            .constraints(TABLE_3_CONSTRAINTS_INVALID_CREATE_DTO)
            .build();

    public final static UUID TABLE_5_ID = UUID.fromString("91306cbd-c51f-47d3-8722-debfdbd8a77e");
    public final static String TABLE_5_NAME = "zoo";
    public final static String TABLE_5_INTERNAL_NAME = "zoo";
    public final static Boolean TABLE_5_VERSIONED = true;
    public final static Boolean TABLE_5_IS_PUBLIC = true;
    public final static Boolean TABLE_5_SCHEMA_PUBLIC = true;
    public final static String TABLE_5_DESCRIPTION = "Some Kaggle dataset";
    public final static String TABLE_5_QUEUE_NAME = TABLE_5_INTERNAL_NAME;
    public final static String TABLE_5_ROUTING_KEY = "dbrepo." + DATABASE_2_ID + "." + TABLE_5_ID;
    public final static Instant TABLE_5_CREATED = Instant.ofEpochSecond(1677400067L) /* 2023-02-26 08:27:47 (UTC) */;
    public final static Instant TABLE_5_LAST_MODIFIED = Instant.ofEpochSecond(1677400067L) /* 2023-02-26 08:27:47 (UTC) */;
    public final static Long TABLE_5_AVG_ROW_LENGTH = 1080L;
    public final static Long TABLE_5_NUM_ROWS = 101L;
    public final static Long TABLE_5_DATA_LENGTH = 15200L;
    public final static Long TABLE_5_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final Table TABLE_5 = Table.builder()
            .id(TABLE_5_ID)
            .tdbid(DATABASE_2_ID)
            .created(Instant.now())
            .internalName(TABLE_5_INTERNAL_NAME)
            .isVersioned(TABLE_5_VERSIONED)
            .isPublic(TABLE_5_IS_PUBLIC)
            .isSchemaPublic(TABLE_5_SCHEMA_PUBLIC)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .queueName(TABLE_5_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_5_COLUMNS */
            .constraints(null) /* TABLE_5_CONSTRAINTS */
            .ownedBy(USER_1_USERNAME)
            .build();

    public final TableDto TABLE_5_DTO = TableDto.builder()
            .id(TABLE_5_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_5_INTERNAL_NAME)
            .isVersioned(TABLE_5_VERSIONED)
            .isPublic(TABLE_5_IS_PUBLIC)
            .isSchemaPublic(TABLE_5_SCHEMA_PUBLIC)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .queueName(TABLE_5_QUEUE_NAME)
            .routingKey(TABLE_5_ROUTING_KEY)
            .columns(new LinkedList<>()) /* TABLE_5_COLUMNS_DTO */
            .constraints(null) /* TABLE_5_CONSTRAINTS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_5_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_5_ID)
            .internalName(TABLE_5_INTERNAL_NAME)
            .isPublic(TABLE_5_IS_PUBLIC)
            .isSchemaPublic(TABLE_5_SCHEMA_PUBLIC)
            .columns(new LinkedList<>()) /* TABLE_5_COLUMNS_CACHE */
            .ownedBy(USER_1_USERNAME)
            .build();

    public final TableBriefDto TABLE_5_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_5_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_5_INTERNAL_NAME)
            .isVersioned(TABLE_5_VERSIONED)
            .isPublic(TABLE_5_IS_PUBLIC)
            .isSchemaPublic(TABLE_5_SCHEMA_PUBLIC)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static UUID TABLE_6_ID = UUID.fromString("ae84d169-d36c-4f5a-a390-153d090f9574");
    public final static String TABLE_6_NAME = "names";
    public final static String TABLE_6_INTERNAL_NAME = "names";
    public final static Boolean TABLE_6_VERSIONED = true;
    public final static Boolean TABLE_6_IS_PUBLIC = true;
    public final static Boolean TABLE_6_SCHEMA_PUBLIC = false;
    public final static String TABLE_6_DESCRIPTION = "Some names dataset";
    public final static String TABLE_6_QUEUE_NAME = TABLE_6_INTERNAL_NAME;
    public final static String TABLE_6_ROUTING_KEY = "dbrepo." + DATABASE_2_ID + "." + TABLE_6_ID;
    public final static Instant TABLE_6_CREATED = Instant.ofEpochSecond(1677400117L) /* 2023-02-26 08:28:37 (UTC) */;
    public final static Instant TABLE_6_LAST_MODIFIED = Instant.ofEpochSecond(1677400117L) /* 2023-02-26 08:28:37 (UTC) */;

    public final Table TABLE_6 = Table.builder()
            .id(TABLE_6_ID)
            .tdbid(DATABASE_2_ID)
            .created(TABLE_6_CREATED)
            .internalName(TABLE_6_INTERNAL_NAME)
            .isVersioned(TABLE_6_VERSIONED)
            .isPublic(TABLE_6_IS_PUBLIC)
            .isSchemaPublic(TABLE_6_SCHEMA_PUBLIC)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .queueName(TABLE_6_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_6_COLUMNS */
            .constraints(null) /* TABLE_6_CONSTRAINTS */
            .ownedBy(USER_1_USERNAME)
            .created(TABLE_6_CREATED)
            .build();

    public final TableDto TABLE_6_DTO = TableDto.builder()
            .id(TABLE_6_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_6_INTERNAL_NAME)
            .isVersioned(TABLE_6_VERSIONED)
            .isPublic(TABLE_6_IS_PUBLIC)
            .isSchemaPublic(TABLE_6_SCHEMA_PUBLIC)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .queueName(TABLE_6_QUEUE_NAME)
            .routingKey(TABLE_6_ROUTING_KEY)
            .columns(new LinkedList<>()) /* TABLE_6_COLUMNS_DTO */
            .constraints(null) /* TABLE_6_CONSTRAINTS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_6_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_6_ID)
            .internalName(TABLE_6_INTERNAL_NAME)
            .isPublic(TABLE_6_IS_PUBLIC)
            .isSchemaPublic(TABLE_6_SCHEMA_PUBLIC)
            .columns(new LinkedList<>()) /* TABLE_6_COLUMNS_CACHE */
            .ownedBy(USER_1_USERNAME)
            .build();

    public final TableBriefDto TABLE_6_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_6_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_6_INTERNAL_NAME)
            .isVersioned(TABLE_6_VERSIONED)
            .isPublic(TABLE_6_IS_PUBLIC)
            .isSchemaPublic(TABLE_6_SCHEMA_PUBLIC)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .ownedBy(USER_1_USERNAME)
            .build();

    @SuppressWarnings("java:S3599")
    public final TableStatisticDto TABLE_6_STATISTIC_DTO = TableStatisticDto.builder()
            .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                            .name("id")
                            .min(BigDecimal.valueOf(1.0))
                            .max(BigDecimal.valueOf(5.0))
                            .mean(BigDecimal.valueOf(3.0))
                            .median(BigDecimal.valueOf(3.0))
                            .stdDev(BigDecimal.valueOf(1.0))
                            .build(),
                    ColumnStatisticDto.builder()
                            .name("firstname")
                            .min(BigDecimal.valueOf(3.0))
                            .max(BigDecimal.valueOf(8.0))
                            .mean(BigDecimal.valueOf(5.8))
                            .median(BigDecimal.valueOf(6))
                            .stdDev(BigDecimal.valueOf(1.0))
                            .build(),
                    ColumnStatisticDto.builder()
                            .name("lastname")
                            .min(BigDecimal.valueOf(11.0))
                            .max(BigDecimal.valueOf(10.0))
                            .mean(BigDecimal.valueOf(7.8))
                            .median(BigDecimal.valueOf(8.0))
                            .stdDev(BigDecimal.valueOf(1.0))
                            .build(),
                    ColumnStatisticDto.builder()
                            .name("birth")
                            .min(BigDecimal.valueOf(1990.0))
                            .max(BigDecimal.valueOf(1991.0))
                            .mean(BigDecimal.valueOf(1990.5))
                            .median(BigDecimal.valueOf(1990.0))
                            .stdDev(BigDecimal.valueOf(1.0))
                            .build(),
                    ColumnStatisticDto.builder()
                            .name("reminder")
                            .min(BigDecimal.valueOf(11.2))
                            .max(BigDecimal.valueOf(23.1))
                            .mean(BigDecimal.valueOf(13.5333))
                            .median(BigDecimal.valueOf(11.4))
                            .stdDev(BigDecimal.valueOf(4.2952))
                            .build(),
                    ColumnStatisticDto.builder()
                            .name("ref_id")
                            .min(BigDecimal.valueOf(0.0))
                            .max(BigDecimal.valueOf(0.0))
                            .mean(BigDecimal.valueOf(0.0))
                            .median(BigDecimal.valueOf(0.0))
                            .stdDev(BigDecimal.valueOf(1.0))
                            .build())))
            .build();

    public final static UUID TABLE_7_ID = UUID.fromString("e5d10200-3e4f-45f4-9f36-ff3ca39c6c29");
    public final static String TABLE_7_NAME = "likes";
    public final static String TABLE_7_INTERNAL_NAME = "likes";
    public final static Boolean TABLE_7_VERSIONED = true;
    public final static Boolean TABLE_7_IS_PUBLIC = true;
    public final static Boolean TABLE_7_SCHEMA_PUBLIC = true;
    public final static String TABLE_7_DESCRIPTION = "Some likes dataset";
    public final static String TABLE_7_QUEUE_NAME = TABLE_7_INTERNAL_NAME;
    public final static String TABLE_7_ROUTING_KEY = "dbrepo." + DATABASE_2_ID + "." + TABLE_7_ID;
    public final static Instant TABLE_7_CREATED = Instant.ofEpochSecond(1677400147L) /* 2023-02-26 08:29:07 (UTC) */;
    public final static Instant TABLE_7_LAST_MODIFIED = Instant.ofEpochSecond(1677400147L) /* 2023-02-26 08:29:07 (UTC) */;

    public final Table TABLE_7 = Table.builder()
            .id(TABLE_7_ID)
            .tdbid(DATABASE_2_ID)
            .created(TABLE_7_CREATED)
            .internalName(TABLE_7_INTERNAL_NAME)
            .isVersioned(TABLE_7_VERSIONED)
            .isPublic(TABLE_7_IS_PUBLIC)
            .isSchemaPublic(TABLE_7_SCHEMA_PUBLIC)
            .description(TABLE_7_DESCRIPTION)
            .name(TABLE_7_NAME)
            .lastModified(TABLE_7_LAST_MODIFIED)
            .queueName(TABLE_7_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_7_COLUMNS */
            .constraints(null) /* TABLE_7_CONSTRAINTS */
            .ownedBy(USER_1_USERNAME)
            .created(TABLE_7_CREATED)
            .build();

    public final TableDto TABLE_7_DTO = TableDto.builder()
            .id(TABLE_7_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_7_INTERNAL_NAME)
            .isVersioned(TABLE_7_VERSIONED)
            .isPublic(TABLE_7_IS_PUBLIC)
            .isSchemaPublic(TABLE_7_SCHEMA_PUBLIC)
            .description(TABLE_7_DESCRIPTION)
            .name(TABLE_7_NAME)
            .queueName(TABLE_7_QUEUE_NAME)
            .routingKey(TABLE_7_ROUTING_KEY)
            .columns(new LinkedList<>()) /* TABLE_7_COLUMNS_DTO */
            .constraints(null) /* TABLE_7_CONSTRAINTS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_7_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_7_ID)
            .internalName(TABLE_7_INTERNAL_NAME)
            .isPublic(TABLE_7_IS_PUBLIC)
            .isSchemaPublic(TABLE_7_SCHEMA_PUBLIC)
            .columns(new LinkedList<>()) /* TABLE_7_COLUMNS_CACHE */
            .ownedBy(USER_1_USERNAME)
            .build();

    public final TableBriefDto TABLE_7_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_7_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_7_INTERNAL_NAME)
            .isVersioned(TABLE_7_VERSIONED)
            .isPublic(TABLE_7_IS_PUBLIC)
            .isSchemaPublic(TABLE_7_SCHEMA_PUBLIC)
            .description(TABLE_7_DESCRIPTION)
            .name(TABLE_7_NAME)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static UUID TABLE_4_ID = UUID.fromString("6c87cbcf-5043-404f-9bf1-b09ddbac25a2");
    public final static String TABLE_4_NAME = "Sensor 2";
    public final static String TABLE_4_INTERNAL_NAME = "sensor_2";
    public final static Boolean TABLE_4_VERSIONED = true;
    public final static Boolean TABLE_4_IS_PUBLIC = true;
    public final static Boolean TABLE_4_SCHEMA_PUBLIC = false;
    public final static String TABLE_4_DESCRIPTION = "Hello sensor";
    public final static String TABLE_4_QUEUE_NAME = TABLE_4_INTERNAL_NAME;
    public final static String TABLE_4_ROUTING_KEY = "dbrepo." + DATABASE_1_ID + "." + TABLE_4_ID;
    public final static Instant TABLE_4_CREATED = Instant.ofEpochSecond(1677400175L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677400175L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Long TABLE_4_AVG_ROW_LENGTH = 0L;
    public final static Long TABLE_4_NUM_ROWS = 0L;
    public final static Long TABLE_4_DATA_LENGTH = 1000L;
    public final static Long TABLE_4_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final Table TABLE_4 = Table.builder()
            .id(TABLE_4_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNAL_NAME)
            .description(TABLE_4_DESCRIPTION)
            .database(null /* DATABASE_1 */)
            .name(TABLE_4_NAME)
            .queueName(TABLE_4_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_4_COLUMNS */
            .constraints(null) /* TABLE_4_CONSTRAINTS */
            .isVersioned(TABLE_4_VERSIONED)
            .isPublic(TABLE_4_IS_PUBLIC)
            .isSchemaPublic(TABLE_4_SCHEMA_PUBLIC)
            .ownedBy(USER_1_USERNAME)
            .created(TABLE_4_CREATED)
            .lastModified(TABLE_4_LAST_MODIFIED)
            .avgRowLength(TABLE_4_AVG_ROW_LENGTH)
            .numRows(TABLE_4_NUM_ROWS)
            .dataLength(TABLE_4_DATA_LENGTH)
            .maxDataLength(TABLE_4_MAX_DATA_LENGTH)
            .build();

    public final TableDto TABLE_4_DTO = TableDto.builder()
            .id(TABLE_4_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNAL_NAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .queueName(TABLE_4_QUEUE_NAME)
            .routingKey(TABLE_4_ROUTING_KEY)
            .columns(new LinkedList<>()) /* TABLE_4_COLUMNS_DTO */
            .constraints(null) /* TABLE_4_CONSTRAINTS_DTO */
            .isVersioned(TABLE_4_VERSIONED)
            .isPublic(TABLE_4_IS_PUBLIC)
            .isSchemaPublic(TABLE_4_SCHEMA_PUBLIC)
            .owner(USER_1_BRIEF_DTO)
            .avgRowLength(TABLE_4_AVG_ROW_LENGTH)
            .numRows(TABLE_4_NUM_ROWS)
            .dataLength(TABLE_4_DATA_LENGTH)
            .maxDataLength(TABLE_4_MAX_DATA_LENGTH)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_4_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_4_ID)
            .internalName(TABLE_4_INTERNAL_NAME)
            .columns(new LinkedList<>()) /* TABLE_4_COLUMNS_CACHE */
            .isPublic(TABLE_4_IS_PUBLIC)
            .isSchemaPublic(TABLE_4_SCHEMA_PUBLIC)
            .ownedBy(USER_1_USERNAME)
            .avgRowLength(TABLE_4_AVG_ROW_LENGTH)
            .numRows(TABLE_4_NUM_ROWS)
            .dataLength(TABLE_4_DATA_LENGTH)
            .maxDataLength(TABLE_4_MAX_DATA_LENGTH)
            .build();

    public final TableBriefDto TABLE_4_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_4_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNAL_NAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .isVersioned(TABLE_4_VERSIONED)
            .isPublic(TABLE_4_IS_PUBLIC)
            .isSchemaPublic(TABLE_4_SCHEMA_PUBLIC)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final ColumnBriefDto TABLE_4_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(UUID.fromString("360f02be-6dfb-48ea-9d1e-1da488b0e324"))
            .name("Timestamp")
            .internalName("timestamp")
            .columnType(ColumnTypeDto.TIMESTAMP)
            .build();

    public final static UUID COLUMN_4_1_ID = UUID.fromString("c8ec8a56-dea1-4316-895f-56e6d289cbf7");
    public final static UUID COLUMN_4_2_ID = UUID.fromString("d06956ae-aabd-474f-a47d-47af1ba043d1");

    public final List<TableColumn> TABLE_4_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_4_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_4)
                    .name("Timestamp")
                    .internalName("timestamp")
                    .columnType(TableColumnType.TIMESTAMP)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_4)
                    .name("Value")
                    .internalName("value")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<CreateTableColumnDto> TABLE_4_COLUMNS_CREATE_DTO = List.of(CreateTableColumnDto.builder()
                    .name("Timestamp")
                    .type(ColumnTypeDto.TIMESTAMP)
                    .nullAllowed(false)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Value")
                    .type(ColumnTypeDto.DECIMAL)
                    .nullAllowed(true)
                    .size(10L)
                    .d(10L)
                    .build());

    public final CreateTableConstraintsDto TABLE_4_CONSTRAINTS_CREATE_DTO = CreateTableConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>(Set.of("Timestamp")))
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>(List.of(List.of("Timestamp"))))
            .build();

    public final CreateTableDto TABLE_4_CREATE_DTO = CreateTableDto.builder()
            .name(TABLE_4_NAME)
            .description(TABLE_4_DESCRIPTION)
            .columns(TABLE_4_COLUMNS_CREATE_DTO)
            .constraints(TABLE_4_CONSTRAINTS_CREATE_DTO)
            .build();

    public final List<ColumnDto> TABLE_4_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_4_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .tableId(TABLE_4_ID)
                    .name("Timestamp")
                    .internalName("timestamp")
                    .columnType(ColumnTypeDto.TIMESTAMP)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .tableId(TABLE_4_ID)
                    .name("Value")
                    .internalName("value")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .build());

    public final List<Column> TABLE_4_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_4_1_ID)
                    .internalName("timestamp")
                    .columnType(ColumnType.TIMESTAMP)
                    .build(),
            Column.builder()
                    .id(COLUMN_4_2_ID)
                    .internalName("value")
                    .columnType(ColumnType.DECIMAL)
                    .build());

    public final static UUID TABLE_8_ID = UUID.fromString("2e039d0d-3257-4083-8b32-76d7cfa1f7fd");
    public final static String TABLE_8_NAME = "location";
    public final static String TABLE_8_INTERNAL_NAME = "mfcc";
    public final static Boolean TABLE_8_VERSIONED = true;
    public final static Boolean TABLE_8_IS_PUBLIC = false;
    public final static Boolean TABLE_8_SCHEMA_PUBLIC = false;
    public final static String TABLE_8_DESCRIPTION = "Hello mfcc";
    public final static String TABLE_8_QUEUE_NAME = TABLE_8_INTERNAL_NAME;
    public final static String TABLE_8_ROUTING_KEY = "dbrepo." + DATABASE_3_ID + "." + TABLE_8_ID;
    public final static Instant TABLE_8_CREATED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_8_LAST_MODIFIED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;

    public final Table TABLE_8 = Table.builder()
            .id(TABLE_8_ID)
            .tdbid(DATABASE_3_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .isVersioned(TABLE_8_VERSIONED)
            .isPublic(TABLE_8_IS_PUBLIC)
            .isSchemaPublic(TABLE_8_SCHEMA_PUBLIC)
            .database(null /* DATABASE_3 */)
            .name(TABLE_8_NAME)
            .queueName(TABLE_8_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_8_COLUMNS */
            .constraints(null) /* TABLE_8_CONSTRAINTS */
            .ownedBy(USER_1_USERNAME)
            .created(TABLE_8_CREATED)
            .lastModified(TABLE_8_LAST_MODIFIED)
            .build();

    public final TableDto TABLE_8_DTO = TableDto.builder()
            .id(TABLE_8_ID)
            .databaseId(DATABASE_3_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .isVersioned(TABLE_8_VERSIONED)
            .isPublic(TABLE_8_IS_PUBLIC)
            .isSchemaPublic(TABLE_8_SCHEMA_PUBLIC)
            .name(TABLE_8_NAME)
            .queueName(TABLE_8_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_8_COLUMNS_DTO */
            .constraints(null) /* TABLE_8_CONSTRAINTS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table TABLE_8_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table.builder()
            .id(TABLE_8_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .isPublic(TABLE_8_IS_PUBLIC)
            .isSchemaPublic(TABLE_8_SCHEMA_PUBLIC)
            .columns(new LinkedList<>()) /* TABLE_8_COLUMNS_CACHE */
            .ownedBy(USER_1_USERNAME)
            .build();

    public final TableUpdateDto TABLE_8_UPDATE_DTO = TableUpdateDto.builder()
            .description(null)
            .isPublic(true)
            .isSchemaPublic(true)
            .build();

    public final TableBriefDto TABLE_8_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_8_ID)
            .databaseId(DATABASE_3_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .isVersioned(TABLE_8_VERSIONED)
            .isPublic(TABLE_8_IS_PUBLIC)
            .isSchemaPublic(TABLE_8_SCHEMA_PUBLIC)
            .name(TABLE_8_NAME)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static UUID TABLE_9_ID = UUID.fromString("9314294f-04fc-4354-8b1f-2a8aeb566453");
    public final static String TABLE_9_NAME = "Weather Location";
    public final static String TABLE_9_INTERNAL_NAME = "weather_location";
    public final static Boolean TABLE_9_VERSIONED = true;
    public final static Boolean TABLE_9_IS_PUBLIC = false;
    public final static Boolean TABLE_9_SCHEMA_PUBLIC = true;
    public final static String TABLE_9_DESCRIPTION = "Location";
    public final static String TABLE_9_QUEUE_NAME = TABLE_9_INTERNAL_NAME;
    public final static String TABLE_9_ROUTING_KEY = "dbrepo." + DATABASE_4_ID + "." + TABLE_9_ID;
    public final static Instant TABLE_9_CREATED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_9_LAST_MODIFIED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;

    public final Table TABLE_9 = Table.builder()
            .id(TABLE_9_ID)
            .tdbid(DATABASE_4_ID)
            .internalName(TABLE_9_INTERNAL_NAME)
            .description(TABLE_9_DESCRIPTION)
            .isVersioned(TABLE_9_VERSIONED)
            .isPublic(TABLE_9_IS_PUBLIC)
            .isSchemaPublic(TABLE_9_SCHEMA_PUBLIC)
            .database(null /* DATABASE_1 */)
            .name(TABLE_9_NAME)
            .queueName(TABLE_9_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_9_COLUMNS */
            .constraints(null) /* TABLE_9_CONSTRAINTS */
            .ownedBy(USER_1_USERNAME)
            .created(TABLE_9_CREATED)
            .lastModified(TABLE_9_LAST_MODIFIED)
            .build();

    public final TableDto TABLE_9_DTO = TableDto.builder()
            .id(TABLE_9_ID)
            .databaseId(DATABASE_4_ID)
            .internalName(TABLE_9_INTERNAL_NAME)
            .description(TABLE_9_DESCRIPTION)
            .isVersioned(TABLE_9_VERSIONED)
            .isPublic(TABLE_9_IS_PUBLIC)
            .isSchemaPublic(TABLE_9_SCHEMA_PUBLIC)
            .name(TABLE_9_NAME)
            .queueName(TABLE_9_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_9_COLUMNS_DTO */
            .constraints(null) /* TABLE_9_CONSTRAINTS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final TableBriefDto TABLE_9_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_9_ID)
            .databaseId(DATABASE_4_ID)
            .internalName(TABLE_9_INTERNAL_NAME)
            .description(TABLE_9_DESCRIPTION)
            .isVersioned(TABLE_9_VERSIONED)
            .isPublic(TABLE_9_IS_PUBLIC)
            .isSchemaPublic(TABLE_9_SCHEMA_PUBLIC)
            .name(TABLE_9_NAME)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static UUID COLUMN_9_1_ID = UUID.fromString("e03c7578-2d1a-4599-9b11-7174f40efc0a");
    public final static String COLUMN_9_1_NAME = "location";
    public final static String COLUMN_9_1_INTERNAL_NAME = "location";

    public final ColumnBriefDto TABLE_9_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_9_1_ID)
            .name(COLUMN_9_1_NAME)
            .internalName(COLUMN_9_1_INTERNAL_NAME)
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static UUID COLUMN_9_2_ID = UUID.fromString("03c07223-17e1-4af5-b1ae-ef9ab434fe2d");
    public final static UUID COLUMN_9_3_ID = UUID.fromString("ee6590db-923b-4234-beb8-3120da055cf6");

    public final List<TableColumn> TABLE_9_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_9_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_9)
                    .name(COLUMN_9_1_NAME)
                    .internalName(COLUMN_9_1_INTERNAL_NAME)
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_9_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_9)
                    .name("lat")
                    .internalName("lat")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_9_3_ID)
                    .ordinalPosition(2)
                    .table(TABLE_9)
                    .name("lng")
                    .internalName("lng")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<ColumnDto> TABLE_9_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_9_1_ID)
                    .ordinalPosition(0)
                    .name(COLUMN_9_1_NAME)
                    .internalName(COLUMN_9_1_INTERNAL_NAME)
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_9_2_ID)
                    .ordinalPosition(1)
                    .name("lat")
                    .internalName("lat")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_9_3_ID)
                    .ordinalPosition(2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final Constraints TABLE_9_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_9)
                    .column(TABLE_9_COLUMNS.get(0))
                    .id(COLUMN_9_1_ID)
                    .build())))
            .build();

    public final ConstraintsDto TABLE_9_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_9_BRIEF_DTO)
                    .column(TABLE_9_COLUMNS_BRIEF_0_DTO)
                    .id(COLUMN_9_1_ID)
                    .build())))
            .build();

    public final static UUID QUERY_9_ID = UUID.fromString("df34f0b9-b64c-406c-9109-7a031f4a7f27");
    public final static String QUERY_9_STATEMENT = "SELECT `lat`, `lng` FROM `mfcc` WHERE `location` = 'Fuji'";
    public final static String QUERY_9_QUERY_HASH = "dfcdec827b2ea74d89415f8d1ce39354f59ef304444ba4e12e4f3d9d3f35abe3";
    public final static String QUERY_9_RESULT_HASH = "f0aba070a1fd29e96230d12d7c0b4d08b89820b3cc2dda0575680492010016e7";
    public final static Instant QUERY_9_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static String QUERY_9_STATEMENT_NORMALIZED = "SELECT `lat`, `lng` FROM `mfcc` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_9_EXECUTION)) + "' WHERE `location` = 'Fuji'";
    public final static Long QUERY_9_RESULT_NUMBER = 6L;
    public final static Boolean QUERY_9_PERSISTED = true;

    public final QueryDto QUERY_9_DTO = QueryDto.builder()
            .id(QUERY_9_ID)
            .databaseId(DATABASE_3_ID)
            .query(QUERY_9_STATEMENT)
            .queryNormalized(QUERY_9_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_9_RESULT_NUMBER)
            .resultHash(QUERY_9_RESULT_HASH)
            .queryHash(QUERY_9_QUERY_HASH)
            .execution(QUERY_9_EXECUTION)
            .isPersisted(QUERY_9_PERSISTED)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final Subset QUERY_9_CACHE = Subset.builder()
            .id(QUERY_9_ID)
            .databaseId(DATABASE_3_ID)
            .query(QUERY_9_STATEMENT)
            .queryNormalized(QUERY_9_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_9_RESULT_NUMBER)
            .resultHash(QUERY_9_RESULT_HASH)
            .queryHash(QUERY_9_QUERY_HASH)
            .execution(QUERY_9_EXECUTION)
            .isPersisted(QUERY_9_PERSISTED)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final SubsetDto QUERY_9_SUBSET_DTO = SubsetDto.builder()
            .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_5_ID)))
            .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_9_2_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_9_3_ID).build())))
            .filters(new LinkedHashSet<>(Set.of(FilterDto.builder()
                    .columnId(COLUMN_9_1_ID)
                    .operatorId(IMAGE_1_OPERATORS_2_ID)
                    .value("Fuji")
                    .type(FilterTypeDto.WHERE)
                    .build())))
            .build();

    public final ViewDto QUERY_9_VIEW_DTO = ViewDto.builder()
            .query(QUERY_9_STATEMENT)
            .queryHash(QUERY_9_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .columns(new LinkedList<>(List.of(ViewColumnDto.builder()
                            .id(UUID.fromString("ff179632-0249-4e4c-9167-23c9d89e8b05"))
                            .name("lat")
                            .internalName("lat")
                            .build(),
                    ViewColumnDto.builder()
                            .id(UUID.fromString("40d1b8e5-0a67-4edd-851a-b1a44393e6a4"))
                            .name("lng")
                            .internalName("lng")
                            .build())))
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.View QUERY_9_VIEW_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.View.builder()
            .query(QUERY_9_STATEMENT)
            .queryHash(QUERY_9_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .columns(new LinkedList<>(List.of(at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                            .id(UUID.fromString("ff179632-0249-4e4c-9167-23c9d89e8b05"))
                            .internalName("lat")
                            .build(),
                    at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                            .id(UUID.fromString("40d1b8e5-0a67-4edd-851a-b1a44393e6a4"))
                            .internalName("lng")
                            .build())))
            .build();

    public final Map<String, ColumnAnalysisResultDto> QUERY_9_ANALYSIS_MAP_DTO = new HashMap<>() {{
        put("lat", ColumnAnalysisResultDto.builder()
                .name("lat")
                .datatype(ColumnTypeDto.DECIMAL)
                .build());
        put("lng", ColumnAnalysisResultDto.builder()
                .name("lng")
                .datatype(ColumnTypeDto.DECIMAL)
                .build());
    }};

    public final static String QUEUE_NAME = "dbrepo";
    public final static String QUEUE_VHOST = "dbrepo";
    public final static Boolean QUEUE_AUTO_DELETE = false;
    public final static Boolean QUEUE_DURABLE = true;
    public final static Boolean QUEUE_EXCLUSIVE = false;
    public final static String QUEUE_TYPE = "quorum";

    public final QueueDto QUEUE_DTO = QueueDto.builder()
            .name(QUEUE_NAME)
            .vhost(QUEUE_VHOST)
            .autoDelete(QUEUE_AUTO_DELETE)
            .durable(QUEUE_DURABLE)
            .exclusive(QUEUE_EXCLUSIVE)
            .type(QUEUE_TYPE)
            .build();

    public final static UUID ONTOLOGY_1_ID = UUID.fromString("dc195d01-0a45-4583-aa83-fd270b874353");
    public final static String ONTOLOGY_1_PREFIX = "om2";
    public final static String ONTOLOGY_1_NEW_PREFIX = "om-2";
    public final static String ONTOLOGY_1_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/";
    public final static String ONTOLOGY_1_URI_PATTERN = "http://www.ontology-of-units-of-measure.org/resource/om-2/.*";
    public final static String ONTOLOGY_1_SPARQL_ENDPOINT = null;
    public final static Boolean ONTOLOGY_1_SPARQL = false;
    public final static String ONTOLOGY_1_RDF_PATH = "rdf/om-2.0.rdf";
    public final static Boolean ONTOLOGY_1_RDF = true;

    public final static UUID ONTOLOGY_2_ID = UUID.fromString("41d902a1-f9f8-4d51-ad64-618b72acf5ed");
    public final static String ONTOLOGY_2_PREFIX = "wd";
    public final static String ONTOLOGY_2_URI = "http://www.wikidata.org/";
    public final static String ONTOLOGY_2_SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";

    public final static UUID ONTOLOGY_3_ID = UUID.fromString("5b41390b-d2d2-45c6-8038-1258c4b2725f");
    public final static String ONTOLOGY_3_PREFIX = "rdfs";
    public final static String ONTOLOGY_3_URI = "http://www.w3.org/2000/01/rdf-schema#";
    public final static String ONTOLOGY_3_SPARQL_ENDPOINT = null;

    public final static UUID ONTOLOGY_4_ID = UUID.fromString("d6992475-9b71-4a4a-a6eb-bc1fe6a34443");
    public final static String ONTOLOGY_4_PREFIX = "schema";
    public final static String ONTOLOGY_4_URI = "http://schema.org/";
    public final static String ONTOLOGY_4_SPARQL_ENDPOINT = null;

    public final static UUID ONTOLOGY_5_ID = UUID.fromString("f95d1330-762e-4f5a-875a-3c64da5808a1");
    public final static String ONTOLOGY_5_PREFIX = "db";
    public final static String ONTOLOGY_5_URI = "http://dbpedia.org";
    public final static String ONTOLOGY_5_SPARQL_ENDPOINT = "http://dbpedia.org/sparql";

    public final static UUID COLUMN_8_1_ID = UUID.fromString("af362ac6-5dbb-4ede-83ea-5d94b39641c8");
    public final static Integer COLUMN_8_1_ORDINALPOS = 0;
    public final static String COLUMN_8_1_NAME = "ID";
    public final static String COLUMN_8_1_INTERNAL_NAME = "id";
    public final static Boolean COLUMN_8_1_NULL = false;

    public final static UUID COLUMN_8_2_ID = UUID.fromString("7ada597b-0766-4612-9ace-67eeee94e2da");
    public final static Integer COLUMN_8_2_ORDINALPOS = 1;
    public final static String COLUMN_8_2_NAME = "Value";
    public final static String COLUMN_8_2_INTERNAL_NAME = "value";
    public final static Long COLUMN_8_2_SIZE = 10L;
    public final static Long COLUMN_8_2_D = 10L;
    public final static Boolean COLUMN_8_2_NULL = true;

    public final static UUID COLUMN_8_3_ID = UUID.fromString("8bcd9ef8-f7b8-4730-acc1-a3d43ba69a56");
    public final static Integer COLUMN_8_3_ORDINALPOS = 2;
    public final static String COLUMN_8_3_NAME = "raw";
    public final static String COLUMN_8_3_INTERNAL_NAME = "raw";
    public final static Boolean COLUMN_8_3_NULL = true;

    public final ColumnBriefDto TABLE_8_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_8_1_ID)
            .name(COLUMN_8_1_NAME)
            .internalName(COLUMN_8_1_INTERNAL_NAME)
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final List<TableColumn> TABLE_8_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_8_1_ID)
                    .ordinalPosition(COLUMN_8_1_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_1_NAME)
                    .internalName(COLUMN_8_1_INTERNAL_NAME)
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(COLUMN_8_1_NULL)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_8_2_ID)
                    .ordinalPosition(COLUMN_8_2_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_2_NAME)
                    .internalName(COLUMN_8_2_INTERNAL_NAME)
                    .columnType(TableColumnType.DECIMAL)
                    .size(COLUMN_8_2_SIZE)
                    .d(COLUMN_8_2_D)
                    .isNullAllowed(COLUMN_8_2_NULL)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_8_3_ID)
                    .ordinalPosition(COLUMN_8_3_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_3_NAME)
                    .internalName(COLUMN_8_3_INTERNAL_NAME)
                    .columnType(TableColumnType.LONGBLOB)
                    .isNullAllowed(COLUMN_8_3_NULL)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<ColumnDto> TABLE_8_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_8_1_ID)
                    .ordinalPosition(COLUMN_8_1_ORDINALPOS)
                    .name(COLUMN_8_1_NAME)
                    .internalName(COLUMN_8_1_INTERNAL_NAME)
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(COLUMN_8_1_NULL)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_8_2_ID)
                    .ordinalPosition(COLUMN_8_2_ORDINALPOS)
                    .name(COLUMN_8_2_NAME)
                    .internalName(COLUMN_8_2_INTERNAL_NAME)
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(COLUMN_8_2_NULL)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_8_3_ID)
                    .ordinalPosition(COLUMN_8_3_ORDINALPOS)
                    .name(COLUMN_8_3_NAME)
                    .internalName(COLUMN_8_3_INTERNAL_NAME)
                    .columnType(ColumnTypeDto.LONGBLOB)
                    .isNullAllowed(COLUMN_8_3_NULL)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<Column> TABLE_8_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_8_1_ID)
                    .internalName(COLUMN_8_1_INTERNAL_NAME)
                    .columnType(ColumnType.BIGINT)
                    .build(),
            Column.builder()
                    .id(COLUMN_8_2_ID)
                    .internalName(COLUMN_8_2_INTERNAL_NAME)
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            Column.builder()
                    .id(COLUMN_8_3_ID)
                    .internalName(COLUMN_8_3_INTERNAL_NAME)
                    .columnType(ColumnType.LONGBLOB)
                    .build());

    public final static Long TABLE_8_DATA_COUNT = 6L;
    @SuppressWarnings("java:S3599")
    public final List<Map<String, Object>> TABLE_8_DATA_DTO = new LinkedList<>(List.of(
            new HashMap<>() {{
                put(COLUMN_8_1_INTERNAL_NAME, BigInteger.valueOf(1L));
                put(COLUMN_8_2_INTERNAL_NAME, 11.2);
                put(COLUMN_8_3_INTERNAL_NAME, null);
            }},
            new HashMap<>() {{
                put(COLUMN_8_1_INTERNAL_NAME, BigInteger.valueOf(2L));
                put(COLUMN_8_2_INTERNAL_NAME, 11.3);
                put(COLUMN_8_3_INTERNAL_NAME, null);
            }},
            new HashMap<>() {{
                put(COLUMN_8_1_INTERNAL_NAME, BigInteger.valueOf(3L));
                put(COLUMN_8_2_INTERNAL_NAME, 11.4);
                put(COLUMN_8_3_INTERNAL_NAME, null);
            }},
            new HashMap<>() {{
                put(COLUMN_8_1_INTERNAL_NAME, BigInteger.valueOf(4L));
                put(COLUMN_8_2_INTERNAL_NAME, 11.9);
                put(COLUMN_8_3_INTERNAL_NAME, null);
            }},
            new HashMap<>() {{
                put(COLUMN_8_1_INTERNAL_NAME, BigInteger.valueOf(5L));
                put(COLUMN_8_2_INTERNAL_NAME, 12.3);
                put(COLUMN_8_3_INTERNAL_NAME, null);
            }},
            new HashMap<>() {{
                put(COLUMN_8_1_INTERNAL_NAME, BigInteger.valueOf(6L));
                put(COLUMN_8_2_INTERNAL_NAME, 23.1);
                put(COLUMN_8_3_INTERNAL_NAME, null);
            }}
    ));

    @SuppressWarnings("java:S3599")
    public final TableStatisticDto TABLE_8_STATISTIC_DTO = TableStatisticDto.builder()
            .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                    .name(COLUMN_8_1_INTERNAL_NAME)
                    .min(BigDecimal.valueOf(11.2))
                    .max(BigDecimal.valueOf(23.1))
                    .mean(BigDecimal.valueOf(13.5333))
                    .median(BigDecimal.valueOf(11.4))
                    .stdDev(BigDecimal.valueOf(4.2952))
                    .build())))
            .build();

    public final static UUID QUERY_1_ID = UUID.fromString("60494137-f000-459e-acd3-4fcadbdf14ca");
    public final static String QUERY_1_STATEMENT = "SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` ORDER BY id ASC";
    public final static Long QUERY_1_RESULT_NUMBER = 3L;
    public final static String QUERY_1_QUERY_HASH = "a3b8ac39e38167d14cf3a9c20a69e4b6954d049525390b973a2c23064953a992";
    public final static String QUERY_1_RESULT_HASH = "c640a88c5646fdad11f94675b4735b09004a034a316c4780a93a53b10f22809c";
    public final static Instant QUERY_1_EXECUTION = Instant.ofEpochSecond(1677648377L) /* 2025-03-07 11:58:07 */;
    public final static String QUERY_1_STATEMENT_NORMALIZED = "SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_1_EXECUTION)) + "' ORDER BY id ASC";
    public final static Boolean QUERY_1_PERSISTED = true;

    public final SubsetDto QUERY_1_SUBSET_DTO = SubsetDto.builder()
            .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_1_ID)))
            .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_1_2_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_1_3_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_1_4_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_1_5_ID).build())))
            .orders(new LinkedHashSet<>(Set.of(OrderDto.builder()
                    .columnId(COLUMN_1_1_ID)
                    .direction(OrderTypeDto.ASC)
                    .build())))
            .build();

    public final ViewDto QUERY_1_VIEW_DTO = ViewDto.builder()
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .columns(new LinkedList<>(List.of(ViewColumnDto.builder()
                            .name("id")
                            .internalName("id")
                            .build(),
                    ViewColumnDto.builder()
                            .name("date")
                            .internalName("date")
                            .build(),
                    ViewColumnDto.builder()
                            .name("location")
                            .internalName("location")
                            .build(),
                    ViewColumnDto.builder()
                            .name("mintemp")
                            .internalName("mintemp")
                            .build(),
                    ViewColumnDto.builder()
                            .name("rainfall")
                            .internalName("rainfall")
                            .build())))
            .build();

    public final QueryBriefDto QUERY_1_BRIEF_DTO = QueryBriefDto.builder()
            .id(QUERY_1_ID)
            .databaseId(DATABASE_1_ID)
            .query(QUERY_1_STATEMENT)
            .queryNormalized(QUERY_1_STATEMENT_NORMALIZED)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .execution(QUERY_1_EXECUTION)
            .owner(USER_1_BRIEF_DTO)
            .isPersisted(QUERY_1_PERSISTED)
            .resultNumber(3L)
            .build();

    public final static UUID QUERY_2_ID = UUID.fromString("4e0ac92a-7cb3-4222-9b85-0498c73e0afd");
    public final static String QUERY_2_STATEMENT = "SELECT `location` FROM `weather_aus`";
    public final static String QUERY_2_QUERY_HASH = "a2d2dd94ebc7653bb5a3b55dd8ed5e91d3d13c225c6855a1eb4eb7ca14c36ced";
    public final static Long QUERY_2_RESULT_NUMBER = 3L;
    public final static String QUERY_2_RESULT_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static Instant QUERY_2_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2018-11-07 10:59:12 */;
    public final static String QUERY_2_STATEMENT_NORMALIZED = "SELECT `location` FROM `weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_2_EXECUTION)) + "'";
    public final static Boolean QUERY_2_PERSISTED = false;

    public final static UUID QUERY_3_ID = UUID.fromString("a9849020-45a7-40a8-9a19-d4ae2b28dd46");
    public final static String QUERY_3_STATEMENT = "SELECT `location`, `mintemp` FROM `weather_aus` WHERE `mintemp` > 10";
    public final static String QUERY_3_QUERY_HASH = "a3d3dd94ebc7653bb5a3b55dd8ed5e91d3d13c335c6855a1eb4eb7ca14c36ced";
    public final static String QUERY_3_RESULT_HASH = "ff3f7cbe1b96d396957f6e39e55b8b1b577fa3d305d4795af99594cfd30cb80d";
    public final static Instant QUERY_3_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2018-11-07 10:59:12 */;
    public final static String QUERY_3_STATEMENT_NORMALIZED = "SELECT `location`, `mintemp` FROM `weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_3_EXECUTION)) + "' WHERE `mintemp` > 10";
    public final static Long QUERY_3_RESULT_NUMBER = 2L;
    public final static Boolean QUERY_3_PERSISTED = true;

    public final static UUID QUERY_4_ID = UUID.fromString("18a98197-51ff-4011-9f40-914a11675a6d");
    public final static String QUERY_4_STATEMENT = "SELECT `id`, `value` FROM `mfcc`";
    public final static String QUERY_4_QUERY_HASH = "df7da3801dfb5c191ff6711d79ce6455f3c09ec8323ce1ff7208ab85387263f5";
    public final static String QUERY_4_RESULT_HASH = "ff4f7cbe1b96d496957f6e49e55b8b1b577fa4d405d4795af99594cfd40cb80d";
    public final static Instant QUERY_4_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2018-11-07 10:59:12 */;
    public final static String QUERY_4_STATEMENT_NORMALIZED = "SELECT `id`, `value` FROM `mfcc` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_4_EXECUTION)) + "'";
    public final static Long QUERY_4_RESULT_NUMBER = 6L;
    public final static Long QUERY_4_RESULT_ID = 4L;
    public final static Boolean QUERY_4_PERSISTED = false;

    public final List<Map<String, Object>> QUERY_4_RESULT_DTO = new LinkedList<>(List.of(
            new HashMap<>() {{
                put("id", BigInteger.valueOf(1L));
                put("value", 11.2);
            }}, new HashMap<>() {{
                put("id", BigInteger.valueOf(2L));
                put("value", 11.3);
            }}, new HashMap<>() {{
                put("id", BigInteger.valueOf(3L));
                put("value", 11.4);
            }}, new HashMap<>() {{
                put("id", BigInteger.valueOf(4L));
                put("value", 11.9);
            }}, new HashMap<>() {{
                put("id", BigInteger.valueOf(5L));
                put("value", 12.3);
            }}, new HashMap<>() {{
                put("id", BigInteger.valueOf(6L));
                put("value", 23.1);
            }}));

    public final QueryDto QUERY_4_DTO = QueryDto.builder()
            .id(QUERY_4_ID)
            .databaseId(DATABASE_3_ID)
            .query(QUERY_4_STATEMENT)
            .queryNormalized(QUERY_4_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .resultHash(QUERY_4_RESULT_HASH)
            .queryHash(QUERY_4_QUERY_HASH)
            .execution(QUERY_4_EXECUTION)
            .isPersisted(QUERY_4_PERSISTED)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final Subset QUERY_4_CACHE = Subset.builder()
            .id(QUERY_4_ID)
            .databaseId(DATABASE_3_ID)
            .query(QUERY_4_STATEMENT)
            .queryNormalized(QUERY_4_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .resultHash(QUERY_4_RESULT_HASH)
            .queryHash(QUERY_4_QUERY_HASH)
            .execution(QUERY_4_EXECUTION)
            .isPersisted(QUERY_4_PERSISTED)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static UUID QUERY_5_ID = UUID.fromString("1a39f775-e3d5-4865-b4f5-dbbb5693b637");
    public final static String QUERY_5_STATEMENT = "SELECT `id`, `value` FROM `mfcc` WHERE `value` > 0";
    public final static String QUERY_5_QUERY_HASH = "6d6dc48b12cdfd959d39a62887334a6bbd529b93eed4f211f3f671bd9e7d6225";
    public final static String QUERY_5_RESULT_HASH = "ff5f7cbe1b96d596957f6e59e55b8b1b577fa5d505d5795af99595cfd50cb80d";
    public final static Instant QUERY_5_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2018-11-07 10:59:12 */;
    public final static String QUERY_5_STATEMENT_NORMALIZED = "SELECT `id`, `value` FROM `mfcc` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_5_EXECUTION)) + "' WHERE `value` > 0";
    public final static Long QUERY_5_RESULT_NUMBER = 6L;
    public final static Boolean QUERY_5_PERSISTED = true;

    public final QueryDto QUERY_5_DTO = QueryDto.builder()
            .id(QUERY_5_ID)
            .databaseId(DATABASE_3_ID)
            .query(QUERY_5_STATEMENT)
            .queryNormalized(QUERY_5_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_5_RESULT_NUMBER)
            .resultHash(QUERY_5_RESULT_HASH)
            .queryHash(QUERY_5_QUERY_HASH)
            .execution(QUERY_5_EXECUTION)
            .isPersisted(QUERY_5_PERSISTED)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final Subset QUERY_5_CACHE = Subset.builder()
            .id(QUERY_5_ID)
            .databaseId(DATABASE_3_ID)
            .query(QUERY_5_STATEMENT)
            .queryNormalized(QUERY_5_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_5_RESULT_NUMBER)
            .resultHash(QUERY_5_RESULT_HASH)
            .queryHash(QUERY_5_QUERY_HASH)
            .execution(QUERY_5_EXECUTION)
            .isPersisted(QUERY_5_PERSISTED)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final Map<String, ColumnAnalysisResultDto> QUERY_5_ANALYSIS_MAP_DTO = new HashMap<>() {{
        put("id", ColumnAnalysisResultDto.builder()
                .name("id")
                .datatype(ColumnTypeDto.BIGINT)
                .build());
        put("value", ColumnAnalysisResultDto.builder()
                .name("value")
                .datatype(ColumnTypeDto.DECIMAL)
                .build());
    }};

    public final SubsetDto QUERY_5_SUBSET_DTO = SubsetDto.builder()
            .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_5_ID)))
            .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_8_1_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_8_2_ID).build())))
            .filters(new LinkedHashSet<>(Set.of(FilterDto.builder()
                    .columnId(COLUMN_8_2_ID)
                    .operatorId(IMAGE_1_OPERATORS_2_ID)
                    .value("0")
                    .type(FilterTypeDto.WHERE)
                    .build())))
            .build();

    public final ViewDto QUERY_5_VIEW_DTO = ViewDto.builder()
            .query(QUERY_5_STATEMENT)
            .queryHash(QUERY_5_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .columns(new LinkedList<>(List.of(ViewColumnDto.builder()
                            .name("id")
                            .internalName("id")
                            .build(),
                    ViewColumnDto.builder()
                            .name("value")
                            .internalName("value")
                            .build())))
            .build();

    public final List<Map<String, Object>> QUERY_5_RESULT_DTO = new LinkedList<>(List.of(
            Map.of("id", BigInteger.valueOf(1L), "value", 11.2),
            Map.of("id", BigInteger.valueOf(2L), "value", 11.3),
            Map.of("id", BigInteger.valueOf(3L), "value", 11.4),
            Map.of("id", BigInteger.valueOf(4L), "value", 11.9),
            Map.of("id", BigInteger.valueOf(5L), "value", 12.3),
            Map.of("id", BigInteger.valueOf(6L), "value", 23.1)
    ));

    public final static UUID QUERY_6_ID = UUID.fromString("7463412a-20c4-4fc1-8a33-948aea026f49");
    public final static String QUERY_6_STATEMENT = "SELECT `location` FROM `weather_aus` WHERE `id` = 1";
    public final static String QUERY_6_QUERY_HASH = "6d6dc48b12cdfd959d39a62887334a6bbd529b93eed4f211f3f671bd9e7d6225";
    public final static String QUERY_6_RESULT_HASH = "ff5f7cbe1b96d596957f6e59e55b8b1b577fa5d505d5795af99595cfd50cb80d";
    public final static Instant QUERY_6_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2018-11-07 10:59:12 */;
    public final static String QUERY_6_STATEMENT_NORMALIZED = "SELECT `location` FROM `weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_6_EXECUTION)) + "' WHERE `id` = 1";
    public final static Long QUERY_6_RESULT_NUMBER = 1L;
    public final static Boolean QUERY_6_PERSISTED = true;

    public final List<TableColumn> TABLE_1_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_1_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_1)
                    .name("id")
                    .internalName("id")
                    .columnType(TableColumnType.SERIAL)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_1)
                    .name("Date")
                    .internalName("date")
                    .columnType(TableColumnType.DATE)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_3_ID)
                    .ordinalPosition(2)
                    .table(TABLE_1)
                    .name("Location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_4_ID)
                    .ordinalPosition(3)
                    .table(TABLE_1)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_5_ID)
                    .ordinalPosition(4)
                    .table(TABLE_1)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .conceptUri(CONCEPT_1_URI)
                    .unitUri(UNIT_1_URI)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final static SchemaAnalysisResultDto TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO = SchemaAnalysisResultDto.builder()
            .comment("")
            .quote("\"")
            .delimiter(",")
            .escape("\\")
            .newlineDelimiter("\n")
            .hasHeader(true)
            .skipRows(1)
            .columns(new LinkedList<>(List.of(ColumnAnalysisResultDto.builder()
                            .name("id")
                            .datatype(ColumnTypeDto.BIGINT)
                            .nullAllowed(true)
                            .primaryKey(true)
                            .build(),
                    ColumnAnalysisResultDto.builder()
                            .name("Date")
                            .datatype(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .size(255)
                            .build(),
                    ColumnAnalysisResultDto.builder()
                            .name("Location")
                            .datatype(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .size(255)
                            .build(),
                    ColumnAnalysisResultDto.builder()
                            .name("MinTemp")
                            .datatype(ColumnTypeDto.DOUBLE)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .size(10)
                            .d(4)
                            .build(),
                    ColumnAnalysisResultDto.builder()
                            .name("Rainfall")
                            .datatype(ColumnTypeDto.DOUBLE)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .size(10)
                            .d(4)
                            .build())))
            .build();

    public final static UUID QUERY_7_ID = UUID.fromString("fe73a325-30a0-444c-b74f-23ce1533e55f");
    public final static String QUERY_7_STATEMENT = "SELECT id, date, a.location, lat, lng FROM weather_aus a JOIN weather_location l on a.location = l.location WHERE date = '2008-12-01'";
    public final static String QUERY_7_QUERY_HASH = "df7da3801dfb5c191ff6711d79ce6455f3c09ec8323ce1ff7208ab85387263f5";
    public final static String QUERY_7_RESULT_HASH = "ff4f7cbe1b96d496957f6e49e55b8b1b577fa4d405d4795af99594cfd40cb80d";
    public final static Instant QUERY_7_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2018-11-07 10:59:12 */;
    public final static String QUERY_7_STATEMENT_NORMALIZED = "SELECT id, date, weather_aus.location, lat, lng FROM weather_aus FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_7_EXECUTION)) + "' JOIN weather_location on weather_aus.location = weather_location.location WHERE date = '2008-12-01'";
    public final static Long QUERY_7_RESULT_NUMBER = 6L;
    public final static Long QUERY_7_RESULT_ID = 4L;
    public final static Boolean QUERY_7_PERSISTED = false;

    public final List<CreateTableColumnDto> TABLE_1_COLUMNS_CREATE_DTO = List.of(CreateTableColumnDto.builder()
                    .name("id")
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Date")
                    .type(ColumnTypeDto.DATE)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Location")
                    .type(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("MinTemp")
                    .type(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Rainfall")
                    .type(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .nullAllowed(true)
                    .conceptUri(CONCEPT_1_URI)
                    .unitUri(UNIT_1_URI)
                    .build());

    public final CreateTableConstraintsDto TABLE_1_CONSTRAINTS_CREATE_INVALID_DTO = CreateTableConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>(List.of(List.of("date"))))
            .build();

    public final CreateTableDto TABLE_1_CREATE_DTO = CreateTableDto.builder()
            .name(TABLE_1_NAME)
            .description(TABLE_1_DESCRIPTION)
            .columns(TABLE_1_COLUMNS_CREATE_DTO)
            .constraints(TABLE_1_CREATE_CONSTRAINTS_DTO)
            .build();

    public final List<TableColumn> TABLE_2_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_2_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_2)
                    .name("location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_2_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_2)
                    .name("lat")
                    .internalName("lat")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_2_3_ID)
                    .ordinalPosition(2)
                    .table(TABLE_2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final static UUID COLUMN_3_1_ID = UUID.fromString("49cc2735-ba75-4e12-8ac7-8aec87ed7724");
    public final static UUID COLUMN_3_2_ID = UUID.fromString("2c240d64-3052-4a74-b696-e7490fdff3ea");
    public final static UUID COLUMN_3_3_ID = UUID.fromString("6fbb0a56-f23a-4aa4-b158-c614a0a30f86");
    public final static UUID COLUMN_3_4_ID = UUID.fromString("9b01f925-93ee-4f28-bf31-9902900a7099");
    public final static UUID COLUMN_3_5_ID = UUID.fromString("9bbd66f1-0d94-401c-b7f7-6e329bb9ee21");
    public final static UUID COLUMN_3_6_ID = UUID.fromString("19ad93d7-b298-495b-9678-9aac80678ff9");
    public final static UUID COLUMN_3_7_ID = UUID.fromString("4d27d9f4-645f-4222-b5a8-4a91fa6e4275");
    public final static UUID COLUMN_3_8_ID = UUID.fromString("b4f8fcf8-5824-45ec-8c58-43f20e6dffc5");
    public final static UUID COLUMN_3_9_ID = UUID.fromString("87247218-369e-484a-9a8f-d758478d8dfc");
    public final static UUID COLUMN_3_10_ID = UUID.fromString("6e191b97-189a-4d88-901e-888ca889e280");
    public final static UUID COLUMN_3_11_ID = UUID.fromString("6ac356ff-9be5-4259-9b62-83b6707be7fe");
    public final static UUID COLUMN_3_12_ID = UUID.fromString("0665b384-c824-4358-b6c5-f17706d46ea4");
    public final static UUID COLUMN_3_13_ID = UUID.fromString("22d3676e-d28e-4075-b223-91a7ac767bcf");
    public final static UUID COLUMN_3_14_ID = UUID.fromString("673326e3-ee2b-4c2f-902f-982e2abce1c2");
    public final static UUID COLUMN_3_15_ID = UUID.fromString("8dcacf4a-736b-4e67-9618-74998cba8940");
    public final static UUID COLUMN_3_16_ID = UUID.fromString("2b2f5359-76d3-4763-a53f-d18ca6b793fb");
    public final static UUID COLUMN_3_17_ID = UUID.fromString("674b6120-06cf-4624-b006-1ed48898bd69");
    public final static UUID COLUMN_3_18_ID = UUID.fromString("13edd7c9-6c88-44d7-b206-34774e49c5af");
    public final static UUID COLUMN_3_19_ID = UUID.fromString("6977bb3f-4ae2-43ea-bb82-c7f68454c538");
    public final static UUID COLUMN_3_20_ID = UUID.fromString("c03d2429-53e1-42eb-a1f5-ce342fa23336");
    public final static UUID COLUMN_3_21_ID = UUID.fromString("06edd332-750e-4aa1-b61b-e757fb2312c3");
    public final static UUID COLUMN_3_22_ID = UUID.fromString("b6b8631d-f283-49da-8d5e-4bb24def2a40");
    public final static UUID COLUMN_3_23_ID = UUID.fromString("0393ee00-31ba-44ab-9e82-1f5034a9f57b");
    public final static UUID COLUMN_3_24_ID = UUID.fromString("a63784ea-f70d-4bda-ace6-1c6a88edf831");
    public final static UUID COLUMN_3_25_ID = UUID.fromString("720fe829-802c-420b-8e41-bdbb636db43c");
    public final static UUID COLUMN_3_26_ID = UUID.fromString("5bce38ef-7d49-43b5-9054-068750684b5f");
    public final static UUID COLUMN_3_27_ID = UUID.fromString("92097c02-3dd3-40ea-bd03-a9135f45a557");
    public final static UUID COLUMN_3_28_ID = UUID.fromString("7361a38a-828b-495e-8a57-b36cca17d7db");
    public final static UUID COLUMN_3_29_ID = UUID.fromString("a06812db-03b7-484c-92a6-45d94eef3bb9");
    public final static UUID COLUMN_3_30_ID = UUID.fromString("05614d89-9216-47ea-96f0-acffc4674acf");
    public final static UUID COLUMN_3_31_ID = UUID.fromString("05ada13d-361a-48e7-9a0f-1191499509f1");
    public final static UUID COLUMN_3_32_ID = UUID.fromString("b3f259f6-700a-4b60-8eac-dceaa0dcda9d");
    public final static UUID COLUMN_3_33_ID = UUID.fromString("9160af06-e168-4b10-a7f9-520f41ae7955");
    public final static UUID COLUMN_3_34_ID = UUID.fromString("fde20c99-ed9c-4a60-8c18-f46e8603ebb5");
    public final static UUID COLUMN_3_35_ID = UUID.fromString("071c7f27-1cdd-4af9-b4d6-f932c27c7287");

    public final ColumnBriefDto TABLE_3_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_3_1_ID)
            .columnType(ColumnTypeDto.BIGINT)
            .name("id")
            .internalName("id")
            .build();

    public final List<TableColumn> TABLE_3_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_3_1_ID)
                    .table(TABLE_3)
                    .ordinalPosition(0)
                    .columnType(TableColumnType.BIGINT)
                    .name("id")
                    .internalName("id")
                    .isNullAllowed(false)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_2_ID)
                    .table(TABLE_3)
                    .ordinalPosition(1)
                    .columnType(TableColumnType.INT)
                    .name("linie")
                    .internalName("linie")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_3_ID)
                    .table(TABLE_3)
                    .ordinalPosition(2)
                    .columnType(TableColumnType.INT)
                    .name("richtung")
                    .internalName("richtung")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_4_ID)
                    .table(TABLE_3)
                    .ordinalPosition(3)
                    .columnType(TableColumnType.DATE)
                    .name("betriebsdatum")
                    .internalName("betriebsdatum")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_5_ID)
                    .table(TABLE_3)
                    .ordinalPosition(4)
                    .columnType(TableColumnType.INT)
                    .name("fahrzeug")
                    .internalName("fahrzeug")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_6_ID)
                    .table(TABLE_3)
                    .ordinalPosition(5)
                    .columnType(TableColumnType.INT)
                    .name("kurs")
                    .internalName("kurs")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_7_ID)
                    .table(TABLE_3)
                    .ordinalPosition(6)
                    .columnType(TableColumnType.INT)
                    .name("seq_von")
                    .internalName("seq_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_8_ID)
                    .table(TABLE_3)
                    .ordinalPosition(7)
                    .columnType(TableColumnType.INT)
                    .name("halt_diva_von")
                    .internalName("halt_diva_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_9_ID)
                    .table(TABLE_3)
                    .ordinalPosition(8)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_diva_von")
                    .internalName("halt_punkt_diva_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_10_ID)
                    .table(TABLE_3)
                    .ordinalPosition(9)
                    .columnType(TableColumnType.INT)
                    .name("halt_kurz_von1")
                    .internalName("halt_kurz_von1")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_11_ID)
                    .table(TABLE_3)
                    .ordinalPosition(10)
                    .columnType(TableColumnType.DATE)
                    .name("datum_von")
                    .internalName("datum_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_12_ID)
                    .table(TABLE_3)
                    .ordinalPosition(11)
                    .columnType(TableColumnType.INT)
                    .name("soll_an_von")
                    .internalName("soll_an_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_13_ID)
                    .table(TABLE_3)
                    .ordinalPosition(12)
                    .columnType(TableColumnType.INT)
                    .name("ist_an_von")
                    .internalName("ist_an_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_14_ID)
                    .table(TABLE_3)
                    .ordinalPosition(13)
                    .columnType(TableColumnType.INT)
                    .name("soll_ab_von")
                    .internalName("soll_ab_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_15_ID)
                    .table(TABLE_3)
                    .ordinalPosition(14)
                    .columnType(TableColumnType.INT)
                    .name("ist_ab_von")
                    .internalName("ist_ab_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_16_ID)
                    .table(TABLE_3)
                    .ordinalPosition(15)
                    .columnType(TableColumnType.INT)
                    .name("seq_nach")
                    .internalName("seq_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_17_ID)
                    .table(TABLE_3)
                    .ordinalPosition(16)
                    .columnType(TableColumnType.INT)
                    .name("halt_diva_nach")
                    .internalName("halt_diva_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_18_ID)
                    .table(TABLE_3)
                    .ordinalPosition(17)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_diva_nach")
                    .internalName("halt_punkt_diva_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_19_ID)
                    .table(TABLE_3)
                    .ordinalPosition(18)
                    .columnType(TableColumnType.INT)
                    .name("halt_kurz_nach1")
                    .internalName("halt_kurz_nach1")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_20_ID)
                    .table(TABLE_3)
                    .ordinalPosition(19)
                    .columnType(TableColumnType.DATE)
                    .name("datum_nach")
                    .internalName("datum_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_21_ID)
                    .table(TABLE_3)
                    .ordinalPosition(20)
                    .columnType(TableColumnType.INT)
                    .name("soll_an_nach")
                    .internalName("soll_an_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_22_ID)
                    .table(TABLE_3)
                    .ordinalPosition(21)
                    .columnType(TableColumnType.INT)
                    .name("ist_an_nach1")
                    .internalName("ist_an_nach1")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_23_ID)
                    .table(TABLE_3)
                    .ordinalPosition(22)
                    .columnType(TableColumnType.INT)
                    .name("soll_ab_nach")
                    .internalName("soll_ab_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_24_ID)
                    .table(TABLE_3)
                    .ordinalPosition(23)
                    .columnType(TableColumnType.INT)
                    .name("ist_ab_nach")
                    .internalName("ist_ab_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_25_ID)
                    .table(TABLE_3)
                    .ordinalPosition(24)
                    .columnType(TableColumnType.INT)
                    .name("fahrt_id")
                    .internalName("fahrt_id")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_26_ID)
                    .table(TABLE_3)
                    .ordinalPosition(25)
                    .columnType(TableColumnType.INT)
                    .name("fahrweg_id")
                    .internalName("fahrweg_id")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_27_ID)
                    .table(TABLE_3)
                    .ordinalPosition(26)
                    .columnType(TableColumnType.INT)
                    .name("fw_no")
                    .internalName("fw_no")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_28_ID)
                    .table(TABLE_3)
                    .ordinalPosition(27)
                    .columnType(TableColumnType.INT)
                    .name("fw_typ")
                    .internalName("fw_typ")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_29_ID)
                    .table(TABLE_3)
                    .ordinalPosition(28)
                    .columnType(TableColumnType.INT)
                    .name("fw_kurz")
                    .internalName("fw_kurz")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_30_ID)
                    .table(TABLE_3)
                    .ordinalPosition(29)
                    .columnType(TableColumnType.INT)
                    .name("fw_lang")
                    .internalName("fw_lang")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_31_ID)
                    .table(TABLE_3)
                    .ordinalPosition(30)
                    .columnType(TableColumnType.INT)
                    .name("umlauf_von")
                    .internalName("umlauf_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_32_ID)
                    .table(TABLE_3)
                    .ordinalPosition(31)
                    .columnType(TableColumnType.INT)
                    .name("halt_id_von")
                    .internalName("halt_id_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_33_ID)
                    .table(TABLE_3)
                    .ordinalPosition(32)
                    .columnType(TableColumnType.INT)
                    .name("halt_id_nach")
                    .internalName("halt_id_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_34_ID)
                    .table(TABLE_3)
                    .ordinalPosition(33)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_id_von")
                    .internalName("halt_punkt_id_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_3_35_ID)
                    .table(TABLE_3)
                    .ordinalPosition(34)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_id_nach")
                    .internalName("halt_punkt_id_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build());

    public final List<ColumnDto> TABLE_3_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_3_1_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.BIGINT)
                    .name("id")
                    .internalName("id")
                    .isNullAllowed(false)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_2_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("linie")
                    .internalName("linie")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_3_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("richtung")
                    .internalName("richtung")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_4_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.DATE)
                    .name("betriebsdatum")
                    .internalName("betriebsdatum")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_5_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("fahrzeug")
                    .internalName("fahrzeug")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_6_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("kurs")
                    .internalName("kurs")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_7_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("seq_von")
                    .internalName("seq_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_8_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_diva_von")
                    .internalName("halt_diva_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_9_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_diva_von")
                    .internalName("halt_punkt_diva_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_10_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_kurz_von1")
                    .internalName("halt_kurz_von1")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_11_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.DATE)
                    .name("datum_von")
                    .internalName("datum_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_12_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_an_von")
                    .internalName("soll_an_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_13_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_an_von")
                    .internalName("ist_an_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_14_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_ab_von")
                    .internalName("soll_ab_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_15_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_ab_von")
                    .internalName("ist_ab_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_16_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("seq_nach")
                    .internalName("seq_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_17_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_diva_nach")
                    .internalName("halt_diva_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_18_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_diva_nach")
                    .internalName("halt_punkt_diva_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_19_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_kurz_nach1")
                    .internalName("halt_kurz_nach1")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_20_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.DATE)
                    .name("datum_nach")
                    .internalName("datum_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_21_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_an_nach")
                    .internalName("soll_an_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_22_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_an_nach1")
                    .internalName("ist_an_nach1")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_23_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_ab_nach")
                    .internalName("soll_ab_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_24_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_ab_nach")
                    .internalName("ist_ab_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_25_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("fahrt_id")
                    .internalName("fahrt_id")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_26_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("fahrweg_id")
                    .internalName("fahrweg_id")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_27_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_no")
                    .internalName("fw_no")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_28_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_typ")
                    .internalName("fw_typ")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_29_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_kurz")
                    .internalName("fw_kurz")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_30_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_lang")
                    .internalName("fw_lang")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_31_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("umlauf_von")
                    .internalName("umlauf_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_32_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_id_von")
                    .internalName("halt_id_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_33_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_id_nach")
                    .internalName("halt_id_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_34_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_id_von")
                    .internalName("halt_punkt_id_von")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_3_35_ID)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_id_nach")
                    .internalName("halt_punkt_id_nach")
                    .isNullAllowed(true)
                    .enums(new LinkedList<>())
                    .sets(new LinkedList<>())
                    .build());

    public final List<Column> TABLE_3_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_3_1_ID)
                    .columnType(ColumnType.BIGINT)
                    .internalName("id")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_2_ID)
                    .columnType(ColumnType.INT)
                    .internalName("linie")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_3_ID)
                    .columnType(ColumnType.INT)
                    .internalName("richtung")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_4_ID)
                    .columnType(ColumnType.DATE)
                    .internalName("betriebsdatum")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_5_ID)
                    .columnType(ColumnType.INT)
                    .internalName("fahrzeug")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_6_ID)
                    .columnType(ColumnType.INT)
                    .internalName("kurs")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_7_ID)
                    .columnType(ColumnType.INT)
                    .internalName("seq_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_8_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_diva_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_9_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_punkt_diva_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_10_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_kurz_von1")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_11_ID)
                    .columnType(ColumnType.DATE)
                    .internalName("datum_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_12_ID)
                    .columnType(ColumnType.INT)
                    .internalName("soll_an_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_13_ID)
                    .columnType(ColumnType.INT)
                    .internalName("ist_an_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_14_ID)
                    .columnType(ColumnType.INT)
                    .internalName("soll_ab_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_15_ID)
                    .columnType(ColumnType.INT)
                    .internalName("ist_ab_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_16_ID)
                    .columnType(ColumnType.INT)
                    .internalName("seq_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_17_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_diva_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_18_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_punkt_diva_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_19_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_kurz_nach1")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_20_ID)
                    .columnType(ColumnType.DATE)
                    .internalName("datum_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_21_ID)
                    .columnType(ColumnType.INT)
                    .internalName("soll_an_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_22_ID)
                    .columnType(ColumnType.INT)
                    .internalName("ist_an_nach1")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_23_ID)
                    .columnType(ColumnType.INT)
                    .internalName("soll_ab_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_24_ID)
                    .columnType(ColumnType.INT)
                    .internalName("ist_ab_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_25_ID)
                    .columnType(ColumnType.INT)
                    .internalName("fahrt_id")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_26_ID)
                    .columnType(ColumnType.INT)
                    .internalName("fahrweg_id")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_27_ID)
                    .columnType(ColumnType.INT)
                    .internalName("fw_no")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_28_ID)
                    .columnType(ColumnType.INT)
                    .internalName("fw_typ")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_29_ID)
                    .columnType(ColumnType.INT)
                    .internalName("fw_kurz")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_30_ID)
                    .columnType(ColumnType.INT)
                    .internalName("fw_lang")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_31_ID)
                    .columnType(ColumnType.INT)
                    .internalName("umlauf_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_32_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_id_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_33_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_id_nach")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_34_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_punkt_id_von")
                    .build(),
            Column.builder()
                    .id(COLUMN_3_35_ID)
                    .columnType(ColumnType.INT)
                    .internalName("halt_punkt_id_nach")
                    .build());

    public final static UUID COLUMN_5_1_ID = UUID.fromString("4efd4cbb-ca2e-48e2-8f40-37514956aa67");
    public final static UUID COLUMN_5_2_ID = UUID.fromString("53061685-c1db-4df6-ad4e-8f384a200104");
    public final static UUID COLUMN_5_3_ID = UUID.fromString("643f9cda-8db1-47a4-bb08-c10e78e54c10");
    public final static UUID COLUMN_5_4_ID = UUID.fromString("efeacc15-3b31-4a9f-9dba-f07d62dcddd6");
    public final static UUID COLUMN_5_5_ID = UUID.fromString("0319db31-473a-47bc-bb9d-fa1edf82fcd5");
    public final static UUID COLUMN_5_6_ID = UUID.fromString("9ba789ca-59cf-4480-b9f6-3b957b1d7f5c");
    public final static UUID COLUMN_5_7_ID = UUID.fromString("81c42954-fd1a-4fef-adb1-bc4945469e26");
    public final static UUID COLUMN_5_8_ID = UUID.fromString("49a38905-52a2-4a9b-b7b9-5e1dcf799b2a");
    public final static UUID COLUMN_5_9_ID = UUID.fromString("1e1a9b6b-5aee-4773-b52d-ea56a5d1e2c8");
    public final static UUID COLUMN_5_10_ID = UUID.fromString("42ede62a-ae98-4a14-ba54-76b8ba1c580f");
    public final static UUID COLUMN_5_11_ID = UUID.fromString("0af0f84a-5a58-418a-8bbc-bde29ed0cda0");
    public final static UUID COLUMN_5_12_ID = UUID.fromString("d9cb30a2-1566-4bd1-899d-060a8ba47722");
    public final static UUID COLUMN_5_13_ID = UUID.fromString("e69f7f75-3731-4706-8193-0393aa0c08a7");
    public final static UUID COLUMN_5_14_ID = UUID.fromString("4441630e-7dfa-4046-8bc2-929860f1c66e");
    public final static UUID COLUMN_5_15_ID = UUID.fromString("f0a12be0-0b26-4686-bf7e-539cdc7e71b4");
    public final static UUID COLUMN_5_16_ID = UUID.fromString("b60abdcc-5786-40f8-a309-e4467f7d963c");
    public final static UUID COLUMN_5_17_ID = UUID.fromString("6d5877e2-daef-43d6-a1b6-1aff3ab1a9a2");
    public final static UUID COLUMN_5_18_ID = UUID.fromString("bb45455f-d449-496e-94f8-eac4d46ba9c0");
    public final static UUID COLUMN_5_19_ID = UUID.fromString("44c5484b-b57d-48a4-8f24-d2074de98e1a");
    public final static UUID COLUMN_5_20_ID = UUID.fromString("6475b937-71fc-4331-bc85-8ee71fa68d99");
    public final static UUID COLUMN_5_21_ID = UUID.fromString("92ff472f-e203-4c8e-b243-81640229ca19");

    public final ColumnBriefDto TABLE_5_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_5_1_ID)
            .name("id")
            .internalName("id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final List<TableColumn> TABLE_5_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_5_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_5)
                    .name("id")
                    .internalName("id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_5)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_3_ID)
                    .ordinalPosition(2)
                    .table(TABLE_5)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_4_ID)
                    .ordinalPosition(3)
                    .table(TABLE_5)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_5_ID)
                    .ordinalPosition(4)
                    .table(TABLE_5)
                    .name("Bread")
                    .internalName("bread")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_6_ID)
                    .ordinalPosition(5)
                    .table(TABLE_5)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_7_ID)
                    .ordinalPosition(6)
                    .table(TABLE_5)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_8_ID)
                    .ordinalPosition(7)
                    .table(TABLE_5)
                    .name("Water")
                    .internalName("water")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_9_ID)
                    .ordinalPosition(8)
                    .table(TABLE_5)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_10_ID)
                    .ordinalPosition(9)
                    .table(TABLE_5)
                    .name("Waterborne")
                    .internalName("waterborne")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_11_ID)
                    .ordinalPosition(10)
                    .table(TABLE_5)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_12_ID)
                    .ordinalPosition(11)
                    .table(TABLE_5)
                    .name("Predator")
                    .internalName("predator")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_13_ID)
                    .ordinalPosition(12)
                    .table(TABLE_5)
                    .name("Backbone")
                    .internalName("backbone")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_14_ID)
                    .ordinalPosition(13)
                    .table(TABLE_5)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_15_ID)
                    .ordinalPosition(14)
                    .table(TABLE_5)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_16_ID)
                    .ordinalPosition(15)
                    .table(TABLE_5)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_17_ID)
                    .ordinalPosition(16)
                    .table(TABLE_5)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(TableColumnType.INT)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_18_ID)
                    .ordinalPosition(17)
                    .table(TABLE_5)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_19_ID)
                    .ordinalPosition(18)
                    .table(TABLE_5)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_20_ID)
                    .ordinalPosition(19)
                    .table(TABLE_5)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_21_ID)
                    .ordinalPosition(20)
                    .table(TABLE_5)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<Column> TABLE_5_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_5_1_ID)
                    .internalName("id")
                    .columnType(ColumnType.BIGINT)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_2_ID)
                    .internalName("animal_name")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_3_ID)
                    .internalName("hair")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_4_ID)
                    .internalName("feathers")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_5_ID)
                    .internalName("bread")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_6_ID)
                    .internalName("eggs")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_7_ID)
                    .internalName("milk")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_8_ID)
                    .internalName("water")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_9_ID)
                    .internalName("airborne")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_10_ID)
                    .internalName("waterborne")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_11_ID)
                    .internalName("aquantic")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_12_ID)
                    .internalName("predator")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_13_ID)
                    .internalName("backbone")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_14_ID)
                    .internalName("breathes")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_15_ID)
                    .internalName("venomous")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_16_ID)
                    .internalName("fin")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_17_ID)
                    .internalName("legs")
                    .columnType(ColumnType.INT)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_18_ID)
                    .internalName("tail")
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_19_ID)
                    .internalName("domestic")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_20_ID)
                    .internalName("catsize")
                    .columnType(ColumnType.BOOL)
                    .build(),
            Column.builder()
                    .id(COLUMN_5_21_ID)
                    .internalName("class_type")
                    .columnType(ColumnType.DECIMAL)
                    .build());

    public final List<ColumnDto> TABLE_5_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_5_1_ID)
                    .ordinalPosition(0)
                    .tableId(TABLE_5_ID)
                    .name("id")
                    .internalName("id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_2_ID)
                    .ordinalPosition(1)
                    .tableId(TABLE_5_ID)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_3_ID)
                    .ordinalPosition(2)
                    .tableId(TABLE_5_ID)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_4_ID)
                    .ordinalPosition(3)
                    .tableId(TABLE_5_ID)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_5_ID)
                    .ordinalPosition(4)
                    .tableId(TABLE_5_ID)
                    .name("Bread")
                    .internalName("bread")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_6_ID)
                    .ordinalPosition(5)
                    .tableId(TABLE_5_ID)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_7_ID)
                    .ordinalPosition(6)
                    .tableId(TABLE_5_ID)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_8_ID)
                    .ordinalPosition(7)
                    .tableId(TABLE_5_ID)
                    .name("Water")
                    .internalName("water")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_9_ID)
                    .ordinalPosition(8)
                    .tableId(TABLE_5_ID)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_10_ID)
                    .ordinalPosition(9)
                    .tableId(TABLE_5_ID)
                    .name("Waterborne")
                    .internalName("waterborne")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_11_ID)
                    .ordinalPosition(10)
                    .tableId(TABLE_5_ID)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_12_ID)
                    .ordinalPosition(11)
                    .tableId(TABLE_5_ID)
                    .name("Predator")
                    .internalName("predator")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_13_ID)
                    .ordinalPosition(12)
                    .tableId(TABLE_5_ID)
                    .name("Backbone")
                    .internalName("backbone")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_14_ID)
                    .ordinalPosition(13)
                    .tableId(TABLE_5_ID)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_15_ID)
                    .ordinalPosition(14)
                    .tableId(TABLE_5_ID)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_16_ID)
                    .ordinalPosition(15)
                    .tableId(TABLE_5_ID)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_17_ID)
                    .ordinalPosition(16)
                    .tableId(TABLE_5_ID)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(ColumnTypeDto.INT)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_18_ID)
                    .ordinalPosition(17)
                    .tableId(TABLE_5_ID)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_19_ID)
                    .ordinalPosition(18)
                    .tableId(TABLE_5_ID)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_20_ID)
                    .ordinalPosition(19)
                    .tableId(TABLE_5_ID)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_21_ID)
                    .ordinalPosition(20)
                    .tableId(TABLE_5_ID)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<CreateForeignKeyDto> TABLE_5_FOREIGN_KEYS_INVALID_CREATE = List.of(CreateForeignKeyDto.builder()
            .columns(new LinkedList<>(List.of("somecolumn")))
            .referencedTable("sometable")
            .referencedColumns(new LinkedList<>(List.of("someothercolumn")))
            .build());

    public final CreateTableConstraintsDto TABLE_5_CONSTRAINTS_INVALID_CREATE = CreateTableConstraintsDto.builder()
            .foreignKeys(TABLE_5_FOREIGN_KEYS_INVALID_CREATE)
            .build();

    public final List<CreateTableColumnDto> TABLE_5_COLUMNS_CREATE = List.of(CreateTableColumnDto.builder()
                    .name("id")
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Animal Name")
                    .type(ColumnTypeDto.VARCHAR)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Hair")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Feathers")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Bread")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Eggs")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Milk")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Water")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Airborne")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Waterborne")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Aquantic")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Predator")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Backbone")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Breathes")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Venomous")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Fin")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Legs")
                    .type(ColumnTypeDto.INT)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Tail")
                    .type(ColumnTypeDto.DECIMAL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Domestic")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Catsize")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("Class Type")
                    .type(ColumnTypeDto.DECIMAL)
                    .nullAllowed(true)
                    .build());

    public final CreateTableConstraintsDto TABLE_5_CREATE_CONSTRAINTS_DTO = CreateTableConstraintsDto.builder()
            .primaryKey(Set.of("id"))
            .uniques(new LinkedList<>(List.of(List.of("id"))))
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .build();

    public final CreateTableDto TABLE_5_CREATE_DTO = CreateTableDto.builder()
            .name(TABLE_5_NAME)
            .description(TABLE_5_DESCRIPTION)
            .columns(TABLE_5_COLUMNS_CREATE)
            .constraints(TABLE_5_CREATE_CONSTRAINTS_DTO)
            .build();

    public final CreateTableDto TABLE_5_INVALID_CREATE_DTO = CreateTableDto.builder()
            .name(TABLE_5_NAME)
            .description(TABLE_5_DESCRIPTION)
            .columns(TABLE_5_COLUMNS_CREATE)
            .constraints(TABLE_5_CONSTRAINTS_INVALID_CREATE)
            .build();

    public final static UUID QUERY_8_ID = UUID.fromString("1c466eee-d551-4ef9-a7e0-b5a2d1b15473");
    public final static String QUERY_8_STATEMENT = "SELECT `id`, `animal_name` FROM `zoo` WHERE `hair` = TRUE AND `feathers` = false;";
    public final static String QUERY_8_QUERY_HASH = "f0ee0d6dd45e092fca120c4f0eab089f91ed26ccf8dc34a03c6b9c6bb4141271";
    public final static Long QUERY_8_RESULT_NUMBER = 5L;
    public final static String QUERY_8_RESULT_HASH = "b5f9cae916d32deff81c5f2e9f8ff43904034bc084b12320730953d120698bed";
    public final static Instant QUERY_8_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static String QUERY_8_STATEMENT_NORMALIZED = "SELECT `id`, `animal_name` FROM `zoo` FOR SYSTEM_TIME AS OF TIMESTAMP '" + MARIADB_DATE_FORMAT.format(Date.from(QUERY_8_EXECUTION)) + "' WHERE `hair` = TRUE AND `feathers` = false;";
    public final static Boolean QUERY_8_PERSISTED = true;

    public final static UUID COLUMN_6_1_ID = UUID.fromString("27b04a64-2849-4fae-b295-858c3e50361f");
    public final static UUID COLUMN_6_2_ID = UUID.fromString("1ea62e32-5719-4152-94da-45d37eb88b6f");
    public final static UUID COLUMN_6_3_ID = UUID.fromString("f523f9f5-42f7-4695-841e-a5fd30fa6879");
    public final static UUID COLUMN_6_4_ID = UUID.fromString("f57ea880-f917-4127-bcbb-202a34831383");
    public final static UUID COLUMN_6_5_ID = UUID.fromString("38aaeb63-b94b-4d90-8eae-a626dfb1f092");
    public final static UUID COLUMN_6_6_ID = UUID.fromString("f788cf6f-66ed-4f28-8b24-d9d173c4d340");

    public final List<TableColumn> TABLE_6_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_6_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_6)
                    .name("id")
                    .internalName("id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_6_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_6)
                    .name("firstname")
                    .internalName("firstname")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_6_3_ID)
                    .ordinalPosition(2)
                    .table(TABLE_6)
                    .name("lastname")
                    .internalName("lastname")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_6_4_ID)
                    .ordinalPosition(3)
                    .table(TABLE_6)
                    .name("birth")
                    .internalName("birth")
                    .columnType(TableColumnType.YEAR)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_6_5_ID)
                    .ordinalPosition(4)
                    .table(TABLE_6)
                    .name("reminder")
                    .internalName("reminder")
                    .columnType(TableColumnType.TIME)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_6_6_ID)
                    .ordinalPosition(5)
                    .table(TABLE_6)
                    .name("ref_id")
                    .internalName("ref_id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(true)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final ColumnBriefDto TABLE_6_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_6_1_ID)
            .name("id")
            .internalName("id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final List<Column> TABLE_6_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_6_1_ID)
                    .internalName("id")
                    .columnType(ColumnType.BIGINT)
                    .build(),
            Column.builder()
                    .id(COLUMN_6_2_ID)
                    .internalName("firstname")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            Column.builder()
                    .id(COLUMN_6_3_ID)
                    .internalName("lastname")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            Column.builder()
                    .id(COLUMN_6_4_ID)
                    .internalName("birth")
                    .columnType(ColumnType.YEAR)
                    .build(),
            Column.builder()
                    .id(COLUMN_6_5_ID)
                    .internalName("reminder")
                    .columnType(ColumnType.TIME)
                    .build(),
            Column.builder()
                    .id(COLUMN_6_6_ID)
                    .internalName("ref_id")
                    .columnType(ColumnType.BIGINT)
                    .build());

    public final List<ColumnDto> TABLE_6_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_6_1_ID)
                    .ordinalPosition(0)
                    .tableId(TABLE_6_ID)
                    .name("id")
                    .internalName("id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_6_2_ID)
                    .ordinalPosition(1)
                    .tableId(TABLE_6_ID)
                    .name("firstname")
                    .internalName("firstname")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_6_3_ID)
                    .ordinalPosition(2)
                    .tableId(TABLE_6_ID)
                    .name("lastname")
                    .internalName("lastname")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_6_4_ID)
                    .ordinalPosition(3)
                    .tableId(TABLE_6_ID)
                    .name("birth")
                    .internalName("birth")
                    .columnType(ColumnTypeDto.YEAR)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_6_5_ID)
                    .ordinalPosition(4)
                    .tableId(TABLE_6_ID)
                    .name("reminder")
                    .internalName("reminder")
                    .columnType(ColumnTypeDto.TIME)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_6_6_ID)
                    .ordinalPosition(5)
                    .tableId(TABLE_6_ID)
                    .name("ref_id")
                    .internalName("ref_id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(true)
                    .build());

    public final List<List<String>> TABLE_6_UNIQUES_CREATE = List.of(
            List.of("firstname", "lastname"));

    public final List<CreateForeignKeyDto> TABLE_6_FOREIGN_KEYS_CREATE = List.of(CreateForeignKeyDto.builder()
            .columns(new LinkedList<>(List.of("ref_id")))
            .referencedTable("zoo")
            .referencedColumns(new LinkedList<>(List.of("id")))
            .build());

    public final CreateTableConstraintsDto TABLE_6_CONSTRAINTS_CREATE = CreateTableConstraintsDto.builder()
            .uniques(TABLE_6_UNIQUES_CREATE)
            .foreignKeys(TABLE_6_FOREIGN_KEYS_CREATE)
            .checks(Set.of("firstname != lastname"))
            .primaryKey(Set.of("id"))
            .build();

    public final List<CreateTableColumnDto> TABLE_6_COLUMNS_CREATE = List.of(
            CreateTableColumnDto.builder()
                    .name("name_id")
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .build(),
            CreateTableColumnDto.builder()
                    .name("zoo_id")
                    .type(ColumnTypeDto.BIGINT)
                    .size(255L)
                    .nullAllowed(false)
                    .build());

    public final CreateTableDto TABLE_6_CREATE_DTO = CreateTableDto.builder()
            .name(TABLE_6_NAME)
            .description(TABLE_6_DESCRIPTION)
            .columns(TABLE_6_COLUMNS_CREATE)
            .constraints(TABLE_6_CONSTRAINTS_CREATE)
            .build();

    public final static UUID COLUMN_7_1_ID = UUID.fromString("395b44a4-0e31-41ea-94ad-c4f2d5e912c6");
    public final static UUID COLUMN_7_2_ID = UUID.fromString("5713333b-872a-44c5-ab94-4d0ab62f5663");

    public final ColumnBriefDto TABLE_7_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_7_1_ID)
            .name("name_id")
            .internalName("name_id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final ColumnBriefDto TABLE_7_COLUMNS_BRIEF_1_DTO = ColumnBriefDto.builder()
            .id(COLUMN_7_2_ID)
            .name("zoo_id")
            .internalName("zoo_id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final List<TableColumn> TABLE_7_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_7_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_7)
                    .name("name_id")
                    .internalName("name_id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_7_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_7)
                    .name("zoo_id")
                    .internalName("zoo_id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<ColumnDto> TABLE_7_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_7_1_ID)
                    .ordinalPosition(0)
                    .tableId(TABLE_7_ID)
                    .name("name_id")
                    .internalName("name_id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_7_2_ID)
                    .ordinalPosition(1)
                    .tableId(TABLE_7_ID)
                    .name("zoo_id")
                    .internalName("zoo_id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .sets(new LinkedList<>())
                    .enums(new LinkedList<>())
                    .build());

    public final List<Column> TABLE_7_COLUMNS_CACHE = List.of(Column.builder()
                    .id(COLUMN_7_1_ID)
                    .internalName("name_id")
                    .columnType(ColumnType.BIGINT)
                    .build(),
            Column.builder()
                    .id(COLUMN_7_2_ID)
                    .internalName("zoo_id")
                    .columnType(ColumnType.BIGINT)
                    .build());

    public final static UUID VIEW_1_ID = UUID.fromString("7d712cf7-78c7-4a47-90b0-d6b9f7f19b70");
    public final static Boolean VIEW_1_INITIAL_VIEW = false;
    public final static String VIEW_1_NAME = "JUnit";
    public final static String VIEW_1_INTERNAL_NAME = "junit";
    public final static Boolean VIEW_1_PUBLIC = false;
    public final static Boolean VIEW_1_SCHEMA_PUBLIC = false;
    public final static String VIEW_1_QUERY = "SELECT `location`, `lat`, `lng` FROM `weather_location`";
    public final static String VIEW_1_QUERY_HASH = "dc81a6877c7c51a6a6f406e1fc2a255e44a0d49a20548596e0d583c3eb849c23";
    public final static UUID VIEW_COLUMN_1_1_ID = UUID.fromString("ebf2c5ce-4deb-4cc6-b6f6-61f5d3f6fc98");
    public final static UUID VIEW_COLUMN_1_2_ID = UUID.fromString("d6ba3475-cefa-4771-aaa1-9274f16335ee");
    public final static UUID VIEW_COLUMN_1_3_ID = UUID.fromString("4f189a5f-c9ca-4518-9758-1a0730f6276b");

    public final SubsetDto VIEW_1_SUBSET_DTO = SubsetDto.builder()
            .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_2_ID)))
            .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_2_1_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_2_2_ID).build(),
                    SubsetColumnDto.builder().id(COLUMN_2_3_ID).build())))
            .build();

    public final List<ViewColumnDto> VIEW_1_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_1_1_ID)
                    .ordinalPosition(0)
                    .databaseId(DATABASE_1_ID)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_1_2_ID)
                    .ordinalPosition(1)
                    .databaseId(DATABASE_1_ID)
                    .name("lat")
                    .internalName("lat")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_1_3_ID)
                    .ordinalPosition(2)
                    .databaseId(DATABASE_1_ID)
                    .name("lng")
                    .internalName("lng")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build());

    public final List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn> VIEW_1_COLUMNS_CACHE = List.of(
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_1_1_ID)
                    .internalName("location")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_1_2_ID)
                    .internalName("lat")
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_1_3_ID)
                    .internalName("lng")
                    .columnType(ColumnType.DECIMAL)
                    .build());

    public final View VIEW_1 = View.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .isPublic(VIEW_1_PUBLIC)
            .isSchemaPublic(VIEW_1_SCHEMA_PUBLIC)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .identifiers(new LinkedList<>()) /* IDENTIFIER_3 */
            .columns(null) /* VIEW_1_COLUMNS */
            .database(null) /* DATABASE_1 */
            .build();

    public final static Long VIEW_1_DATA_COUNT = 3L;
    public final List<Map<String, Object>> VIEW_1_DATA_DTO = new LinkedList<>(List.of(
            new HashMap<>() {{
                put("location", "Albury");
                put("lat", -36.0653583);
                put("lng", 146.9112214);
            }},
            new HashMap<>() {{
                put("location", "Sydney");
                put("lat", -33.847927);
                put("lng", 150.6517942);
            }},
            new HashMap<>() {{
                put("location", "Vienna");
                put("lat", null);
                put("lng", null);
            }}
    ));

    public final List<ViewColumn> VIEW_1_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(VIEW_COLUMN_1_1_ID)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .view(VIEW_1)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_1_2_ID)
                    .ordinalPosition(1)
                    .name("lat")
                    .internalName("lat")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_1)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_1_3_ID)
                    .ordinalPosition(2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_1)
                    .build());

    public final ViewDto VIEW_1_DTO = ViewDto.builder()
            .id(VIEW_1_ID)
            .databaseId(DATABASE_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .isPublic(VIEW_1_PUBLIC)
            .isSchemaPublic(VIEW_1_SCHEMA_PUBLIC)
            .identifiers(null /* VIEW_1_DTO_IDENTIFIERS */)
            .owner(USER_1_BRIEF_DTO)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .columns(VIEW_1_COLUMNS_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.View VIEW_1_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.View.builder()
            .id(VIEW_1_ID)
            .internalName(VIEW_1_INTERNAL_NAME)
            .isPublic(VIEW_1_PUBLIC)
            .isSchemaPublic(VIEW_1_SCHEMA_PUBLIC)
            .ownedBy(USER_1_USERNAME)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .columns(VIEW_1_COLUMNS_CACHE)
            .build();

    public final ViewBriefDto VIEW_1_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(DATABASE_1_ID)
            .isPublic(VIEW_1_PUBLIC)
            .isSchemaPublic(VIEW_1_SCHEMA_PUBLIC)
            .ownedBy(USER_1_USERNAME)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .build();

    public final CreateViewDto VIEW_1_CREATE_DTO = CreateViewDto.builder()
            .isPublic(VIEW_1_PUBLIC)
            .name(VIEW_1_NAME)
            .query(VIEW_1_SUBSET_DTO)
            .build();

    @SuppressWarnings("java:S3599")
    public final TableStatisticDto VIEW_1_STATISTIC_DTO = TableStatisticDto.builder()
            .columns(new LinkedList<>(List.of(ColumnStatisticDto.builder()
                            .name("location")
                            .min(BigDecimal.valueOf(6.0))
                            .max(BigDecimal.valueOf(6.0))
                            .mean(BigDecimal.valueOf(6.0))
                            .median(BigDecimal.valueOf(6.0))
                            .stdDev(BigDecimal.valueOf(0.0))
                            .build(),
                    ColumnStatisticDto.builder()
                            .name("lat")
                            .min(BigDecimal.valueOf(-33.847927))
                            .max(BigDecimal.valueOf(-36.0653583))
                            .mean(BigDecimal.valueOf(-34.95664265))
                            .median(BigDecimal.valueOf(-33.847927))
                            .stdDev(BigDecimal.valueOf(1.0))
                            .build(),
                    ColumnStatisticDto.builder()
                            .name("lng")
                            .min(BigDecimal.valueOf(146.9112214))
                            .max(BigDecimal.valueOf(150.6517942))
                            .mean(BigDecimal.valueOf(148.7815078))
                            .median(BigDecimal.valueOf(146.9112214))
                            .stdDev(BigDecimal.valueOf(1.0))
                            .build())))
            .build();

    public final static UUID VIEW_2_ID = UUID.fromString("1921a0a0-e4b0-4d12-a05f-be920af9b5ce");
    public final static Boolean VIEW_2_INITIAL_VIEW = false;
    public final static String VIEW_2_NAME = "JUnit2";
    public final static String VIEW_2_INTERNAL_NAME = "junit2";
    public final static Boolean VIEW_2_PUBLIC = true;
    public final static Boolean VIEW_2_SCHEMA_PUBLIC = true;
    public final static String VIEW_2_QUERY = "select `date`, `location` as loc, `mintemp`, `rainfall` from `weather_aus` where `location` = 'Albury'";
    public final static String VIEW_2_QUERY_HASH = "987fc946772ffb6d85060262dcb5df419692a1f6772ea995e3dedb53c191e984";
    public final static UUID VIEW_COLUMN_2_1_ID = UUID.fromString("8fb30bce-04a8-4e9a-9c6b-0776eda3aab8");
    public final static UUID VIEW_COLUMN_2_2_ID = UUID.fromString("d43f9940-ae27-4d81-b17b-ccbaf578186c");
    public final static UUID VIEW_COLUMN_2_3_ID = UUID.fromString("b47733bb-aeea-414d-811e-405c64463730");
    public final static UUID VIEW_COLUMN_2_4_ID = UUID.fromString("2b467e3a-acef-4944-be19-b4b0680874c2");

    public final List<ViewColumnDto> VIEW_2_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_2_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .name("Date")
                    .internalName("date")
                    .columnType(ColumnTypeDto.DATE)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_2_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .name("loc")
                    .internalName("loc")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_2_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_2_4_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build());

    public final List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn> VIEW_2_COLUMNS_CACHE = List.of(
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_2_1_ID)
                    .internalName("date")
                    .columnType(ColumnType.DATE)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_2_2_ID)
                    .internalName("loc")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_2_3_ID)
                    .internalName("rainfall")
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_2_4_ID)
                    .internalName("mintemp")
                    .columnType(ColumnType.DECIMAL)
                    .build());

    public final View VIEW_2 = View.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .columns(null)  /* VIEW_2_COLUMNS */
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .database(null) /* DATABASE_1 */
            .build();

    public final List<ViewColumn> VIEW_2_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(VIEW_COLUMN_2_1_ID)
                    .ordinalPosition(0)
                    .name("Date")
                    .internalName("date")
                    .columnType(TableColumnType.DATE)
                    .isNullAllowed(true)
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_2_2_ID)
                    .ordinalPosition(1)
                    .name("loc")
                    .internalName("loc")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_2_3_ID)
                    .ordinalPosition(2)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_2_4_ID)
                    .ordinalPosition(3)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_2)
                    .build());

    public final ViewDto VIEW_2_DTO = ViewDto.builder()
            .id(VIEW_2_ID)
            .databaseId(DATABASE_1_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .identifiers(new LinkedList<>())
            .columns(VIEW_2_COLUMNS_DTO)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.View VIEW_2_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.View.builder()
            .id(VIEW_2_ID)
            .internalName(VIEW_2_INTERNAL_NAME)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .columns(VIEW_2_COLUMNS_CACHE)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final ViewBriefDto VIEW_2_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(DATABASE_1_ID)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static UUID VIEW_3_ID = UUID.fromString("88940939-d456-4aae-88a6-f2b6b343c614");
    public final static Boolean VIEW_3_INITIAL_VIEW = false;
    public final static String VIEW_3_NAME = "JUnit3";
    public final static String VIEW_3_INTERNAL_NAME = "junit3";
    public final static Boolean VIEW_3_PUBLIC = true;
    public final static Boolean VIEW_3_SCHEMA_PUBLIC = false;
    public final static String VIEW_3_QUERY = "select w.`mintemp`, w.`rainfall`, w.`location`, m.`date` from `weather_aus` w join `junit2` m on m.`location` = w.`location` and m.`date` = w.`date`";
    public final static String VIEW_3_QUERY_HASH = "bbbaa56a5206b3dc3e6cf9301b0db9344eb6f19b100c7b88550ffb597a0bd255";
    public final static Long VIEW_3_DATA_COUNT = 3L;
    public final static UUID VIEW_COLUMN_3_1_ID = UUID.fromString("129839cb-dbd7-492d-8fd0-ee44a8f51c4d");
    public final static UUID VIEW_COLUMN_3_2_ID = UUID.fromString("e229d80a-c25c-4fbe-8f31-bbb2e1dff3d5");
    public final static UUID VIEW_COLUMN_3_3_ID = UUID.fromString("12083a5d-fdd3-41db-9f92-d1298558e477");
    public final static UUID VIEW_COLUMN_3_4_ID = UUID.fromString("668f8a87-1fa6-4be7-9761-1844aa8315a4");

    public final List<ViewColumnDto> VIEW_3_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_3_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_3_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_3_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .name("Location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(VIEW_COLUMN_3_4_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .name("Date")
                    .internalName("date")
                    .columnType(ColumnTypeDto.DATE)
                    .isNullAllowed(true)
                    .build());

    public final List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn> VIEW_3_COLUMNS_CACHE = List.of(
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_3_1_ID)
                    .internalName("mintemp")
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_3_2_ID)
                    .internalName("rainfall")
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_3_3_ID)
                    .internalName("location")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(VIEW_COLUMN_3_4_ID)
                    .internalName("date")
                    .columnType(ColumnType.DATE)
                    .build());

    public final View VIEW_3 = View.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .columns(null)  /* VIEW_3_COLUMNS */
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .database(null) /* DATABASE_1 */
            .build();

    public final List<ViewColumn> VIEW_3_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(VIEW_COLUMN_3_1_ID)
                    .ordinalPosition(0)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_3)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_3_2_ID)
                    .ordinalPosition(1)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_3)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_3_3_ID)
                    .ordinalPosition(2)
                    .name("Location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .view(VIEW_3)
                    .build(),
            ViewColumn.builder()
                    .id(VIEW_COLUMN_3_4_ID)
                    .ordinalPosition(3)
                    .name("Date")
                    .internalName("date")
                    .columnType(TableColumnType.DATE)
                    .isNullAllowed(true)
                    .view(VIEW_3)
                    .build());

    public final ViewDto VIEW_3_DTO = ViewDto.builder()
            .id(VIEW_3_ID)
            .databaseId(DATABASE_1_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .identifiers(new LinkedList<>())
            .columns(VIEW_3_COLUMNS_DTO)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.View VIEW_3_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.View.builder()
            .id(VIEW_3_ID)
            .internalName(VIEW_3_INTERNAL_NAME)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .columns(VIEW_3_COLUMNS_CACHE)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final ViewBriefDto VIEW_3_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(DATABASE_1_ID)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final static UUID VIEW_4_ID = UUID.fromString("13b36fa0-a65a-4ccf-80b1-5b3a2444a41a");
    public final static Boolean VIEW_4_INITIAL_VIEW = false;
    public final static String VIEW_4_NAME = "Mock View";
    public final static String VIEW_4_INTERNAL_NAME = "mock_view";
    public final Table VIEW_4_TABLE = TABLE_5;
    public final static Boolean VIEW_4_PUBLIC = true;
    public final static Boolean VIEW_4_SCHEMA_PUBLIC = false;
    public final static String VIEW_4_QUERY = "SELECT `animal_name`, `hair`, `feathers`, `eggs`, `milk`, `airborne`, `aquatic`, `predator`, `backbone`, `breathes`, `venomous`, `fins`, `legs`, `tail`, `domestic`, `catsize`, `class_type` FROM `zoo` WHERE `class_type` = 1";
    public final static String VIEW_4_QUERY_HASH = "3561cd0bb0b0e94d6f15ae602134252a5760d09d660a71a4fb015b6991c8ba0b";

    public final List<at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn> VIEW_4_COLUMNS_CACHE = List.of(
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_1_ID)
                    .internalName("animal_name")
                    .columnType(ColumnType.VARCHAR)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_2_ID)
                    .internalName("hair")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_3_ID)
                    .internalName("feathers")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_4_ID)
                    .internalName("eggs")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_5_ID)
                    .internalName("milk")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_6_ID)
                    .internalName("airborne")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_7_ID)
                    .internalName("aquantic")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_8_ID)
                    .internalName("predator")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_9_ID)
                    .internalName("backbone")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_10_ID)
                    .internalName("breathes")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_11_ID)
                    .internalName("venomous")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_12_ID)
                    .internalName("fin")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_13_ID)
                    .internalName("legs")
                    .columnType(ColumnType.INT)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_14_ID)
                    .internalName("tail")
                    .columnType(ColumnType.DECIMAL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_15_ID)
                    .internalName("domestic")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_16_ID)
                    .internalName("catsize")
                    .columnType(ColumnType.BOOL)
                    .build(),
            at.ac.tuwien.ifs.dbrepo.core.entity.cache.ViewColumn.builder()
                    .id(COLUMN_5_17_ID)
                    .internalName("class_type")
                    .columnType(ColumnType.DECIMAL)
                    .build());

    public final List<ViewColumnDto> VIEW_4_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(COLUMN_5_1_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(0)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_2_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(1)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_3_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(2)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_4_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(3)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_5_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(4)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_6_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(5)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_7_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(6)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_8_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(7)
                    .name("Predator")
                    .internalName("predator")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_9_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(8)
                    .name("Backbone")
                    .internalName("backbone")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_10_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(9)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_11_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(10)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_12_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(11)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_13_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(12)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(ColumnTypeDto.INT)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_14_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(13)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_15_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(14)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_16_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(15)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_5_17_ID)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(16)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .build());

    public final View VIEW_4 = View.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .isPublic(VIEW_4_PUBLIC)
            .isSchemaPublic(VIEW_4_SCHEMA_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .columns(null) /* VIEW_4_COLUMNS */
            .build();

    public final ViewDto VIEW_4_DTO = ViewDto.builder()
            .id(VIEW_4_ID)
            .databaseId(DATABASE_2_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .isPublic(VIEW_4_PUBLIC)
            .isSchemaPublic(VIEW_4_SCHEMA_PUBLIC)
            .identifiers(new LinkedList<>())
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .columns(VIEW_4_COLUMNS_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.View VIEW_4_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.View.builder()
            .id(VIEW_4_ID)
            .internalName(VIEW_4_INTERNAL_NAME)
            .isPublic(VIEW_4_PUBLIC)
            .isSchemaPublic(VIEW_4_SCHEMA_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .columns(VIEW_4_COLUMNS_CACHE)
            .build();

    public final ViewBriefDto VIEW_4_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vdbid(DATABASE_2_ID)
            .isPublic(VIEW_4_PUBLIC)
            .isSchemaPublic(VIEW_4_SCHEMA_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .build();

    public final List<ViewColumn> VIEW_4_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(COLUMN_5_1_ID)
                    .ordinalPosition(0)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_2_ID)
                    .ordinalPosition(1)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_3_ID)
                    .ordinalPosition(2)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_4_ID)
                    .ordinalPosition(3)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_5_ID)
                    .ordinalPosition(4)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_6_ID)
                    .ordinalPosition(5)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_7_ID)
                    .ordinalPosition(6)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_8_ID)
                    .ordinalPosition(7)
                    .name("Predator")
                    .internalName("predator")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_9_ID)
                    .ordinalPosition(8)
                    .name("Backbone")
                    .internalName("backbone")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_10_ID)
                    .ordinalPosition(9)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_11_ID)
                    .ordinalPosition(10)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_12_ID)
                    .ordinalPosition(11)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_13_ID)
                    .ordinalPosition(12)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(TableColumnType.INT)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_14_ID)
                    .ordinalPosition(13)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_15_ID)
                    .ordinalPosition(14)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_16_ID)
                    .ordinalPosition(15)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_5_17_ID)
                    .ordinalPosition(16)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build());

    public final static UUID VIEW_5_ID = UUID.fromString("bc6b8507-51f1-4d05-bb0c-1f619a991dec");
    public final static Boolean VIEW_5_INITIAL_VIEW = false;
    public final static String VIEW_5_NAME = "Mock View";
    public final static String VIEW_5_INTERNAL_NAME = "mock_view";
    public final static Boolean VIEW_5_PUBLIC = true;
    public final static Boolean VIEW_5_SCHEMA_PUBLIC = true;
    public final static String VIEW_5_QUERY = "SELECT `location`, `lat`, `lng` FROM `weather_location` WHERE `location` = 'Albury'";
    public final static String VIEW_5_QUERY_HASH = "120f32478aaff874c25ab32eceb9f00b64cc9d422831046f2f5d43953aca01e7";

    public final View VIEW_5 = View.builder()
            .id(VIEW_5_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .isPublic(VIEW_5_PUBLIC)
            .isSchemaPublic(VIEW_5_SCHEMA_PUBLIC)
            .query(VIEW_5_QUERY)
            .queryHash(VIEW_5_QUERY_HASH)
            .ownedBy(USER_1_USERNAME)
            .columns(null)
            .build();

    public final ViewDto VIEW_5_DTO = ViewDto.builder()
            .id(VIEW_5_ID)
            .databaseId(DATABASE_3_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .isPublic(VIEW_5_PUBLIC)
            .isSchemaPublic(VIEW_5_SCHEMA_PUBLIC)
            .identifiers(new LinkedList<>())
            .query(VIEW_5_QUERY)
            .queryHash(VIEW_5_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .columns(new LinkedList<>())
            .build();

    public final ViewBriefDto VIEW_5_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_5_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .vdbid(DATABASE_3_ID)
            .isPublic(VIEW_5_PUBLIC)
            .isSchemaPublic(VIEW_5_SCHEMA_PUBLIC)
            .query(VIEW_5_QUERY)
            .queryHash(VIEW_5_QUERY_HASH)
            .build();

    public final List<ViewColumn> VIEW_5_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(COLUMN_2_1_ID)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .view(VIEW_5)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_2_2_ID)
                    .ordinalPosition(1)
                    .name("lat")
                    .internalName("lat")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_5)
                    .build(),
            ViewColumn.builder()
                    .id(COLUMN_2_3_ID)
                    .ordinalPosition(2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_5)
                    .build());

    public final List<ViewColumnDto> VIEW_5_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(COLUMN_2_1_ID)
                    .databaseId(DATABASE_3_ID)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_2_2_ID)
                    .databaseId(DATABASE_3_ID)
                    .ordinalPosition(1)
                    .name("lat")
                    .internalName("lat")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(COLUMN_2_3_ID)
                    .databaseId(DATABASE_3_ID)
                    .ordinalPosition(2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build());

    public final static Long QUERY_1_RESULT_ID = 1L;
    public final List<Map<String, Object>> QUERY_1_RESULT_DTO = new LinkedList<>(List.of(
            new HashMap<>() {{
                put("location", "Albury");
                put("lat", -36.0653583);
                put("lng", 146.9112214);
            }}, new HashMap<>() {{
                put("location", "Sydney");
                put("lat", -33.847927);
                put("lng", 150.6517942);
            }}));

    public final Map<String, ColumnAnalysisResultDto> QUERY_1_ANALYSIS_MAP_DTO = new HashMap<>() {{
        put("location", ColumnAnalysisResultDto.builder()
                .name("location")
                .datatype(ColumnTypeDto.VARCHAR)
                .build());
        put("lat", ColumnAnalysisResultDto.builder()
                .name("lat")
                .datatype(ColumnTypeDto.DECIMAL)
                .build());
        put("lng", ColumnAnalysisResultDto.builder()
                .name("lng")
                .datatype(ColumnTypeDto.DECIMAL)
                .build());
    }};

    public final static String LICENSE_1_IDENTIFIER = "MIT";
    public final static String LICENSE_1_URI = "https://opensource.org/license/mit/";
    public final License LICENSE_1 = License.builder()
            .identifier(LICENSE_1_IDENTIFIER)
            .uri(LICENSE_1_URI)
            .build();

    public final LicenseDto LICENSE_1_DTO = LicenseDto.builder()
            .identifier(LICENSE_1_IDENTIFIER)
            .uri(LICENSE_1_URI)
            .build();

    public final static UUID CREATOR_1_ID = UUID.fromString("a0417f34-80ff-419f-821d-ce179021484a");
    public final static String CREATOR_1_ORCID = "https://orcid.org/00000-00000-00000";
    public final static String CREATOR_1_AFFIL = "TU Graz";
    public final static String CREATOR_1_AFFIL_ROR = "https://ror.org/04wn28048";
    public final static String CREATOR_1_AFFIL_URI = "https://ror.org/";
    public final static AffiliationIdentifierSchemeType CREATOR_1_AFFIL_TYPE = AffiliationIdentifierSchemeType.ROR;
    public final static AffiliationIdentifierSchemeTypeDto CREATOR_1_AFFIL_TYPE_DTO = AffiliationIdentifierSchemeTypeDto.ROR;
    public final static String CREATOR_1_FIRSTNAME = "Max";
    public final static String CREATOR_1_LASTNAME = "Mustermann";
    public final static String CREATOR_1_NAME = CREATOR_1_LASTNAME + ", " + CREATOR_1_FIRSTNAME;

    public final OrcidDto ORCID_1_DTO = OrcidDto.builder()
            .person(OrcidPersonDto.builder()
                    .name(OrcidNameDto.builder()
                            .givenNames(OrcidValueDto.builder()
                                    .value(USER_1_FIRSTNAME)
                                    .build())
                            .familyName(OrcidValueDto.builder()
                                    .value(USER_1_LASTNAME)
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
                                                                            .name(USER_1_AFFILIATION)
                                                                            .build())
                                                                    .build())
                                                            .build()
                                            })
                                            .build()
                            })
                            .build())
                    .build())
            .build();

    public final static UUID CREATOR_2_ID = UUID.fromString("56b70dae-17a7-4f76-9c1e-a493762ba760");
    public final static Long CREATOR_2_QUERY_ID = 1L;
    public final static String CREATOR_2_ORCID = "https://orcid.org/00000-00000-00000";
    public final static String CREATOR_2_AFFIL = "TU Wien";
    public final static String CREATOR_2_FIRSTNAME = "Martina";
    public final static String CREATOR_2_LASTNAME = "Mustermann";
    public final static String CREATOR_2_NAME = CREATOR_2_LASTNAME + ", " + CREATOR_2_FIRSTNAME;

    public final static UUID CREATOR_3_ID = UUID.fromString("a2dfea46-7d88-4069-9b93-2417e1fb578b");
    public final static Long CREATOR_3_QUERY_ID = 1L;
    public final static String CREATOR_3_ORCID = "https://orcid.org/00000-00000-00000";
    public final static String CREATOR_3_AFFIL = "TU Graz";
    public final static String CREATOR_3_AFFIL_ROR = "https://ror.org/04wn28048";
    public final static AffiliationIdentifierSchemeType CREATOR_3_AFFIL_SCHEME_TYPE = AffiliationIdentifierSchemeType.ROR;
    public final static AffiliationIdentifierSchemeTypeDto CREATOR_3_AFFIL_SCHEME_TYPE_DTO = AffiliationIdentifierSchemeTypeDto.ROR;
    public final static String CREATOR_3_AFFIL_URI = "https://ror.org/";
    public final static String CREATOR_3_FIRSTNAME = "Max";
    public final static String CREATOR_3_LASTNAME = "Mustermann";
    public final static String CREATOR_3_NAME = CREATOR_3_LASTNAME + ", " + CREATOR_3_FIRSTNAME;

    public final static UUID CREATOR_4_ID = UUID.fromString("473489fa-ad02-4e48-856f-5a3f83ff541d");
    public final static Long CREATOR_4_QUERY_ID = 1L;
    public final static String CREATOR_4_ORCID = "00000-00000-00000";
    public final static String CREATOR_4_AFFIL = "TU Wien";
    public final static String CREATOR_4_AFFIL_ROR = "https://ror.org/04d836q62";
    public final static String CREATOR_4_AFFIL_URI = "https://ror.org/";
    public final static AffiliationIdentifierSchemeType CREATOR_4_AFFIL_TYPE = AffiliationIdentifierSchemeType.ROR;
    public final static AffiliationIdentifierSchemeTypeDto CREATOR_4_AFFIL_TYPE_DTO = AffiliationIdentifierSchemeTypeDto.ROR;
    public final static String CREATOR_4_FIRSTNAME = "Martina";
    public final static String CREATOR_4_LASTNAME = "Mustermann";
    public final static String CREATOR_4_NAME = CREATOR_4_LASTNAME + ", " + CREATOR_4_FIRSTNAME;

    public final static UUID IDENTIFIER_1_ID = UUID.fromString("679a83f2-ef23-4b4b-98f7-ad77b9d68733");
    public final static String IDENTIFIER_1_DOI = "10.12345/183";
    public final static Instant IDENTIFIER_1_CREATED = Instant.ofEpochSecond(1641588352L) /* 2022-01-07 20:45:52 */;
    public final static Instant IDENTIFIER_1_MODIFIED = Instant.ofEpochSecond(1541588352L) /* 2022-01-07 20:45:52 */;
    public final static Instant IDENTIFIER_1_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2022-01-07 20:45:52 */;
    public final static Integer IDENTIFIER_1_PUBLICATION_MONTH = 5;
    public final static Integer IDENTIFIER_1_PUBLICATION_YEAR = 2022;
    public final static Integer IDENTIFIER_1_PUBLICATION_DAY = null;
    public final static String IDENTIFIER_1_PUBLISHER = "Austrian Government";
    public final static IdentifierType IDENTIFIER_1_TYPE = IdentifierType.DATABASE;
    public final static IdentifierTypeDto IDENTIFIER_1_TYPE_DTO = IdentifierTypeDto.DATABASE;
    public final static IdentifierStatusType IDENTIFIER_1_STATUS_TYPE = IdentifierStatusType.DRAFT;
    public final static IdentifierStatusTypeDto IDENTIFIER_1_STATUS_TYPE_DTO = IdentifierStatusTypeDto.DRAFT;

    public final static UUID IDENTIFIER_1_TITLE_1_ID = UUID.fromString("3df6b286-9bd2-4ae3-b8f4-29c217544bef");
    public final static String IDENTIFIER_1_TITLE_1_TITLE = "Austrian weather data";
    public final static Integer IDENTIFIER_1_TITLE_1_ORD_POS = 0;
    public final static String IDENTIFIER_1_TITLE_1_TITLE_MODIFY = "Austrian weather some data";
    public final static TitleType IDENTIFIER_1_TITLE_1_TYPE = null;
    public final static TitleTypeDto IDENTIFIER_1_TITLE_1_TYPE_DTO = null;
    public final static LanguageType IDENTIFIER_1_TITLE_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_1_TITLE_1_LANG_DTO = LanguageTypeDto.EN;

    public final IdentifierTitle IDENTIFIER_1_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_1_TITLE_1_ID)
            .ordinalPosition(IDENTIFIER_1_TITLE_1_ORD_POS)
            .title(IDENTIFIER_1_TITLE_1_TITLE)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE)
            .language(IDENTIFIER_1_TITLE_1_LANG)
            .build();

    public final IdentifierTitleDto IDENTIFIER_1_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_1_ID)
            .title(IDENTIFIER_1_TITLE_1_TITLE)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final IdentifierTitleDto IDENTIFIER_1_TITLE_1_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_1_ID)
            .title(IDENTIFIER_1_TITLE_1_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final CreateIdentifierTitleDto IDENTIFIER_1_TITLE_1_CREATE_DTO = CreateIdentifierTitleDto.builder()
            .title(IDENTIFIER_1_TITLE_1_TITLE)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_1_TITLE_1_SAVE_DTO = SaveIdentifierTitleDto.builder()
            .id(null)
            .title(IDENTIFIER_1_TITLE_1_TITLE)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_1_TITLE_1_UPDATE_DTO = SaveIdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_1_ID)
            .title(IDENTIFIER_1_TITLE_1_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final static UUID IDENTIFIER_1_TITLE_2_ID = UUID.fromString("903a7e5b-8014-4b8a-b8fd-44f477880905");
    public final static String IDENTIFIER_1_TITLE_2_TITLE = "Österreichische Wetterdaten";
    public final static String IDENTIFIER_1_TITLE_2_TITLE_MODIFY = "Österreichische Wetterdaten übersetzt";
    public final static TitleType IDENTIFIER_1_TITLE_2_TYPE = TitleType.TRANSLATED_TITLE;
    public final static Integer IDENTIFIER_1_TITLE_2_ORD_POS = 1;
    public final static TitleTypeDto IDENTIFIER_1_TITLE_2_TYPE_DTO = TitleTypeDto.TRANSLATED_TITLE;
    public final static LanguageType IDENTIFIER_1_TITLE_2_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_1_TITLE_2_LANG_DTO = LanguageTypeDto.EN;

    public final IdentifierTitle IDENTIFIER_1_TITLE_2 = IdentifierTitle.builder()
            .id(IDENTIFIER_1_TITLE_2_ID)
            .ordinalPosition(IDENTIFIER_1_TITLE_2_ORD_POS)
            .title(IDENTIFIER_1_TITLE_2_TITLE)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE)
            .language(IDENTIFIER_1_TITLE_2_LANG)
            .build();

    public final IdentifierTitleDto IDENTIFIER_1_TITLE_2_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_2_ID)
            .title(IDENTIFIER_1_TITLE_2_TITLE)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final IdentifierTitleDto IDENTIFIER_1_TITLE_2_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_2_ID)
            .title(IDENTIFIER_1_TITLE_2_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final CreateIdentifierTitleDto IDENTIFIER_1_TITLE_2_CREATE_DTO = CreateIdentifierTitleDto.builder()
            .title(IDENTIFIER_1_TITLE_2_TITLE)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_1_TITLE_2_SAVE_DTO = SaveIdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_2_ID)
            .title(IDENTIFIER_1_TITLE_2_TITLE)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_1_TITLE_2_UPDATE_DTO = SaveIdentifierTitleDto.builder()
            .title(IDENTIFIER_1_TITLE_2_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final static UUID IDENTIFIER_1_DESCRIPTION_1_ID = UUID.fromString("1c438756-93f0-4797-983c-175a17e18c2c");
    public final static String IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION = "Selecting all from the weather Austrian table";
    public final static String IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION_MODIFY = "Selecting some from the weather Austrian table";
    public final static DescriptionType IDENTIFIER_1_DESCRIPTION_1_TYPE = null;
    public final static Integer IDENTIFIER_1_DESCRIPTION_1_ORD_POS = 0;
    public final static DescriptionTypeDto IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO = null;
    public final static LanguageType IDENTIFIER_1_DESCRIPTION_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_1_DESCRIPTION_1_LANG_DTO = LanguageTypeDto.EN;

    public final IdentifierDescription IDENTIFIER_1_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_1_DESCRIPTION_1_ID)
            .ordinalPosition(IDENTIFIER_1_DESCRIPTION_1_ORD_POS)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_1_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_1_DESCRIPTION_1_ID)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_1_DESCRIPTION_1_DTO_MODIFY = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_1_DESCRIPTION_1_ID)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION_MODIFY)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(null)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final CreateIdentifierDescriptionDto IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO = CreateIdentifierDescriptionDto.builder()
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static UUID IDENTIFIER_1_CREATOR_1_ID = UUID.fromString("667cd1d6-4f94-4808-b5cb-12e5ec0788d8");
    public final static Integer IDENTIFIER_1_CREATOR_1_ORD_POS = 0;
    public final static String IDENTIFIER_1_CREATOR_1_FIRSTNAME = CREATOR_1_FIRSTNAME;
    public final static String IDENTIFIER_1_CREATOR_1_LASTNAME = CREATOR_1_LASTNAME;
    public final static String IDENTIFIER_1_CREATOR_1_NAME = CREATOR_1_NAME;
    public final static NameType IDENTIFIER_1_CREATOR_1_NAME_TYPE = NameType.PERSONAL;
    public final static String IDENTIFIER_1_CREATOR_1_ORCID = CREATOR_1_ORCID;
    public final static String IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_URI = "https://orcid.org/";
    public final NameIdentifierSchemeType IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE = NameIdentifierSchemeType.ORCID;
    public final NameIdentifierSchemeTypeDto IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO = NameIdentifierSchemeTypeDto.ORCID;
    public final static String IDENTIFIER_1_CREATOR_1_AFFILIATION = CREATOR_1_AFFIL;
    public final static String IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER = CREATOR_1_AFFIL_ROR;
    public final static AffiliationIdentifierSchemeType IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME = CREATOR_1_AFFIL_TYPE;
    public final static AffiliationIdentifierSchemeTypeDto IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_DTO = CREATOR_1_AFFIL_TYPE_DTO;
    public final static String IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI = CREATOR_1_AFFIL_URI;

    public final Creator IDENTIFIER_1_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_1_CREATOR_1_ID)
            .ordinalPosition(IDENTIFIER_1_CREATOR_1_ORD_POS)
            .firstname(IDENTIFIER_1_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_1_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_1_CREATOR_1_NAME)
            .nameType(IDENTIFIER_1_CREATOR_1_NAME_TYPE)
            .nameIdentifier(IDENTIFIER_1_CREATOR_1_ORCID)
            .nameIdentifierScheme(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .affiliationIdentifier(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER)
            .affiliationIdentifierScheme(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME)
            .affiliationIdentifierSchemeUri(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI)
            .build();

    public final CreatorDto IDENTIFIER_1_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_1_CREATOR_1_ID)
            .firstname(IDENTIFIER_1_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_1_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_1_CREATOR_1_NAME)
            .nameType(NameTypeDto.PERSONAL)
            .nameIdentifier(IDENTIFIER_1_CREATOR_1_ORCID)
            .nameIdentifierScheme(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .affiliationIdentifier(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER)
            .affiliationIdentifierScheme(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_DTO)
            .affiliationIdentifierSchemeUri(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI)
            .build();

    public final CreateIdentifierCreatorDto IDENTIFIER_1_CREATOR_1_CREATE_DTO = CreateIdentifierCreatorDto.builder()
            .firstname(IDENTIFIER_1_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_1_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_1_CREATOR_1_NAME)
            .nameType(NameTypeDto.PERSONAL)
            .nameIdentifier(IDENTIFIER_1_CREATOR_1_ORCID)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .affiliationIdentifier(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_1_CREATOR_1_SAVE_DTO = SaveIdentifierCreatorDto.builder()
            .id(IDENTIFIER_1_CREATOR_1_ID)
            .firstname(IDENTIFIER_1_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_1_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_1_CREATOR_1_NAME)
            .nameType(NameTypeDto.PERSONAL)
            .nameIdentifier(IDENTIFIER_1_CREATOR_1_ORCID)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .affiliationIdentifier(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER)
            .build();

    public final static UUID IDENTIFIER_1_FUNDER_1_ID = UUID.fromString("8deb273d-6dd6-407d-970a-01534035ac01");
    public final static String IDENTIFIER_1_FUNDER_1_NAME = "European Commission";
    public final static String IDENTIFIER_1_FUNDER_1_IDENTIFIER = "https://doi.org/10.13039/501100000780";
    public final static String IDENTIFIER_1_FUNDER_1_IDENTIFIER_ID_ONLY = "10.13039/501100000780";
    public final static Integer IDENTIFIER_1_FUNDER_1_ORD_POS = 0;
    public final IdentifierFunderType IDENTIFIER_1_FUNDER_1_IDENTIFIER_TYPE = IdentifierFunderType.CROSSREF_FUNDER_ID;
    public final IdentifierFunderTypeDto IDENTIFIER_1_FUNDER_1_IDENTIFIER_TYPE_DTO = IdentifierFunderTypeDto.CROSSREF_FUNDER_ID;
    public final static String IDENTIFIER_1_FUNDER_1_AWARD_TITLE = "Institutionalizing global genetic-resource commons. Global Strategies for accessing and using essential public knowledge assets in the life science";

    public final IdentifierFunder IDENTIFIER_1_FUNDER_1 = IdentifierFunder.builder()
            .id(IDENTIFIER_1_FUNDER_1_ID)
            .ordinalPosition(IDENTIFIER_1_FUNDER_1_ORD_POS)
            .funderName(IDENTIFIER_1_FUNDER_1_NAME)
            .funderIdentifier(IDENTIFIER_1_FUNDER_1_IDENTIFIER)
            .funderIdentifierType(IDENTIFIER_1_FUNDER_1_IDENTIFIER_TYPE)
            .awardTitle(IDENTIFIER_1_FUNDER_1_AWARD_TITLE)
            .build();

    public final IdentifierFunderDto IDENTIFIER_1_FUNDER_1_DTO = IdentifierFunderDto.builder()
            .id(IDENTIFIER_1_FUNDER_1_ID)
            .funderName(IDENTIFIER_1_FUNDER_1_NAME)
            .funderIdentifier(IDENTIFIER_1_FUNDER_1_IDENTIFIER)
            .funderIdentifierType(IDENTIFIER_1_FUNDER_1_IDENTIFIER_TYPE_DTO)
            .awardTitle(IDENTIFIER_1_FUNDER_1_AWARD_TITLE)
            .build();

    public final CreateIdentifierFunderDto IDENTIFIER_1_FUNDER_1_CREATE_DTO = CreateIdentifierFunderDto.builder()
            .funderName(IDENTIFIER_1_FUNDER_1_NAME)
            .funderIdentifier(IDENTIFIER_1_FUNDER_1_IDENTIFIER)
            .funderIdentifierType(IDENTIFIER_1_FUNDER_1_IDENTIFIER_TYPE_DTO)
            .awardTitle(IDENTIFIER_1_FUNDER_1_AWARD_TITLE)
            .build();

    public final SaveIdentifierFunderDto IDENTIFIER_1_FUNDER_1_SAVE_DTO = SaveIdentifierFunderDto.builder()
            .id(IDENTIFIER_1_FUNDER_1_ID)
            .funderName(IDENTIFIER_1_FUNDER_1_NAME)
            .funderIdentifier(IDENTIFIER_1_FUNDER_1_IDENTIFIER)
            .funderIdentifierType(IDENTIFIER_1_FUNDER_1_IDENTIFIER_TYPE_DTO)
            .awardTitle(IDENTIFIER_1_FUNDER_1_AWARD_TITLE)
            .build();

    public final DataCiteBody<DataCiteDoi> IDENTIFIER_1_DATA_CITE = DataCiteBody.<DataCiteDoi>builder()
            .data(DataCiteData.<DataCiteDoi>builder()
                    .type("dois")
                    .attributes(DataCiteDoi.builder()
                            .doi(IDENTIFIER_1_DOI)
                            .build())
                    .build())
            .build();

    public final Identifier IDENTIFIER_1 = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1, IDENTIFIER_1_TITLE_2)))
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1)))
            .doi(IDENTIFIER_1_DOI)
            .database(null /* DATABASE_1 */)
            .created(IDENTIFIER_1_CREATED)
            .lastModified(IDENTIFIER_1_MODIFIED)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE)
            .ownedBy(USER_1_USERNAME)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1)))
            .relatedIdentifiers(new LinkedList<>())
            .status(IDENTIFIER_1_STATUS_TYPE)
            .build();

    public final IdentifierDto IDENTIFIER_1_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .links(LinksDto.builder()
                    .self("/api/v1/identifier/" + IDENTIFIER_1_ID)
                    .selfHtml("/pid/" + IDENTIFIER_1_ID)
                    .build())
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_DTO, IDENTIFIER_1_TITLE_2_DTO)))
            .doi(IDENTIFIER_1_DOI)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .owner(USER_1_BRIEF_DTO)
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_DTO)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_DTO)))
            .status(IDENTIFIER_1_STATUS_TYPE_DTO)
            .build();

    public final IdentifierBriefDto IDENTIFIER_1_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_DTO, IDENTIFIER_1_TITLE_2_DTO)))
            .doi(IDENTIFIER_1_DOI)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .status(IDENTIFIER_1_STATUS_TYPE_DTO)
            .build();

    public final CreateIdentifierDto IDENTIFIER_1_CREATE_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_1_ID)
            .type(IDENTIFIER_1_TYPE_DTO)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO, IDENTIFIER_1_TITLE_2_CREATE_DTO)))
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .doi(IDENTIFIER_1_DOI)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_CREATE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .build();

    public final CreateIdentifierDto IDENTIFIER_1_CREATE_WITH_DOI_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_1_ID)
            .type(IDENTIFIER_1_TYPE_DTO)
            .doi(IDENTIFIER_1_DOI)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO, IDENTIFIER_1_TITLE_2_CREATE_DTO)))
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_CREATE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .build();

    public final IdentifierSaveDto IDENTIFIER_1_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO, IDENTIFIER_1_TITLE_2_SAVE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_SAVE_DTO)))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .doi(IDENTIFIER_1_DOI)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .build();

    public final IdentifierSaveDto IDENTIFIER_1_SAVE_MODIFY_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .descriptions(new LinkedList<>(List.of())) // <<<
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))) // <<<
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of())) // <<<
            .funders(new LinkedList<>(List.of())) // <<<
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .licenses(new LinkedList<>(List.of())) // <<<
            .build();

    public final static UUID IDENTIFIER_5_ID = UUID.fromString("e05bb4c9-ed26-48c9-bd91-5c48a93a04bd");
    public final static String IDENTIFIER_5_DOI = "10.12345/13/50BBFCFE08A12";
    public final static Instant IDENTIFIER_5_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant IDENTIFIER_5_MODIFIED = Instant.ofEpochSecond(1541588352L);
    public final static Instant IDENTIFIER_5_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2018-11-07 10:59:12 */;
    public final static Integer IDENTIFIER_5_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_5_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_5_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_5_QUERY_HASH = QUERY_2_QUERY_HASH;
    public final static String IDENTIFIER_5_RESULT_HASH = QUERY_2_RESULT_HASH;
    public final static String IDENTIFIER_5_QUERY = QUERY_2_STATEMENT;
    public final static String IDENTIFIER_5_NORMALIZED = QUERY_2_STATEMENT_NORMALIZED;
    public final static Long IDENTIFIER_5_RESULT_NUMBER = QUERY_2_RESULT_NUMBER;
    public final static String IDENTIFIER_5_PUBLISHER = "Australian Government";
    public final static IdentifierType IDENTIFIER_5_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_5_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static IdentifierStatusType IDENTIFIER_5_STATUS_TYPE = IdentifierStatusType.DRAFT;
    public final static IdentifierStatusTypeDto IDENTIFIER_5_STATUS_TYPE_DTO = IdentifierStatusTypeDto.DRAFT;

    public final static UUID IDENTIFIER_5_TITLE_1_ID = UUID.fromString("1a0ae9c2-61c6-44f8-b886-26a4f4dabc52");
    public final static String IDENTIFIER_5_TITLE_1_TITLE = "Australische Wetterdaten";
    public final static LanguageType IDENTIFIER_5_TITLE_1_LANG = LanguageType.DE;
    public final static LanguageTypeDto IDENTIFIER_5_TITLE_1_LANG_DTO = LanguageTypeDto.DE;
    public final static TitleType IDENTIFIER_5_TITLE_1_TYPE = TitleType.SUBTITLE;
    public final static Integer IDENTIFIER_5_TITLE_1_ORD_POS = 0;
    public final static TitleTypeDto IDENTIFIER_5_TITLE_1_TYPE_DTO = TitleTypeDto.SUBTITLE;

    public final IdentifierTitle IDENTIFIER_5_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_5_TITLE_1_ID)
            .ordinalPosition(IDENTIFIER_5_TITLE_1_ORD_POS)
            .title(IDENTIFIER_5_TITLE_1_TITLE)
            .language(IDENTIFIER_5_TITLE_1_LANG)
            .titleType(IDENTIFIER_5_TITLE_1_TYPE)
            .build();

    public final IdentifierTitleDto IDENTIFIER_5_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_5_TITLE_1_ID)
            .title(IDENTIFIER_5_TITLE_1_TITLE)
            .language(IDENTIFIER_5_TITLE_1_LANG_DTO)
            .titleType(IDENTIFIER_5_TITLE_1_TYPE_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_5_TITLE_1_CREATE_DTO = SaveIdentifierTitleDto.builder()
            .title(IDENTIFIER_5_TITLE_1_TITLE)
            .language(IDENTIFIER_5_TITLE_1_LANG_DTO)
            .titleType(IDENTIFIER_5_TITLE_1_TYPE_DTO)
            .build();

    public final static UUID IDENTIFIER_5_DESCRIPTION_1_ID = UUID.fromString("ab49bdca-f373-4823-9947-2a0cbfa88350");
    public final static String IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION = "Alle Wetterdaten in Australien";
    public final static LanguageType IDENTIFIER_5_DESCRIPTION_1_LANG = LanguageType.DE;
    public final static Integer IDENTIFIER_5_DESCRIPTION_1_ORD_POS = 0;
    public final static LanguageTypeDto IDENTIFIER_5_DESCRIPTION_1_LANG_DTO = LanguageTypeDto.DE;
    public final static DescriptionType IDENTIFIER_5_DESCRIPTION_1_TYPE = DescriptionType.ABSTRACT;
    public final static DescriptionTypeDto IDENTIFIER_5_DESCRIPTION_1_TYPE_DTO = DescriptionTypeDto.ABSTRACT;

    public final IdentifierDescription IDENTIFIER_5_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_5_DESCRIPTION_1_ID)
            .ordinalPosition(IDENTIFIER_5_DESCRIPTION_1_ORD_POS)
            .description(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_5_DESCRIPTION_1_LANG)
            .descriptionType(IDENTIFIER_5_DESCRIPTION_1_TYPE)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_5_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_5_DESCRIPTION_1_ID)
            .description(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_5_DESCRIPTION_1_LANG_DTO)
            .descriptionType(IDENTIFIER_5_DESCRIPTION_1_TYPE_DTO)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_5_DESCRIPTION_1_CREATE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(null)
            .description(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_5_DESCRIPTION_1_LANG_DTO)
            .descriptionType(IDENTIFIER_5_DESCRIPTION_1_TYPE_DTO)
            .build();

    public final static UUID IDENTIFIER_5_CREATOR_1_ID = UUID.fromString("6844b684-93e4-47d2-a615-5939127fdafe");
    public final static Integer IDENTIFIER_5_CREATOR_1_ORD_POS = 0;
    public final static String IDENTIFIER_5_CREATOR_1_FIRSTNAME = "Max";
    public final static String IDENTIFIER_5_CREATOR_1_LASTNAME = "Mustermann";
    public final static String IDENTIFIER_5_CREATOR_1_NAME = IDENTIFIER_5_CREATOR_1_LASTNAME + ", " + IDENTIFIER_5_CREATOR_1_FIRSTNAME;
    public final static String IDENTIFIER_5_CREATOR_1_AFFIL = "TU Graz";
    public final static String IDENTIFIER_5_CREATOR_1_AFFIL_ROR = "https://ror.org/04wn28048";
    public final static String IDENTIFIER_5_CREATOR_1_AFFIL_URI = "https://ror.org/";
    public final static AffiliationIdentifierSchemeType IDENTIFIER_5_CREATOR_1_AFFIL_SCHEME = AffiliationIdentifierSchemeType.ROR;
    public final static String IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER = "https://orcid.org/00000-00000-00000";
    public final static NameIdentifierSchemeType IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE = NameIdentifierSchemeType.ORCID;
    public final static NameIdentifierSchemeTypeDto IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE_DTO = NameIdentifierSchemeTypeDto.ORCID;
    public final static NameType IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_TYPE = NameType.PERSONAL;
    public final static NameTypeDto IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_TYPE_DTO = NameTypeDto.PERSONAL;
    public final static String IDENTIFIER_5_CREATOR_1_IDENTIFIER_SCHEME_URI = "https://orcid.org/";

    public final Creator IDENTIFIER_5_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_5_CREATOR_1_ID)
            .ordinalPosition(IDENTIFIER_5_CREATOR_1_ORD_POS)
            .firstname(IDENTIFIER_5_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_1_NAME)
            .nameType(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_TYPE)
            .nameIdentifier(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_5_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_5_CREATOR_1_AFFIL)
            .identifier(null /* IDENTIFIER_5 */)
            .build();

    public final CreatorDto IDENTIFIER_5_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_5_CREATOR_1_ID)
            .firstname(IDENTIFIER_5_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_1_NAME)
            .nameType(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_5_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_5_CREATOR_1_AFFIL)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_5_CREATOR_1_CREATE_DTO = SaveIdentifierCreatorDto.builder()
            .firstname(IDENTIFIER_5_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_1_NAME)
            .nameIdentifier(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER)
            .nameType(IDENTIFIER_5_CREATOR_1_NAME_IDENTIFIER_TYPE_DTO)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_5_CREATOR_1_MODIFY_DTO = SaveIdentifierCreatorDto.builder()
            .firstname(IDENTIFIER_5_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_1_NAME)
            .affiliation(null) // <<<
            .build();

    public final static UUID IDENTIFIER_5_CREATOR_2_ID = UUID.fromString("14943ad6-a935-49f5-b07e-f9eb789b8604");
    public final static Integer IDENTIFIER_5_CREATOR_2_ORD_POS = 1;
    public final static String IDENTIFIER_5_CREATOR_2_AFFIL = "TU Wien";
    public final static String IDENTIFIER_5_CREATOR_2_FIRSTNAME = "Martina";
    public final static String IDENTIFIER_5_CREATOR_2_LASTNAME = "Mustermann";
    public final static String IDENTIFIER_5_CREATOR_2_NAME = IDENTIFIER_5_CREATOR_2_LASTNAME + ", " + IDENTIFIER_5_CREATOR_2_FIRSTNAME;
    public final static String IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER = "https://orcid.org/00000-00000-00000";
    public final static NameIdentifierSchemeType IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE = NameIdentifierSchemeType.ORCID;
    public final static NameIdentifierSchemeTypeDto IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE_DTO = NameIdentifierSchemeTypeDto.ORCID;
    public final static NameType IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_TYPE = NameType.PERSONAL;
    public final static NameTypeDto IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_TYPE_DTO = NameTypeDto.PERSONAL;
    public final static String IDENTIFIER_5_CREATOR_2_IDENTIFIER_SCHEME_URI = "https://orcid.org/";

    public final Creator IDENTIFIER_5_CREATOR_2 = Creator.builder()
            .id(IDENTIFIER_5_CREATOR_2_ID)
            .ordinalPosition(IDENTIFIER_5_CREATOR_2_ORD_POS)
            .firstname(IDENTIFIER_5_CREATOR_2_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_2_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_2_NAME)
            .nameType(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_TYPE)
            .nameIdentifier(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_5_CREATOR_2_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_5_CREATOR_2_AFFIL)
            .identifier(null /* IDENTIFIER_5 */)
            .build();

    public final CreatorDto IDENTIFIER_5_CREATOR_2_DTO = CreatorDto.builder()
            .id(IDENTIFIER_5_CREATOR_2_ID)
            .firstname(IDENTIFIER_5_CREATOR_2_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_2_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_2_NAME)
            .nameType(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_5_CREATOR_2_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_5_CREATOR_2_AFFIL)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_5_CREATOR_2_CREATE_DTO = SaveIdentifierCreatorDto.builder()
            .firstname(IDENTIFIER_5_CREATOR_2_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_2_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_2_NAME)
            .nameType(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER)
            .affiliation(IDENTIFIER_5_CREATOR_2_AFFIL)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_5_CREATOR_2_MODIFY_DTO = SaveIdentifierCreatorDto.builder()
            .firstname(IDENTIFIER_5_CREATOR_2_FIRSTNAME)
            .lastname(IDENTIFIER_5_CREATOR_2_LASTNAME)
            .creatorName(IDENTIFIER_5_CREATOR_2_NAME)
            .nameType(IDENTIFIER_5_CREATOR_2_NAME_IDENTIFIER_TYPE_DTO)
            .nameIdentifier(null) /* <<<< */
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final Identifier IDENTIFIER_5 = Identifier.builder()
            .id(IDENTIFIER_5_ID)
            .queryId(QUERY_2_ID)
            .database(null) /* DATABASE_2 */
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_5_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_5_TITLE_1)))
            .doi(IDENTIFIER_5_DOI)
            .created(IDENTIFIER_5_CREATED)
            .lastModified(IDENTIFIER_5_MODIFIED)
            .execution(IDENTIFIER_5_EXECUTION)
            .publicationDay(IDENTIFIER_5_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_5_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_5_QUERY_HASH)
            .resultHash(IDENTIFIER_5_RESULT_HASH)
            .query(IDENTIFIER_5_QUERY)
            .queryNormalized(IDENTIFIER_5_NORMALIZED)
            .resultNumber(IDENTIFIER_5_RESULT_NUMBER)
            .publisher(IDENTIFIER_5_PUBLISHER)
            .type(IDENTIFIER_5_TYPE)
            .ownedBy(USER_2_USERNAME)
            .creators(new LinkedList<>(List.of(IDENTIFIER_5_CREATOR_1, IDENTIFIER_5_CREATOR_2)))
            .status(IDENTIFIER_5_STATUS_TYPE)
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .relatedIdentifiers(new LinkedList<>())
            .build();

    public final IdentifierDto IDENTIFIER_5_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_5_ID)
            .databaseId(DATABASE_2_ID)
            .queryId(QUERY_2_ID)
            .links(LinksDto.builder()
                    .self("/api/v1/identifier/" + IDENTIFIER_5_ID)
                    .selfHtml("/pid/" + IDENTIFIER_5_ID)
                    .data("/api/v1/database/" + DATABASE_2_ID + "/subset/" + QUERY_2_ID + "/data")
                    .dashboardHtml("/d/" + DATABASE_2_DASHBOARD_UID)
                    .build())
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_5_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_5_TITLE_1_DTO)))
            .doi(IDENTIFIER_5_DOI)
            .execution(IDENTIFIER_5_EXECUTION)
            .publicationDay(IDENTIFIER_5_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_5_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_5_QUERY_HASH)
            .resultHash(IDENTIFIER_5_RESULT_HASH)
            .query(IDENTIFIER_5_QUERY)
            .queryNormalized(IDENTIFIER_5_NORMALIZED)
            .resultNumber(IDENTIFIER_5_RESULT_NUMBER)
            .publisher(IDENTIFIER_5_PUBLISHER)
            .type(IDENTIFIER_5_TYPE_DTO)
            .owner(USER_2_BRIEF_DTO)
            .status(IDENTIFIER_5_STATUS_TYPE_DTO)
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_5_CREATOR_1_DTO, IDENTIFIER_5_CREATOR_2_DTO)))
            .build();

    public final IdentifierBriefDto IDENTIFIER_5_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_5_ID)
            .databaseId(DATABASE_2_ID)
            .queryId(QUERY_2_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_5_TITLE_1_DTO)))
            .doi(IDENTIFIER_5_DOI)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_5_PUBLISHER)
            .type(IDENTIFIER_5_TYPE_DTO)
            .build();

    public final static UUID RELATED_IDENTIFIER_5_ID = UUID.fromString("26545877-574d-44fa-819d-d9d9a9750b38");
    public final static String RELATED_IDENTIFIER_5_VALUE = "10.5281/zenodo.6637333";
    public final RelatedType RELATED_IDENTIFIER_5_TYPE = RelatedType.DOI;
    public final RelatedTypeDto RELATED_IDENTIFIER_5_TYPE_DTO = RelatedTypeDto.DOI;
    public final RelationType RELATED_IDENTIFIER_5_RELATION_TYPE = RelationType.CITES;
    public final RelationTypeDto RELATED_IDENTIFIER_5_RELATION_TYPE_DTO = RelationTypeDto.CITES;

    public final RelatedIdentifier IDENTIFIER_1_RELATED_IDENTIFIER_1 = RelatedIdentifier.builder()
            .id(RELATED_IDENTIFIER_5_ID)
            .identifier(IDENTIFIER_5)
            .type(RELATED_IDENTIFIER_5_TYPE)
            .relation(RELATED_IDENTIFIER_5_RELATION_TYPE)
            .value(RELATED_IDENTIFIER_5_VALUE)
            .build();

    public final RelatedIdentifierDto IDENTIFIER_1_RELATED_IDENTIFIER_1_DTO = RelatedIdentifierDto.builder()
            .id(RELATED_IDENTIFIER_5_ID)
            .type(RELATED_IDENTIFIER_5_TYPE_DTO)
            .relation(RELATED_IDENTIFIER_5_RELATION_TYPE_DTO)
            .value(RELATED_IDENTIFIER_5_VALUE)
            .build();

    public final DataCiteDoiRelatedIdentifier IDENTIFIER_1_DATACITE_RELATED_IDENTIFIER_1_DTO = DataCiteDoiRelatedIdentifier.builder()
            .relatedIdentifierType(RELATED_IDENTIFIER_5_TYPE.toString())
            .relationType(RELATED_IDENTIFIER_5_RELATION_TYPE.toString())
            .relatedIdentifier(RELATED_IDENTIFIER_5_VALUE)
            .build();

    public final CreateRelatedIdentifierDto IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO = CreateRelatedIdentifierDto.builder()
            .value(RELATED_IDENTIFIER_5_VALUE)
            .type(RELATED_IDENTIFIER_5_TYPE_DTO)
            .relation(RELATED_IDENTIFIER_5_RELATION_TYPE_DTO)
            .build();

    public final SaveRelatedIdentifierDto IDENTIFIER_1_RELATED_IDENTIFIER_5_SAVE_DTO = SaveRelatedIdentifierDto.builder()
            .value(RELATED_IDENTIFIER_5_VALUE)
            .type(RELATED_IDENTIFIER_5_TYPE_DTO)
            .relation(RELATED_IDENTIFIER_5_RELATION_TYPE_DTO)
            .build();

    public final CreateIdentifierDto IDENTIFIER_5_CREATE_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_2_ID)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_5_PUBLISHER)
            .build();

    public final IdentifierSaveDto IDENTIFIER_5_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_5_ID)
            .queryId(QUERY_2_ID)
            .databaseId(DATABASE_2_ID)
            .doi(IDENTIFIER_5_DOI)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_5_DESCRIPTION_1_CREATE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_5_TITLE_1_CREATE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationDay(IDENTIFIER_5_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_5_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_5_CREATOR_1_CREATE_DTO, IDENTIFIER_5_CREATOR_2_CREATE_DTO)))
            .publisher(IDENTIFIER_5_PUBLISHER)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .type(IDENTIFIER_5_TYPE_DTO)
            .funders(new LinkedList<>())
            .build();

    public final static UUID IDENTIFIER_6_ID = UUID.fromString("a244204d-9671-42a0-be07-9b14402238fd");
    public final static String IDENTIFIER_6_DOI = null;
    public final static Instant IDENTIFIER_6_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant IDENTIFIER_6_MODIFIED = Instant.ofEpochSecond(1541588352L);
    public final static Instant IDENTIFIER_6_EXECUTION = Instant.ofEpochSecond(1541588352L);
    public final static Integer IDENTIFIER_6_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_6_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_6_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_6_QUERY_HASH = QUERY_3_QUERY_HASH;
    public final static String IDENTIFIER_6_RESULT_HASH = QUERY_3_RESULT_HASH;
    public final static String IDENTIFIER_6_QUERY = QUERY_3_STATEMENT;
    public final static String IDENTIFIER_6_NORMALIZED = QUERY_3_STATEMENT_NORMALIZED;
    public final static Long IDENTIFIER_6_RESULT_NUMBER = QUERY_3_RESULT_NUMBER;
    public final static String IDENTIFIER_6_PUBLISHER = "Norwegian Government";
    public final static IdentifierType IDENTIFIER_6_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_6_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static IdentifierStatusType IDENTIFIER_6_STATUS_TYPE = IdentifierStatusType.PUBLISHED;
    public final static IdentifierStatusTypeDto IDENTIFIER_6_STATUS_TYPE_DTO = IdentifierStatusTypeDto.PUBLISHED;

    public final static UUID IDENTIFIER_6_TITLE_1_ID = UUID.fromString("0449011c-1490-4c8e-b46c-c1f862126aea");
    public final static String IDENTIFIER_6_TITLE_1_TITLE = "Norwegian weather data";
    public final static String IDENTIFIER_6_TITLE_1_TITLE_MODIFY = "Norwegian weather some data";
    public final static LanguageType IDENTIFIER_6_TITLE_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_6_TITLE_1_LANG_DTO = LanguageTypeDto.EN;

    public final IdentifierTitle IDENTIFIER_6_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_6_TITLE_1_ID)
            .title(IDENTIFIER_6_TITLE_1_TITLE)
            .language(IDENTIFIER_6_TITLE_1_LANG)
            .build();

    public final IdentifierTitleDto IDENTIFIER_6_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_6_TITLE_1_ID)
            .title(IDENTIFIER_6_TITLE_1_TITLE)
            .language(IDENTIFIER_6_TITLE_1_LANG_DTO)
            .build();

    public final IdentifierTitleDto IDENTIFIER_6_TITLE_1_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_6_TITLE_1_ID)
            .title(IDENTIFIER_6_TITLE_1_TITLE_MODIFY)
            .language(IDENTIFIER_6_TITLE_1_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_6_TITLE_1_CREATE_DTO = SaveIdentifierTitleDto.builder()
            .title(IDENTIFIER_6_TITLE_1_TITLE_MODIFY)
            .language(IDENTIFIER_6_TITLE_1_LANG_DTO)
            .build();

    public final static UUID IDENTIFIER_6_DESCRIPTION_1_ID = UUID.fromString("aac03bbd-27e6-419d-8118-f996d594f00f");
    public final static String IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION = "Selecting all from the weather Norwegian table";
    public final static String IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION_MODIFY = "Selecting some from the weather Norwegian table";
    public final static LanguageType IDENTIFIER_6_DESCRIPTION_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_6_DESCRIPTION_1_LANG_DTO = LanguageTypeDto.EN;

    public final IdentifierDescription IDENTIFIER_6_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_6_DESCRIPTION_1_ID)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_6_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_6_DESCRIPTION_1_ID)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG_DTO)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_6_DESCRIPTION_1_DTO_MODIFY = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_6_DESCRIPTION_1_ID)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION_MODIFY)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG_DTO)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_6_DESCRIPTION_1_CREATE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(null)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION_MODIFY)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static UUID IDENTIFIER_6_CREATOR_1_ID = UUID.fromString("f8a52dca-8aec-46c1-b0e1-603dbe6a1a65");
    public final static Integer IDENTIFIER_6_CREATOR_1_ORD_POS = 0;
    public final static String IDENTIFIER_6_CREATOR_1_FIRSTNAME = "Max";
    public final static String IDENTIFIER_6_CREATOR_1_LASTNAME = "Mustermann";
    public final static String IDENTIFIER_6_CREATOR_1_NAME = IDENTIFIER_5_CREATOR_1_LASTNAME + ", " + IDENTIFIER_5_CREATOR_1_FIRSTNAME;
    public final static String IDENTIFIER_6_CREATOR_1_AFFIL = "TU Graz";
    public final static String IDENTIFIER_6_CREATOR_1_AFFIL_ROR = "https://ror.org/04wn28048";
    public final static String IDENTIFIER_6_CREATOR_1_AFFIL_URI = "https://ror.org/";
    public final static AffiliationIdentifierSchemeType IDENTIFIER_6_CREATOR_1_AFFIL_SCHEME = AffiliationIdentifierSchemeType.ROR;
    public final static AffiliationIdentifierSchemeTypeDto IDENTIFIER_6_CREATOR_1_AFFIL_SCHEME_DTO = AffiliationIdentifierSchemeTypeDto.ROR;
    public final static String IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER = "https://orcid.org/00000-00000-00000";
    public final static NameIdentifierSchemeType IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE = NameIdentifierSchemeType.ORCID;
    public final static NameIdentifierSchemeTypeDto IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE_DTO = NameIdentifierSchemeTypeDto.ORCID;
    public final static String IDENTIFIER_6_CREATOR_1_IDENTIFIER_SCHEME_URI = "https://orcid.org/";

    public final Creator IDENTIFIER_6_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_6_CREATOR_1_ID)
            .firstname(IDENTIFIER_6_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_1_NAME)
            .nameIdentifier(IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_6_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_6_CREATOR_1_AFFIL)
            .affiliationIdentifier(IDENTIFIER_6_CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(IDENTIFIER_6_CREATOR_1_AFFIL_SCHEME)
            .affiliationIdentifierSchemeUri(IDENTIFIER_6_CREATOR_1_AFFIL_URI)
            .build();

    public final CreatorDto IDENTIFIER_6_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_1_ID)
            .firstname(IDENTIFIER_6_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_1_NAME)
            .nameIdentifier(IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER_SCHEMA_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_6_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_6_CREATOR_1_AFFIL)
            .affiliationIdentifier(IDENTIFIER_6_CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(IDENTIFIER_6_CREATOR_1_AFFIL_SCHEME_DTO)
            .affiliationIdentifierSchemeUri(IDENTIFIER_6_CREATOR_1_AFFIL_URI)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_6_CREATOR_1_CREATE_DTO = SaveIdentifierCreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_1_ID)
            .firstname(IDENTIFIER_6_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_1_NAME)
            .nameIdentifier(IDENTIFIER_6_CREATOR_1_NAME_IDENTIFIER)
            .affiliation(IDENTIFIER_6_CREATOR_1_AFFIL)
            .affiliationIdentifier(IDENTIFIER_6_CREATOR_1_AFFIL_ROR)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_6_CREATOR_1_MODIFY_DTO = SaveIdentifierCreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_1_ID)
            .firstname(IDENTIFIER_6_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_1_NAME)
            .nameIdentifier(null) // <<<
            .affiliation(IDENTIFIER_6_CREATOR_1_AFFIL)
            .affiliationIdentifier(IDENTIFIER_6_CREATOR_1_AFFIL_ROR)
            .build();

    public final static UUID IDENTIFIER_6_CREATOR_2_ID = UUID.fromString("eeae78cb-75a1-42e2-b608-7082e5fbecc6");
    public final static Integer IDENTIFIER_6_CREATOR_2_ORD_POS = 1;
    public final static String IDENTIFIER_6_CREATOR_2_FIRSTNAME = "Martina";
    public final static String IDENTIFIER_6_CREATOR_2_LASTNAME = "Mustermann";
    public final static String IDENTIFIER_6_CREATOR_2_NAME = IDENTIFIER_5_CREATOR_2_LASTNAME + ", " + IDENTIFIER_5_CREATOR_2_FIRSTNAME;
    public final static String IDENTIFIER_6_CREATOR_2_AFFIL = "TU Wien";
    public final static AffiliationIdentifierSchemeType IDENTIFIER_6_CREATOR_2_AFFIL_SCHEME = AffiliationIdentifierSchemeType.ROR;
    public final static String IDENTIFIER_6_CREATOR_2_NAME_IDENTIFIER = "https://orcid.org/00000-00000-00000";
    public final static NameIdentifierSchemeType IDENTIFIER_6_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE = NameIdentifierSchemeType.ORCID;
    public final static NameIdentifierSchemeTypeDto IDENTIFIER_6_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE_DTO = NameIdentifierSchemeTypeDto.ORCID;
    public final static String IDENTIFIER_6_CREATOR_2_IDENTIFIER_SCHEME_URI = "https://orcid.org/";

    public final Creator IDENTIFIER_6_CREATOR_2 = Creator.builder()
            .id(IDENTIFIER_6_CREATOR_2_ID)
            .firstname(IDENTIFIER_6_CREATOR_2_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_2_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_2_NAME)
            .nameIdentifier(IDENTIFIER_6_CREATOR_2_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_6_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_6_CREATOR_2_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_6_CREATOR_2_AFFIL)
            .build();

    public final CreatorDto IDENTIFIER_6_CREATOR_2_DTO = CreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_2_ID)
            .firstname(IDENTIFIER_6_CREATOR_2_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_2_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_2_NAME)
            .nameIdentifier(IDENTIFIER_6_CREATOR_2_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_6_CREATOR_2_NAME_IDENTIFIER_SCHEMA_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_6_CREATOR_2_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_6_CREATOR_2_AFFIL)
            .build();

    public final static UUID IDENTIFIER_6_CREATOR_3_ID = UUID.fromString("700058f1-6314-4cd1-9c0c-62e75c8f422b");
    public final static Integer IDENTIFIER_6_CREATOR_3_ORD_POS = 2;
    public final static String IDENTIFIER_6_CREATOR_3_FIRSTNAME = "Martina";
    public final static String IDENTIFIER_6_CREATOR_3_LASTNAME = "Mustermann";
    public final static String IDENTIFIER_6_CREATOR_3_NAME = IDENTIFIER_5_CREATOR_2_LASTNAME + ", " + IDENTIFIER_5_CREATOR_2_FIRSTNAME;
    public final static String IDENTIFIER_6_CREATOR_3_AFFIL = "TU Wien";
    public final static AffiliationIdentifierSchemeType IDENTIFIER_6_CREATOR_3_AFFIL_SCHEME = AffiliationIdentifierSchemeType.ROR;
    public final static String IDENTIFIER_6_CREATOR_3_NAME_IDENTIFIER = "https://orcid.org/00000-00000-00000";
    public final static NameIdentifierSchemeType IDENTIFIER_6_CREATOR_3_NAME_IDENTIFIER_SCHEMA_TYPE = NameIdentifierSchemeType.ORCID;
    public final static NameIdentifierSchemeTypeDto IDENTIFIER_6_CREATOR_3_NAME_IDENTIFIER_SCHEMA_TYPE_DTO = NameIdentifierSchemeTypeDto.ORCID;
    public final static String IDENTIFIER_6_CREATOR_3_IDENTIFIER_SCHEME_URI = "https://orcid.org/";
    public final static String IDENTIFIER_6_CREATOR_3_AFFIL_ROR = "https://ror.org/04wn28048";
    public final static String IDENTIFIER_6_CREATOR_3_AFFIL_URI = "https://ror.org/";
    public final static AffiliationIdentifierSchemeType IDENTIFIER_6_CREATOR_3_AFFIL_SCHEME_TYPE = AffiliationIdentifierSchemeType.ROR;
    public final static AffiliationIdentifierSchemeTypeDto IDENTIFIER_6_CREATOR_3_AFFIL_SCHEME_TYPE_DTO = AffiliationIdentifierSchemeTypeDto.ROR;

    public final Creator IDENTIFIER_6_CREATOR_3 = Creator.builder()
            .id(IDENTIFIER_6_CREATOR_3_ID)
            .firstname(IDENTIFIER_6_CREATOR_3_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_3_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_3_NAME)
            .nameIdentifier(IDENTIFIER_6_CREATOR_3_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_6_CREATOR_3_NAME_IDENTIFIER_SCHEMA_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_6_CREATOR_3_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_6_CREATOR_3_AFFIL)
            .affiliationIdentifier(IDENTIFIER_6_CREATOR_3_AFFIL_ROR)
            .affiliationIdentifierScheme(IDENTIFIER_6_CREATOR_3_AFFIL_SCHEME_TYPE)
            .affiliationIdentifierSchemeUri(IDENTIFIER_6_CREATOR_3_AFFIL_URI)
            .build();

    public final CreatorDto IDENTIFIER_6_CREATOR_3_DTO = CreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_3_ID)
            .firstname(IDENTIFIER_6_CREATOR_3_FIRSTNAME)
            .lastname(IDENTIFIER_6_CREATOR_3_LASTNAME)
            .creatorName(IDENTIFIER_6_CREATOR_3_NAME)
            .nameIdentifier(IDENTIFIER_6_CREATOR_3_NAME_IDENTIFIER)
            .nameIdentifierScheme(IDENTIFIER_6_CREATOR_3_NAME_IDENTIFIER_SCHEMA_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_6_CREATOR_3_IDENTIFIER_SCHEME_URI)
            .affiliation(IDENTIFIER_6_CREATOR_3_AFFIL)
            .affiliationIdentifier(IDENTIFIER_6_CREATOR_3_AFFIL_ROR)
            .affiliationIdentifierScheme(IDENTIFIER_6_CREATOR_3_AFFIL_SCHEME_TYPE_DTO)
            .affiliationIdentifierSchemeUri(IDENTIFIER_6_CREATOR_3_AFFIL_URI)
            .build();

    public final Identifier IDENTIFIER_6 = Identifier.builder()
            .id(IDENTIFIER_6_ID)
            .queryId(QUERY_3_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_6_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_6_TITLE_1)))
            .doi(IDENTIFIER_6_DOI)
            .database(null) /* DATABASE_3 */
            .created(IDENTIFIER_6_CREATED)
            .lastModified(IDENTIFIER_6_MODIFIED)
            .execution(IDENTIFIER_6_EXECUTION)
            .publicationDay(IDENTIFIER_6_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_6_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_6_QUERY_HASH)
            .resultHash(IDENTIFIER_6_RESULT_HASH)
            .query(IDENTIFIER_6_QUERY)
            .queryNormalized(IDENTIFIER_6_NORMALIZED)
            .resultNumber(IDENTIFIER_6_RESULT_NUMBER)
            .publisher(IDENTIFIER_6_PUBLISHER)
            .type(IDENTIFIER_6_TYPE)
            .ownedBy(USER_3_USERNAME)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_6_CREATOR_1, IDENTIFIER_6_CREATOR_2, IDENTIFIER_6_CREATOR_3)))
            .status(IDENTIFIER_6_STATUS_TYPE)
            .funders(new LinkedList<>())
            .build();

    public final IdentifierDto IDENTIFIER_6_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(DATABASE_3_ID)
            .queryId(QUERY_3_ID)
            .links(LinksDto.builder()
                    .self("/api/v1/identifier/" + IDENTIFIER_6_ID)
                    .selfHtml("/pid/" + IDENTIFIER_6_ID)
                    .data("/api/v1/database/" + DATABASE_3_ID + "/subset/" + QUERY_3_ID + "/data")
                    .dashboardHtml("/d/" + DATABASE_3_DASHBOARD_UID)
                    .build())
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_6_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_6_TITLE_1_DTO)))
            .doi(IDENTIFIER_6_DOI)
            .execution(IDENTIFIER_6_EXECUTION)
            .publicationDay(IDENTIFIER_6_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_6_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_6_QUERY_HASH)
            .resultHash(IDENTIFIER_6_RESULT_HASH)
            .query(IDENTIFIER_6_QUERY)
            .queryNormalized(IDENTIFIER_6_NORMALIZED)
            .resultNumber(IDENTIFIER_6_RESULT_NUMBER)
            .publisher(IDENTIFIER_6_PUBLISHER)
            .type(IDENTIFIER_6_TYPE_DTO)
            .owner(USER_3_BRIEF_DTO)
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_6_CREATOR_1_DTO, IDENTIFIER_6_CREATOR_2_DTO, IDENTIFIER_6_CREATOR_3_DTO)))
            .status(IDENTIFIER_6_STATUS_TYPE_DTO)
            .build();


    public final IdentifierBriefDto IDENTIFIER_6_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(DATABASE_3_ID)
            .queryId(QUERY_3_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_6_TITLE_1_DTO)))
            .doi(IDENTIFIER_6_DOI)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_6_PUBLISHER)
            .type(IDENTIFIER_6_TYPE_DTO)
            .status(IDENTIFIER_6_STATUS_TYPE_DTO)
            .build();

    public final CreateIdentifierDto IDENTIFIER_6_CREATE_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_3_ID)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_6_PUBLISHER)
            .build();

    public final IdentifierSaveDto IDENTIFIER_6_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(DATABASE_3_ID)
            .queryId(QUERY_3_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_6_DESCRIPTION_1_CREATE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_6_TITLE_1_CREATE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_6_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_6_CREATOR_1_CREATE_DTO)))
            .publisher(IDENTIFIER_6_PUBLISHER)
            .type(IDENTIFIER_6_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .build();

    public final static UUID IDENTIFIER_7_ID = UUID.fromString("b216ae00-a31d-4ecb-95fb-37eb4da3946f");
    public final static String IDENTIFIER_7_DOI = null;
    public final static Instant IDENTIFIER_7_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant IDENTIFIER_7_MODIFIED = Instant.ofEpochSecond(1541588352L);
    public final static Integer IDENTIFIER_7_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_7_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_7_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_7_PUBLISHER = "Swedish Government";
    public final static IdentifierType IDENTIFIER_7_TYPE = IdentifierType.DATABASE;
    public final static IdentifierTypeDto IDENTIFIER_7_TYPE_DTO = IdentifierTypeDto.DATABASE;
    public final static IdentifierStatusType IDENTIFIER_7_STATUS_TYPE = IdentifierStatusType.DRAFT;
    public final static IdentifierStatusTypeDto IDENTIFIER_7_STATUS_TYPE_DTO = IdentifierStatusTypeDto.DRAFT;

    public final DataCiteBody<DataCiteDoi> IDENTIFIER_7_DATA_CITE = DataCiteBody.<DataCiteDoi>builder()
            .data(DataCiteData.<DataCiteDoi>builder()
                    .type("dois")
                    .attributes(DataCiteDoi.builder()
                            .doi(IDENTIFIER_7_DOI)
                            .build())
                    .build())
            .build();

    public final static UUID IDENTIFIER_7_TITLE_1_ID = UUID.fromString("fdd698a7-ebcf-444f-b972-00effb3e87b2");
    public final static String IDENTIFIER_7_TITLE_1_TITLE = "Some data";
    public final static Integer IDENTIFIER_7_TITLE_1_ORD_POS = 0;

    public final IdentifierTitle IDENTIFIER_7_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_7_TITLE_1_ID)
            .ordinalPosition(IDENTIFIER_7_TITLE_1_ORD_POS)
            .title(IDENTIFIER_7_TITLE_1_TITLE)
            .build();

    public final IdentifierTitleDto IDENTIFIER_7_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_7_TITLE_1_ID)
            .title(IDENTIFIER_7_TITLE_1_TITLE)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_7_TITLE_1_SAVE_DTO = SaveIdentifierTitleDto.builder()
            .id(IDENTIFIER_7_TITLE_1_ID)
            .title(IDENTIFIER_7_TITLE_1_TITLE)
            .build();

    public final static UUID IDENTIFIER_7_DESCRIPTION_1_ID = UUID.fromString("dc4cbcb4-b1a1-496b-a65d-e4db23ae29d3");
    public final static String IDENTIFIER_7_DESCRIPTION_1_DESCRIPTION = "The nicest data you will ever see";
    public final static Integer IDENTIFIER_7_DESCRIPTION_1_ORD_POS = 0;
    public final static LanguageType IDENTIFIER_7_DESCRIPTION_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_7_DESCRIPTION_1_LANG_DTO = LanguageTypeDto.EN;

    public final IdentifierDescription IDENTIFIER_7_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_7_DESCRIPTION_1_ID)
            .ordinalPosition(IDENTIFIER_7_DESCRIPTION_1_ORD_POS)
            .description(IDENTIFIER_7_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_7_DESCRIPTION_1_LANG)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_7_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_7_DESCRIPTION_1_ID)
            .description(IDENTIFIER_7_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_7_DESCRIPTION_1_LANG_DTO)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_7_DESCRIPTION_1_SAVE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(IDENTIFIER_7_DESCRIPTION_1_ID)
            .description(IDENTIFIER_7_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_7_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static UUID IDENTIFIER_7_CREATOR_1_ID = UUID.fromString("b899c367-06c7-4f47-8aea-5f15061ee3ee");
    public final static Integer IDENTIFIER_7_CREATOR_1_ORD_POS = 0;
    public final static String IDENTIFIER_7_CREATOR_1_IDENTIFIER_SCHEME_URI = "https://orcid.org/";

    public final Creator IDENTIFIER_7_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_7_CREATOR_1_ID)
            .ordinalPosition(IDENTIFIER_7_CREATOR_1_ORD_POS)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeType.ORCID)
            .nameIdentifierSchemeUri(IDENTIFIER_7_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final CreatorDto IDENTIFIER_7_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_7_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .nameIdentifierSchemeUri(IDENTIFIER_7_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final IdentifierDto IDENTIFIER_7_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_7_ID)
            .databaseId(DATABASE_4_ID)
            .links(LinksDto.builder()
                    .self("/api/v1/identifier/" + IDENTIFIER_7_ID)
                    .selfHtml("/pid/" + IDENTIFIER_7_ID)
                    .dashboardHtml("/d/" + DATABASE_4_DASHBOARD_UID)
                    .build())
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_7_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_7_TITLE_1_DTO)))
            .doi(IDENTIFIER_7_DOI)
            .publicationDay(IDENTIFIER_7_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE_DTO)
            .owner(USER_4_BRIEF_DTO)
            .relatedIdentifiers(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .funders(new LinkedList<>())
            .creators(new LinkedList<>(List.of(IDENTIFIER_7_CREATOR_1_DTO)))
            .status(IDENTIFIER_7_STATUS_TYPE_DTO)
            .created(IDENTIFIER_7_CREATED)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_7_CREATOR_1_CREATE_DTO = SaveIdentifierCreatorDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .build();

    public final CreateIdentifierDto IDENTIFIER_7_CREATE_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_4_ID)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .build();

    public final IdentifierSaveDto IDENTIFIER_7_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_7_ID)
            .databaseId(DATABASE_4_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_7_DESCRIPTION_1_SAVE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_7_TITLE_1_SAVE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationDay(IDENTIFIER_7_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_7_CREATOR_1_CREATE_DTO)))
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE_DTO)
            .build();

    public final static UUID IDENTIFIER_2_DESCRIPTION_1_ID = UUID.fromString("ab026bb6-9e1b-43cd-91bb-a556b76b65f4");
    public final static String IDENTIFIER_2_DESCRIPTION_1_DESCRIPTION = "Weather data collected";
    public final static Integer IDENTIFIER_2_DESCRIPTION_1_ORD_POS = 0;

    public final IdentifierDescription IDENTIFIER_2_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_2_DESCRIPTION_1_ID)
            .ordinalPosition(IDENTIFIER_2_DESCRIPTION_1_ORD_POS)
            .description(IDENTIFIER_2_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_2_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_2_DESCRIPTION_1_ID)
            .description(IDENTIFIER_2_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final CreateIdentifierDescriptionDto IDENTIFIER_2_DESCRIPTION_1_CREATE_DTO = CreateIdentifierDescriptionDto.builder()
            .description(IDENTIFIER_2_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_2_DESCRIPTION_1_SAVE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(IDENTIFIER_2_DESCRIPTION_1_ID)
            .description(IDENTIFIER_2_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final static UUID IDENTIFIER_2_ID = UUID.fromString("fdb95f60-48e7-4e74-8122-d3c8d079c889");
    public final static String IDENTIFIER_2_DOI = null;
    public final static Instant IDENTIFIER_2_CREATED = Instant.ofEpochSecond(1651588352L);
    public final static Instant IDENTIFIER_2_MODIFIED = Instant.ofEpochSecond(1551588352L);
    public final static Instant IDENTIFIER_2_EXECUTION = QUERY_1_EXECUTION;
    public final static Integer IDENTIFIER_2_PUBLICATION_DAY = 10;
    public final static Integer IDENTIFIER_2_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_2_PUBLICATION_YEAR = 2023;
    public final static String IDENTIFIER_2_QUERY_HASH = QUERY_1_QUERY_HASH;
    public final static String IDENTIFIER_2_RESULT_HASH = QUERY_1_RESULT_HASH;
    public final static String IDENTIFIER_2_QUERY = QUERY_1_STATEMENT;
    public final static String IDENTIFIER_2_NORMALIZED = QUERY_1_STATEMENT_NORMALIZED;
    public final static Long IDENTIFIER_2_RESULT_NUMBER = QUERY_1_RESULT_NUMBER;
    public final static String IDENTIFIER_2_PUBLISHER = "Swedish Government";
    public final static IdentifierType IDENTIFIER_2_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_2_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static IdentifierStatusType IDENTIFIER_2_STATUS_TYPE = IdentifierStatusType.DRAFT;
    public final static IdentifierStatusTypeDto IDENTIFIER_2_STATUS_TYPE_DTO = IdentifierStatusTypeDto.DRAFT;

    public final static UUID IDENTIFIER_2_CREATOR_1_ID = UUID.fromString("c541c1ab-0a27-4403-8278-333f6ff14139");
    public final static Integer IDENTIFIER_2_CREATOR_1_ORD_POS = 0;
    public final static String IDENTIFIER_2_CREATOR_1_NAME = "Siemens (Austria)";
    public final static NameType IDENTIFIER_2_CREATOR_1_NAME_TYPE = NameType.ORGANIZATIONAL;
    public final static NameTypeDto IDENTIFIER_2_CREATOR_1_NAME_TYPE_DTO = NameTypeDto.ORGANIZATIONAL;
    public final static String IDENTIFIER_2_CREATOR_1_ROR = "https://ror.org/03794w632";
    public final static String IDENTIFIER_2_CREATOR_1_IDENTIFIER_SCHEME_URI = "https://ror.org/";
    public final NameIdentifierSchemeType IDENTIFIER_2_CREATOR_1_IDENTIFIER_SCHEME_TYPE = NameIdentifierSchemeType.ROR;
    public final NameIdentifierSchemeTypeDto IDENTIFIER_2_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO = NameIdentifierSchemeTypeDto.ROR;

    public final Creator IDENTIFIER_2_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_2_CREATOR_1_ID)
            .ordinalPosition(IDENTIFIER_2_CREATOR_1_ORD_POS)
            .creatorName(IDENTIFIER_2_CREATOR_1_NAME)
            .nameType(IDENTIFIER_2_CREATOR_1_NAME_TYPE)
            .nameIdentifier(IDENTIFIER_2_CREATOR_1_ROR)
            .nameIdentifierScheme(IDENTIFIER_2_CREATOR_1_IDENTIFIER_SCHEME_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_2_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .build();

    public final CreatorDto IDENTIFIER_2_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_2_CREATOR_1_ID)
            .creatorName(IDENTIFIER_2_CREATOR_1_NAME)
            .nameType(IDENTIFIER_2_CREATOR_1_NAME_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_2_CREATOR_1_ROR)
            .nameIdentifierScheme(IDENTIFIER_2_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_2_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_2_CREATOR_1_SAVE_DTO = SaveIdentifierCreatorDto.builder()
            .id(IDENTIFIER_2_CREATOR_1_ID)
            .creatorName(IDENTIFIER_2_CREATOR_1_NAME)
            .nameType(IDENTIFIER_2_CREATOR_1_NAME_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_2_CREATOR_1_ROR)
            .build();

    public final CreateIdentifierCreatorDto IDENTIFIER_2_CREATOR_1_CREATE_DTO = CreateIdentifierCreatorDto.builder()
            .creatorName(IDENTIFIER_2_CREATOR_1_NAME)
            .nameType(IDENTIFIER_2_CREATOR_1_NAME_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_2_CREATOR_1_ROR)
            .build();

    public final static UUID IDENTIFIER_2_TITLE_1_ID = UUID.fromString("ddb41a2e-d9a2-4715-92f8-a785ab651b51");
    public final static String IDENTIFIER_2_TITLE_1_TITLE = "Wetterdaten";
    public final static Integer IDENTIFIER_2_TITLE_1_ORD_POS = 0;
    public final static LanguageType IDENTIFIER_2_TITLE_1_LANG = LanguageType.DE;
    public final static LanguageTypeDto IDENTIFIER_2_TITLE_1_LANG_DTO = LanguageTypeDto.DE;

    public final IdentifierTitle IDENTIFIER_2_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_2_TITLE_1_ID)
            .ordinalPosition(IDENTIFIER_2_TITLE_1_ORD_POS)
            .title(IDENTIFIER_2_TITLE_1_TITLE)
            .language(IDENTIFIER_2_TITLE_1_LANG)
            .build();

    public final IdentifierTitleDto IDENTIFIER_2_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_2_TITLE_1_ID)
            .title(IDENTIFIER_2_TITLE_1_TITLE)
            .language(IDENTIFIER_2_TITLE_1_LANG_DTO)
            .build();

    public final IdentifierTitleDto IDENTIFIER_2_TITLE_1_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_2_TITLE_1_ID)
            .language(IDENTIFIER_2_TITLE_1_LANG_DTO)
            .build();

    public final CreateIdentifierTitleDto IDENTIFIER_2_TITLE_1_CREATE_DTO = CreateIdentifierTitleDto.builder()
            .title(IDENTIFIER_2_TITLE_1_TITLE)
            .language(IDENTIFIER_2_TITLE_1_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_2_TITLE_1_SAVE_DTO = SaveIdentifierTitleDto.builder()
            .id(IDENTIFIER_2_TITLE_1_ID)
            .title(IDENTIFIER_2_TITLE_1_TITLE)
            .language(IDENTIFIER_2_TITLE_1_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_2_TITLE_1_UPDATE_DTO = SaveIdentifierTitleDto.builder()
            .id(IDENTIFIER_2_TITLE_1_ID)
            .language(IDENTIFIER_2_TITLE_1_LANG_DTO)
            .build();

    public final Identifier IDENTIFIER_2 = Identifier.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(QUERY_1_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_2_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_2_TITLE_1)))
            .doi(IDENTIFIER_2_DOI)
            .database(null /* DATABASE_1 */)
            .created(IDENTIFIER_2_CREATED)
            .lastModified(IDENTIFIER_2_MODIFIED)
            .execution(IDENTIFIER_2_EXECUTION)
            .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_2_QUERY_HASH)
            .resultHash(IDENTIFIER_2_RESULT_HASH)
            .query(IDENTIFIER_2_QUERY)
            .queryNormalized(IDENTIFIER_2_NORMALIZED)
            .resultNumber(IDENTIFIER_2_RESULT_NUMBER)
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE)
            .ownedBy(USER_1_USERNAME)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_2_CREATOR_1)))
            .status(IDENTIFIER_2_STATUS_TYPE)
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .build();

    public final IdentifierDto IDENTIFIER_2_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(QUERY_1_ID)
            .databaseId(DATABASE_1_ID)
            .links(LinksDto.builder()
                    .self("/api/v1/identifier/" + IDENTIFIER_2_ID)
                    .selfHtml("/pid/" + IDENTIFIER_2_ID)
                    .data("/api/v1/database/" + DATABASE_1_ID + "/subset/" + QUERY_1_ID + "/data")
                    .build())
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_2_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_2_TITLE_1_DTO)))
            .doi(IDENTIFIER_2_DOI)
            .execution(IDENTIFIER_2_EXECUTION)
            .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_2_QUERY_HASH)
            .resultHash(IDENTIFIER_2_RESULT_HASH)
            .query(IDENTIFIER_2_QUERY)
            .queryNormalized(IDENTIFIER_2_NORMALIZED)
            .resultNumber(IDENTIFIER_2_RESULT_NUMBER)
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .owner(USER_1_BRIEF_DTO)
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_2_CREATOR_1_DTO)))
            .status(IDENTIFIER_2_STATUS_TYPE_DTO)
            .build();

    public final IdentifierBriefDto IDENTIFIER_2_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(QUERY_1_ID)
            .databaseId(DATABASE_1_ID)
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_2_DOI)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .status(IDENTIFIER_2_STATUS_TYPE_DTO)
            .build();

    public final IdentifierSaveDto IDENTIFIER_2_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_2_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(QUERY_1_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_2_DESCRIPTION_1_SAVE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_2_TITLE_1_SAVE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_2_CREATOR_1_SAVE_DTO)))
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .queryId(QUERY_1_ID)
            .build();

    public final CreateIdentifierDto IDENTIFIER_2_CREATE_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_1_ID)
            .queryId(QUERY_1_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_2_DESCRIPTION_1_CREATE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_2_TITLE_1_CREATE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_2_CREATOR_1_CREATE_DTO)))
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .queryId(QUERY_1_ID)
            .build();

    public final static UUID IDENTIFIER_3_CREATOR_1_ID = UUID.fromString("275b6f93-350a-4488-84d3-8b0471aaefd8");
    public final static Integer IDENTIFIER_3_CREATOR_1_ORD_POS = 0;
    public final static String IDENTIFIER_3_CREATOR_1_NAME = "WD-40 (United States)";
    public final static NameType IDENTIFIER_3_CREATOR_1_NAME_TYPE = NameType.ORGANIZATIONAL;
    public final static NameTypeDto IDENTIFIER_3_CREATOR_1_NAME_TYPE_DTO = NameTypeDto.ORGANIZATIONAL;
    public final static String IDENTIFIER_3_CREATOR_1_ROR = "https://ror.org/01eac6f58";
    public final static String IDENTIFIER_3_CREATOR_1_IDENTIFIER_SCHEME_URI = "https://ror.org/";
    public final NameIdentifierSchemeType IDENTIFIER_3_CREATOR_1_IDENTIFIER_SCHEME_TYPE = NameIdentifierSchemeType.ROR;
    public final NameIdentifierSchemeTypeDto IDENTIFIER_3_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO = NameIdentifierSchemeTypeDto.ROR;

    public final Creator IDENTIFIER_3_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_3_CREATOR_1_ID)
            .ordinalPosition(IDENTIFIER_3_CREATOR_1_ORD_POS)
            .creatorName(IDENTIFIER_3_CREATOR_1_NAME)
            .nameType(IDENTIFIER_3_CREATOR_1_NAME_TYPE)
            .nameIdentifier(IDENTIFIER_3_CREATOR_1_ROR)
            .nameIdentifierScheme(IDENTIFIER_3_CREATOR_1_IDENTIFIER_SCHEME_TYPE)
            .nameIdentifierSchemeUri(IDENTIFIER_3_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .build();

    public final CreatorDto IDENTIFIER_3_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_3_CREATOR_1_ID)
            .creatorName(IDENTIFIER_3_CREATOR_1_NAME)
            .nameType(IDENTIFIER_3_CREATOR_1_NAME_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_3_CREATOR_1_ROR)
            .nameIdentifierScheme(IDENTIFIER_3_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO)
            .nameIdentifierSchemeUri(IDENTIFIER_3_CREATOR_1_IDENTIFIER_SCHEME_URI)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_3_CREATOR_1_SAVE_DTO = SaveIdentifierCreatorDto.builder()
            .id(IDENTIFIER_3_CREATOR_1_ID)
            .creatorName(IDENTIFIER_3_CREATOR_1_NAME)
            .nameType(IDENTIFIER_3_CREATOR_1_NAME_TYPE_DTO)
            .nameIdentifier(IDENTIFIER_3_CREATOR_1_ROR)
            .build();

    public final static UUID IDENTIFIER_3_DESCRIPTION_1_ID = UUID.fromString("fb613521-470d-44f0-9dc9-687caf3a6cd3");
    public final static String IDENTIFIER_3_DESCRIPTION_1_DESCRIPTION = "Polish weather data";
    public final static Integer IDENTIFIER_3_DESCRIPTION_1_ORD_POS = 0;

    public final IdentifierDescription IDENTIFIER_3_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_3_DESCRIPTION_1_ID)
            .ordinalPosition(IDENTIFIER_3_DESCRIPTION_1_ORD_POS)
            .description(IDENTIFIER_3_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_3_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_3_DESCRIPTION_1_ID)
            .description(IDENTIFIER_3_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_3_DESCRIPTION_1_DTO_MODIFY = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_3_DESCRIPTION_1_ID)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_3_DESCRIPTION_1_CREATE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(null)
            .description(IDENTIFIER_3_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_3_DESCRIPTION_1_SAVE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(IDENTIFIER_3_DESCRIPTION_1_ID)
            .description(IDENTIFIER_3_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final static UUID IDENTIFIER_3_TITLE_1_ID = UUID.fromString("02ef1fd4-de91-4510-9af3-732586fa2a06");
    public final static String IDENTIFIER_3_TITLE_1_TITLE = "Poland weather data";
    public final static Integer IDENTIFIER_3_TITLE_1_ORD_POS = 0;
    public final static LanguageType IDENTIFIER_3_TITLE_1_LANG = LanguageType.PL;
    public final static LanguageTypeDto IDENTIFIER_3_TITLE_1_LANG_DTO = LanguageTypeDto.PL;

    public final IdentifierTitle IDENTIFIER_3_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_3_TITLE_1_ID)
            .ordinalPosition(IDENTIFIER_3_TITLE_1_ORD_POS)
            .title(IDENTIFIER_3_TITLE_1_TITLE)
            .language(IDENTIFIER_3_TITLE_1_LANG)
            .build();

    public final IdentifierTitleDto IDENTIFIER_3_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_3_TITLE_1_ID)
            .title(IDENTIFIER_3_TITLE_1_TITLE)
            .language(IDENTIFIER_3_TITLE_1_LANG_DTO)
            .build();

    public final IdentifierTitleDto IDENTIFIER_3_TITLE_1_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_3_TITLE_1_ID)
            .language(IDENTIFIER_3_TITLE_1_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_3_TITLE_1_CREATE_DTO = SaveIdentifierTitleDto.builder()
            .id(null)
            .title(IDENTIFIER_3_TITLE_1_TITLE)
            .language(IDENTIFIER_3_TITLE_1_LANG_DTO)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_3_TITLE_1_SAVE_DTO = SaveIdentifierTitleDto.builder()
            .id(IDENTIFIER_3_TITLE_1_ID)
            .title(IDENTIFIER_3_TITLE_1_TITLE)
            .language(IDENTIFIER_3_TITLE_1_LANG_DTO)
            .build();

    public final static UUID IDENTIFIER_3_ID = UUID.fromString("e2d831c2-3694-4fdc-8c48-7a7e94b73c43");
    public final static String IDENTIFIER_3_DOI = null;
    public final static Instant IDENTIFIER_3_CREATED = Instant.ofEpochSecond(1651588352L);
    public final static Instant IDENTIFIER_3_MODIFIED = Instant.ofEpochSecond(1551588352L);
    public final static Integer IDENTIFIER_3_PUBLICATION_DAY = 10;
    public final static Integer IDENTIFIER_3_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_3_PUBLICATION_YEAR = 2023;
    public final static String IDENTIFIER_3_QUERY_HASH = VIEW_1_QUERY_HASH;
    public final static String IDENTIFIER_3_RESULT_HASH = null;
    public final static String IDENTIFIER_3_QUERY = VIEW_1_QUERY;
    public final static String IDENTIFIER_3_NORMALIZED = VIEW_1_QUERY;
    public final static Long IDENTIFIER_3_RESULT_NUMBER = null;
    public final static String IDENTIFIER_3_PUBLISHER = "Polish Government";
    public final static IdentifierType IDENTIFIER_3_TYPE = IdentifierType.VIEW;
    public final static IdentifierTypeDto IDENTIFIER_3_TYPE_DTO = IdentifierTypeDto.VIEW;
    public final static IdentifierStatusType IDENTIFIER_3_STATUS_TYPE = IdentifierStatusType.DRAFT;
    public final static IdentifierStatusTypeDto IDENTIFIER_3_STATUS_TYPE_DTO = IdentifierStatusTypeDto.DRAFT;

    public final Identifier IDENTIFIER_3 = Identifier.builder()
            .id(IDENTIFIER_3_ID)
            .viewId(VIEW_1_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_3_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_3_TITLE_1)))
            .doi(IDENTIFIER_3_DOI)
            .database(null /* DATABASE_1 */)
            .created(IDENTIFIER_3_CREATED)
            .lastModified(IDENTIFIER_3_MODIFIED)
            .publicationDay(IDENTIFIER_3_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_3_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_3_QUERY_HASH)
            .resultHash(IDENTIFIER_3_RESULT_HASH)
            .query(IDENTIFIER_3_QUERY)
            .queryNormalized(IDENTIFIER_3_NORMALIZED)
            .resultNumber(IDENTIFIER_3_RESULT_NUMBER)
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE)
            .ownedBy(USER_1_USERNAME)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_3_CREATOR_1)))
            .funders(new LinkedList<>())
            .relatedIdentifiers(new LinkedList<>())
            .status(IDENTIFIER_3_STATUS_TYPE)
            .build();

    public final IdentifierDto IDENTIFIER_3_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(DATABASE_1_ID)
            .viewId(VIEW_1_ID)
            .links(LinksDto.builder()
                    .self("/api/v1/identifier/" + IDENTIFIER_3_ID)
                    .selfHtml("/pid/" + IDENTIFIER_3_ID)
                    .data("/api/v1/database/" + DATABASE_1_ID + "/view/" + VIEW_1_ID + "/data")
                    .build())
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_3_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_3_TITLE_1_DTO)))
            .doi(IDENTIFIER_3_DOI)
            .publicationDay(IDENTIFIER_3_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_3_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_3_QUERY_HASH)
            .resultHash(IDENTIFIER_3_RESULT_HASH)
            .query(IDENTIFIER_3_QUERY)
            .queryNormalized(IDENTIFIER_3_NORMALIZED)
            .resultNumber(IDENTIFIER_3_RESULT_NUMBER)
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .owner(USER_1_BRIEF_DTO)
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_3_CREATOR_1_DTO)))
            .status(IDENTIFIER_3_STATUS_TYPE_DTO)
            .build();

    public final IdentifierBriefDto IDENTIFIER_3_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(DATABASE_1_ID)
            .viewId(VIEW_1_ID)
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_3_DOI)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .status(IDENTIFIER_3_STATUS_TYPE_DTO)
            .build();

    public final CreateIdentifierDto IDENTIFIER_3_CREATE_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_1_ID)
            .viewId(VIEW_1_ID)
            .doi(IDENTIFIER_3_DOI)
            .type(IDENTIFIER_3_TYPE_DTO)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_3_PUBLISHER)
            .build();

    public final IdentifierSaveDto IDENTIFIER_3_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(DATABASE_1_ID)
            .viewId(VIEW_1_ID)
            .doi(IDENTIFIER_3_DOI)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_3_DESCRIPTION_1_SAVE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_3_TITLE_1_SAVE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationDay(IDENTIFIER_3_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_3_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_3_CREATOR_1_SAVE_DTO)))
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .funders(new LinkedList<>())
            .relatedIdentifiers(new LinkedList<>())
            .build();

    public final static UUID IDENTIFIER_4_CREATOR_1_ID = UUID.fromString("adbfe9ac-0a6d-492f-9782-8915d0bdc76f");
    public final static Integer IDENTIFIER_4_CREATOR_1_ORD_POS = 0;
    public final static String IDENTIFIER_4_CREATOR_1_NAME = CREATOR_4_NAME;
    public final static NameType IDENTIFIER_4_CREATOR_1_NAME_TYPE = NameType.PERSONAL;
    public final static NameTypeDto IDENTIFIER_4_CREATOR_1_NAME_TYPE_DTO = NameTypeDto.PERSONAL;

    public final Creator IDENTIFIER_4_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_4_CREATOR_1_ID)
            .ordinalPosition(IDENTIFIER_4_CREATOR_1_ORD_POS)
            .creatorName(IDENTIFIER_4_CREATOR_1_NAME)
            .nameType(IDENTIFIER_4_CREATOR_1_NAME_TYPE)
            .build();

    public final CreatorDto IDENTIFIER_4_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_4_CREATOR_1_ID)
            .creatorName(IDENTIFIER_4_CREATOR_1_NAME)
            .nameType(IDENTIFIER_4_CREATOR_1_NAME_TYPE_DTO)
            .build();

    public final SaveIdentifierCreatorDto IDENTIFIER_4_CREATOR_1_SAVE_DTO = SaveIdentifierCreatorDto.builder()
            .id(IDENTIFIER_4_CREATOR_1_ID)
            .creatorName(IDENTIFIER_4_CREATOR_1_NAME)
            .nameType(IDENTIFIER_4_CREATOR_1_NAME_TYPE_DTO)
            .build();

    public final static UUID IDENTIFIER_4_TITLE_1_ID = UUID.fromString("7409c03c-35ab-4e19-9876-88967ef37024");
    public final static String IDENTIFIER_4_TITLE_1_TITLE = "Austrian weather data";
    public final static Integer IDENTIFIER_4_TITLE_1_ORD_POS = 0;

    public final IdentifierTitle IDENTIFIER_4_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_4_TITLE_1_ID)
            .ordinalPosition(IDENTIFIER_4_TITLE_1_ORD_POS)
            .title(IDENTIFIER_4_TITLE_1_TITLE)
            .build();

    public final IdentifierTitleDto IDENTIFIER_4_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_4_TITLE_1_ID)
            .title(IDENTIFIER_4_TITLE_1_TITLE)
            .build();

    public final SaveIdentifierTitleDto IDENTIFIER_4_TITLE_1_SAVE_DTO = SaveIdentifierTitleDto.builder()
            .id(IDENTIFIER_4_TITLE_1_ID)
            .title(IDENTIFIER_4_TITLE_1_TITLE)
            .build();

    public final static UUID IDENTIFIER_4_DESCRIPTION_1_ID = UUID.fromString("e616779b-7c46-46a9-89a9-92187441b7a3");
    public final static String IDENTIFIER_4_DESCRIPTION_1_DESCRIPTION = "Weather data";
    public final static Integer IDENTIFIER_4_DESCRIPTION_1_ORD_POS = 0;

    public final IdentifierDescription IDENTIFIER_4_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_4_DESCRIPTION_1_ID)
            .ordinalPosition(IDENTIFIER_4_DESCRIPTION_1_ORD_POS)
            .description(IDENTIFIER_4_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final IdentifierDescriptionDto IDENTIFIER_4_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_4_DESCRIPTION_1_ID)
            .description(IDENTIFIER_4_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final SaveIdentifierDescriptionDto IDENTIFIER_4_DESCRIPTION_1_SAVE_DTO = SaveIdentifierDescriptionDto.builder()
            .id(IDENTIFIER_4_DESCRIPTION_1_ID)
            .description(IDENTIFIER_4_DESCRIPTION_1_DESCRIPTION)
            .build();

    public final static UUID IDENTIFIER_4_ID = UUID.fromString("3bd69bb8-f7e3-48e4-9717-823787e7ba23");
    public final static String IDENTIFIER_4_DOI = null;
    public final static Instant IDENTIFIER_4_CREATED = Instant.ofEpochSecond(1751588352L);
    public final static Instant IDENTIFIER_4_MODIFIED = Instant.ofEpochSecond(1551588352L);
    public final static Instant IDENTIFIER_4_EXECUTION = Instant.ofEpochSecond(1551588352L);
    public final static Integer IDENTIFIER_4_PUBLICATION_DAY = 10;
    public final static Integer IDENTIFIER_4_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_4_PUBLICATION_YEAR = 2023;
    public final static String IDENTIFIER_4_RESULT_HASH = null;
    public final static Long IDENTIFIER_4_RESULT_NUMBER = null;
    public final static String IDENTIFIER_4_PUBLISHER = "Example Publisher";
    public final static IdentifierType IDENTIFIER_4_TYPE = IdentifierType.TABLE;
    public final static IdentifierTypeDto IDENTIFIER_4_TYPE_DTO = IdentifierTypeDto.TABLE;
    public final static IdentifierStatusType IDENTIFIER_4_STATUS_TYPE = IdentifierStatusType.PUBLISHED;
    public final static IdentifierStatusTypeDto IDENTIFIER_4_STATUS_TYPE_DTO = IdentifierStatusTypeDto.PUBLISHED;

    public final Identifier IDENTIFIER_4 = Identifier.builder()
            .id(IDENTIFIER_4_ID)
            .tableId(TABLE_1_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_4_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_4_TITLE_1)))
            .doi(IDENTIFIER_4_DOI)
            .database(null /* DATABASE_1 */)
            .created(IDENTIFIER_4_CREATED)
            .lastModified(IDENTIFIER_4_MODIFIED)
            .execution(IDENTIFIER_4_EXECUTION)
            .publicationDay(IDENTIFIER_4_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE)
            .ownedBy(USER_1_USERNAME)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_4_CREATOR_1)))
            .status(IDENTIFIER_4_STATUS_TYPE)
            .funders(new LinkedList<>())
            .relatedIdentifiers(new LinkedList<>())
            .build();

    public final IdentifierDto IDENTIFIER_4_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(DATABASE_1_ID)
            .tableId(TABLE_1_ID)
            .links(LinksDto.builder()
                    .self("/api/v1/identifier/" + IDENTIFIER_4_ID)
                    .selfHtml("/pid/" + IDENTIFIER_4_ID)
                    .data("/api/v1/database/" + DATABASE_1_ID + "/table/" + TABLE_1_ID + "/data")
                    .build())
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_4_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_4_TITLE_1_DTO)))
            .doi(IDENTIFIER_4_DOI)
            .execution(IDENTIFIER_4_EXECUTION)
            .publicationDay(IDENTIFIER_4_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .resultHash(IDENTIFIER_4_RESULT_HASH)
            .resultNumber(IDENTIFIER_4_RESULT_NUMBER)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE_DTO)
            .owner(USER_1_BRIEF_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_4_CREATOR_1_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .status(IDENTIFIER_4_STATUS_TYPE_DTO)
            .build();

    public final IdentifierBriefDto IDENTIFIER_4_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(DATABASE_1_ID)
            .tableId(TABLE_1_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_4_TITLE_1_DTO)))
            .doi(IDENTIFIER_4_DOI)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE_DTO)
            .status(IDENTIFIER_4_STATUS_TYPE_DTO)
            .build();

    public final CreateIdentifierDto IDENTIFIER_4_CREATE_DTO = CreateIdentifierDto.builder()
            .databaseId(DATABASE_1_ID)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .build();

    public final IdentifierSaveDto IDENTIFIER_4_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(DATABASE_1_ID)
            .tableId(TABLE_1_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_4_DESCRIPTION_1_SAVE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_4_TITLE_1_SAVE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_4_CREATOR_1_SAVE_DTO)))
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE_DTO)
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .build();

    public final static String VIRTUAL_HOST_NAME = "fda";
    public final static String VIRTUAL_HOST_DESCRIPTION = "FAIR Data Austria";
    public final static String VIRTUAL_HOST_TAGS = "";

    public final CreateVirtualHostDto VIRTUAL_HOST_CREATE_DTO = CreateVirtualHostDto.builder()
            .name(VIRTUAL_HOST_NAME)
            .description(VIRTUAL_HOST_DESCRIPTION)
            .tags(VIRTUAL_HOST_TAGS)
            .build();

    public final ExchangeUpdatePermissionsDto VIRTUAL_HOST_EXCHANGE_UPDATE_DTO = ExchangeUpdatePermissionsDto.builder()
            .exchange(DATABASE_1_EXCHANGE)
            .read(".*")
            .write(".*")
            .build();

    public final GrantVirtualHostPermissionsDto VIRTUAL_HOST_GRANT_DTO = GrantVirtualHostPermissionsDto.builder()
            .read(".*")
            .write(".*")
            .configure(".*")
            .build();

    public final static UUID BANNER_MESSAGE_1_ID = UUID.fromString("81cf09b7-0d86-44ad-be8e-a407e7d114e1");
    public final static String BANNER_MESSAGE_1_MESSAGE = "Next maintenance in 7 days!";
    public final static BannerMessageType BANNER_MESSAGE_1_TYPE = BannerMessageType.INFO;
    public final static BannerMessageTypeDto BANNER_MESSAGE_1_TYPE_DTO = BannerMessageTypeDto.INFO;
    public final static Instant BANNER_MESSAGE_1_START = Instant.ofEpochSecond(1684577786L) /* 2022-12-23 22:00:00 (UTC) */;
    public final static Instant BANNER_MESSAGE_1_END = null;

    public final BannerMessage BANNER_MESSAGE_1 = BannerMessage.builder()
            .id(BANNER_MESSAGE_1_ID)
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BANNER_MESSAGE_1_TYPE)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final BannerMessageDto BANNER_MESSAGE_1_DTO = BannerMessageDto.builder()
            .id(BANNER_MESSAGE_1_ID)
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BANNER_MESSAGE_1_TYPE_DTO)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final BannerMessageCreateDto BANNER_MESSAGE_1_CREATE_DTO = BannerMessageCreateDto.builder()
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BANNER_MESSAGE_1_TYPE_DTO)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final BannerMessageUpdateDto BANNER_MESSAGE_1_UPDATE_DTO = BannerMessageUpdateDto.builder()
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BannerMessageTypeDto.WARNING)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final static UUID BANNER_MESSAGE_2_ID = UUID.fromString("1e7e2c03-e2c6-46b8-9fdc-6668ef055d99");
    public final static String BANNER_MESSAGE_2_MESSAGE = "No operation on Christmas 2022!";
    public final static BannerMessageType BANNER_MESSAGE_2_TYPE = BannerMessageType.ERROR;
    public final static BannerMessageTypeDto BANNER_MESSAGE_2_TYPE_DTO = BannerMessageTypeDto.ERROR;
    public final static Instant BANNER_MESSAGE_2_START = Instant.ofEpochSecond(1671836400L) /* 2022-12-23 22:00:00 (UTC) */;
    public final static Instant BANNER_MESSAGE_2_END = Instant.ofEpochSecond(1672009200L) /* 2022-12-25 22:00:00 (UTC) */;

    public final BannerMessage BANNER_MESSAGE_2 = BannerMessage.builder()
            .id(BANNER_MESSAGE_2_ID)
            .message(BANNER_MESSAGE_2_MESSAGE)
            .type(BANNER_MESSAGE_2_TYPE)
            .displayStart(BANNER_MESSAGE_2_START)
            .displayEnd(BANNER_MESSAGE_2_END)
            .build();

    public final BannerMessageCreateDto BANNER_MESSAGE_2_CREATE_DTO = BannerMessageCreateDto.builder()
            .message(BANNER_MESSAGE_2_MESSAGE)
            .type(BANNER_MESSAGE_2_TYPE_DTO)
            .displayStart(BANNER_MESSAGE_2_START)
            .displayEnd(BANNER_MESSAGE_2_END)
            .build();

    public final Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_1_PUBLIC)
            .isSchemaPublic(DATABASE_1_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_1_DASHBOARD_ENABLED)
            .dashboardUid(DATABASE_1_DASHBOARD_UID)
            .name(DATABASE_1_NAME)
            .description(DATABASE_1_DESCRIPTION)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4)))
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_1_INTERNAL_NAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .created(DATABASE_1_CREATED)
            .lastModified(DATABASE_1_LAST_MODIFIED)
            .ownedBy(USER_1_USERNAME)
            .image(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
            .contactPerson(USER_1_USERNAME)
            .subsets(new LinkedList<>())
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .accesses(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final DatabaseDto DATABASE_1_DTO = DatabaseDto.builder()
            .id(DATABASE_1_ID)
            .isPublic(DATABASE_1_PUBLIC)
            .isSchemaPublic(DATABASE_1_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_1_DASHBOARD_ENABLED)
            .dashboardUid(DATABASE_1_DASHBOARD_UID)
            .name(DATABASE_1_NAME)
            .container(CONTAINER_1_DTO)
            .internalName(DATABASE_1_INTERNAL_NAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_1_DTO, IDENTIFIER_2_DTO, IDENTIFIER_3_DTO, IDENTIFIER_4_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO)))
            .views(new LinkedList<>(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO)))
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database DATABASE_1_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database.builder()
            .id(DATABASE_1_ID)
            .isPublic(DATABASE_1_PUBLIC)
            .isSchemaPublic(DATABASE_1_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_1_DASHBOARD_ENABLED)
            .container(CONTAINER_1_CACHE)
            .internalName(DATABASE_1_INTERNAL_NAME)
            .accesses(new LinkedList<>(List.of())) /* DATABASE_1_USER_1_READ_ACCESS_CACHE */
            .tables(new LinkedList<>(List.of(TABLE_1_CACHE, TABLE_2_CACHE, TABLE_3_CACHE, TABLE_4_CACHE)))
            .views(new LinkedList<>(List.of(VIEW_1_CACHE, VIEW_2_CACHE, VIEW_3_CACHE)))
            .ownedBy(USER_1_USERNAME)
            .build();

    public final DatabaseBriefDto DATABASE_1_BRIEF_DTO = DatabaseBriefDto.builder()
            .id(DATABASE_1_ID)
            .isPublic(DATABASE_1_PUBLIC)
            .isSchemaPublic(DATABASE_1_SCHEMA_PUBLIC)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNAL_NAME)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_1_BRIEF_DTO, IDENTIFIER_2_BRIEF_DTO, IDENTIFIER_3_BRIEF_DTO, IDENTIFIER_4_BRIEF_DTO)))
            .build();

    public final DatabaseAccess DATABASE_1_USER_LOCAL_ADMIN_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_LOCAL_ADMIN_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_1_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_1_USER_1_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_1_ID)
            .username(USER_1_USERNAME)
            .user(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_1_USER_1_READ_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.READ)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseGrantsDto READ_GRANT_DTO = DatabaseGrantsDto.builder()
            .type(GrantTypeDto.READ)
            .grants(Set.of("SELECT", "EXECUTE"))
            .build();

    public final DatabaseGrantsDto WRITE_GRANT_DTO = DatabaseGrantsDto.builder()
            .type(GrantTypeDto.WRITE)
            .grants(Set.of("SELECT", "CREATE", "CREATE VIEW", "CREATE ROUTINE", "CREATE TEMPORARY TABLES", "EXECUTE", "LOCK TABLES", "INDEX", "TRIGGER", "INSERT", "UPDATE", "DELETE"))
            .build();

    public final DatabaseGrantsDto UNKNOWN_GRANT_DTO = DatabaseGrantsDto.builder()
            .grants(Set.of("MONITOR"))
            .build();

    public final DatabaseAccess DATABASE_1_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_1_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_1_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_1_USER_2_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_1_ID)
            .username(USER_2_USERNAME)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final DatabaseAccess DATABASE_1_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .username(USER_2_USERNAME)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_1_USER_2_WRITE_OWN_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.WRITE_OWN)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_1_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_1_USER_2_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .username(USER_2_USERNAME)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final DatabaseAccess DATABASE_1_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_1_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_1_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_1_USER_3_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .username(USER_3_USERNAME)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.WRITE_ALL)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_1_USER_4_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .username(USER_4_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_1_USER_4_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_1_ID)
            .username(USER_4_USERNAME)
            .user(USER_4_BRIEF_DTO)
            .build();

    public final Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_2_CREATED)
            .lastModified(Instant.now())
            .isPublic(DATABASE_2_PUBLIC)
            .isSchemaPublic(DATABASE_2_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_2_DASHBOARD_ENABLED)
            .dashboardUid(DATABASE_2_DASHBOARD_UID)
            .name(DATABASE_2_NAME)
            .description(DATABASE_2_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_2_INTERNAL_NAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .created(DATABASE_2_CREATED)
            .lastModified(DATABASE_2_LAST_MODIFIED)
            .ownedBy(USER_2_USERNAME)
            .contactPerson(USER_2_USERNAME)
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .accesses(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final DatabaseDto DATABASE_2_DTO = DatabaseDto.builder()
            .id(DATABASE_2_ID)
            .isPublic(DATABASE_2_PUBLIC)
            .isSchemaPublic(DATABASE_2_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_2_DASHBOARD_ENABLED)
            .name(DATABASE_2_NAME)
            .container(CONTAINER_1_DTO)
            .internalName(DATABASE_2_INTERNAL_NAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_5_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_5_DTO, TABLE_6_DTO, TABLE_7_DTO)))
            .views(new LinkedList<>(List.of(VIEW_4_DTO)))
            .owner(USER_2_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database DATABASE_2_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database.builder()
            .id(DATABASE_2_ID)
            .isPublic(DATABASE_2_PUBLIC)
            .isSchemaPublic(DATABASE_2_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_2_DASHBOARD_ENABLED)
            .container(CONTAINER_1_CACHE)
            .internalName(DATABASE_2_INTERNAL_NAME)
            .tables(new LinkedList<>(List.of(TABLE_5_CACHE, TABLE_6_CACHE, TABLE_7_CACHE)))
            .views(new LinkedList<>(List.of(VIEW_4_CACHE)))
            .ownedBy(USER_2_USERNAME)
            .build();

    public final DatabaseBriefDto DATABASE_2_BRIEF_DTO = DatabaseBriefDto.builder()
            .id(DATABASE_2_ID)
            .isPublic(DATABASE_2_PUBLIC)
            .isSchemaPublic(DATABASE_2_SCHEMA_PUBLIC)
            .name(DATABASE_2_NAME)
            .internalName(DATABASE_2_INTERNAL_NAME)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_5_BRIEF_DTO)))
            .ownedBy(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_2_USER_2_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_2_ID)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_2_USERNAME)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_2_USER_2_WRITE_ALL_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.WRITE_ALL)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_2_USER_2_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .username(USER_2_USERNAME)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final DatabaseAccess DATABASE_2_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_2_USER_3_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_2_ID)
            .username(USER_3_USERNAME)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_2_USER_3_READ_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.READ)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_2_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .username(USER_3_USERNAME)
            .build();

    public final Database DATABASE_3 = Database.builder()
            .id(DATABASE_3_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_3_PUBLIC)
            .isSchemaPublic(DATABASE_3_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_3_DASHBOARD_ENABLED)
            .dashboardUid(DATABASE_3_DASHBOARD_UID)
            .name(DATABASE_3_NAME)
            .description(DATABASE_3_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_3_INTERNAL_NAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .created(DATABASE_3_CREATED)
            .lastModified(DATABASE_3_LAST_MODIFIED)
            .ownedBy(USER_3_USERNAME)
            .contactPerson(USER_3_USERNAME)
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .accesses(new LinkedList<>()) /* DATABASE_3_USER_1_WRITE_ALL_ACCESS */
            .identifiers(new LinkedList<>()) /* IDENTIFIER_6 */
            .build();

    public final DatabaseAccess DATABASE_3_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_3_USER_1_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_3_ID)
            .username(USER_1_USERNAME)
            .user(USER_1_BRIEF_DTO)
            .build();

    public final DatabaseAccess DATABASE_3_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .username(USER_1_USERNAME)
            .user(USER_1_BRIEF_DTO)
            .build();

    public final DatabaseAccess DATABASE_3_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_3_USER_1_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .username(USER_1_USERNAME)
            .user(USER_1_BRIEF_DTO)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_3_USER_1_WRITE_ALL_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.WRITE_ALL)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_3_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_3_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_3_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_3_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_3_USER_3_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_3_ID)
            .username(USER_3_USERNAME)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final DatabaseAccess DATABASE_3_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .username(USER_3_USERNAME)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final DatabaseAccess DATABASE_3_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccessDto DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .username(USER_3_USERNAME)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final Identifier IDENTIFIER_7 = Identifier.builder()
            .id(IDENTIFIER_7_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_7_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_7_TITLE_1)))
            .doi(IDENTIFIER_7_DOI)
            .database(null) /* DATABASE_4 */
            .created(IDENTIFIER_7_CREATED)
            .lastModified(IDENTIFIER_7_MODIFIED)
            .publicationDay(IDENTIFIER_7_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE)
            .ownedBy(USER_4_USERNAME)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_7_CREATOR_1)))
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .status(IDENTIFIER_7_STATUS_TYPE)
            .build();

    public final Database DATABASE_4 = Database.builder()
            .id(DATABASE_4_ID)
            .created(Instant.now().minus(4, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_4_PUBLIC)
            .isSchemaPublic(DATABASE_4_SCHEMA_PUBLIC)
            .isDashboardEnabled(DATABASE_4_DASHBOARD_ENABLED)
            .dashboardUid(DATABASE_4_DASHBOARD_UID)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .cid(CONTAINER_4_ID)
            .container(CONTAINER_4)
            .internalName(DATABASE_4_INTERNAL_NAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .created(DATABASE_4_CREATED)
            .lastModified(DATABASE_4_LAST_MODIFIED)
            .ownedBy(USER_4_USERNAME)
            .contactPerson(USER_4_USERNAME)
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final DatabaseAccess DATABASE_4_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_1_USERNAME)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_4_USER_1_READ_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.READ)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_1_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_2_USERNAME)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_4_USER_2_WRITE_OWN_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.WRITE_OWN)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_2_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_3_USERNAME)
            .build();

    public final DatabaseAccess DATABASE_4_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .username(USER_3_USERNAME)
            .build();

    public final at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess DATABASE_4_USER_3_WRITE_ALL_ACCESS_CACHE = at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder()
            .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.WRITE_ALL)
            .username(USER_3_USERNAME)
            .build();

    public final List<IdentifierDto> VIEW_1_DTO_IDENTIFIERS = List.of(IDENTIFIER_3_DTO);

    public final Constraints TABLE_1_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_1)
                    .column(TABLE_1_COLUMNS.get(0))
                    .id(COLUMN_1_1_ID)
                    .build())))
            .build();

    public final ConstraintsDto TABLE_1_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .id(UUID.fromString("b3f40a88-4f21-4de0-a595-3d15e63943aa"))
                    .table(TABLE_1_BRIEF_DTO)
                    .column(TABLE_1_COLUMNS_BRIEF_0_DTO)
                    .build())))
            .build();

    public final Constraints TABLE_2_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>(List.of("`mintemp` > 0")))
            .foreignKeys(new LinkedList<>(List.of(ForeignKey.builder()
                    .id(UUID.fromString("d79f0fb1-05d6-4f3e-a5e2-8559982b8516"))
                    .name("fk_location")
                    .onDelete(ReferenceType.NO_ACTION)
                    .references(new LinkedList<>(List.of(ForeignKeyReference.builder()
                            .id(UUID.fromString("a4da8f2f-2999-4621-8066-801a2fb73c8d"))
                            .column(TABLE_2_COLUMNS.get(2))
                            .referencedColumn(TABLE_1_COLUMNS.get(0))
                            .foreignKey(null) // set later
                            .build())))
                    .table(TABLE_2)
                    .referencedTable(TABLE_1)
                    .onUpdate(ReferenceType.NO_ACTION)
                    .build())))
            .uniques(new LinkedList<>(List.of(Unique.builder()
                    .id(UUID.fromString("408e398f-d157-49a1-8b45-87a070f3b4de"))
                    .table(TABLE_2)
                    .name("uk_1")
                    .columns(new LinkedList<>(List.of(TABLE_2_COLUMNS.get(1))))
                    .build())))
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_2)
                    .column(TABLE_2_COLUMNS.get(0))
                    .id(COLUMN_2_1_ID)
                    .build())))
            .build();

    public final ConstraintsDto TABLE_2_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>(List.of("`mintemp` > 0")))
            .foreignKeys(new LinkedList<>(List.of(ForeignKeyDto.builder()
                    .id(UUID.fromString("d79f0fb1-05d6-4f3e-a5e2-8559982b8516"))
                    .name("fk_location")
                    .onDelete(ReferenceTypeDto.NO_ACTION)
                    .references(new LinkedList<>(List.of(ForeignKeyReferenceDto.builder()
                            .id(UUID.fromString("a4da8f2f-2999-4621-8066-801a2fb73c8d"))
                            .column(TABLE_2_COLUMNS_BRIEF_DTO.get(2))
                            .referencedColumn(TABLE_1_COLUMNS_BRIEF_0_DTO)
                            .foreignKey(null) // set later
                            .build())))
                    .table(TABLE_2_BRIEF_DTO)
                    .referencedTable(TABLE_1_BRIEF_DTO)
                    .onUpdate(ReferenceTypeDto.NO_ACTION)
                    .build())))
            .uniques(new LinkedList<>(List.of(UniqueDto.builder()
                    .id(UUID.fromString("408e398f-d157-49a1-8b45-87a070f3b4de"))
                    .table(TABLE_2_BRIEF_DTO)
                    .name("uk_1")
                    .columns(new LinkedList<>(List.of(TABLE_2_COLUMNS_BRIEF_DTO.get(1))))
                    .build())))
            .primaryKey(new LinkedHashSet<>(List.of(PrimaryKeyDto.builder()
                    .table(TABLE_2_BRIEF_DTO)
                    .column(TABLE_2_COLUMNS_BRIEF_DTO.get(0))
                    .id(COLUMN_2_1_ID)
                    .build())))
            .build();

    public final Constraints TABLE_3_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_3)
                    .column(TABLE_3_COLUMNS.get(0))
                    .id(COLUMN_3_1_ID)
                    .build())))
            .build();

    public final ConstraintsDto TABLE_3_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_3_BRIEF_DTO)
                    .column(TABLE_3_COLUMNS_BRIEF_0_DTO)
                    .id(COLUMN_3_1_ID)
                    .build())))
            .build();

    public final Constraints TABLE_4_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_4)
                    .column(TABLE_4_COLUMNS.get(0))
                    .id(COLUMN_4_1_ID)
                    .build())))
            .build();

    public final ConstraintsDto TABLE_4_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_4_BRIEF_DTO)
                    .column(TABLE_4_COLUMNS_BRIEF_0_DTO)
                    .id(COLUMN_4_1_ID)
                    .build())))
            .build();

    public final Constraints TABLE_5_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_5)
                    .column(TABLE_5_COLUMNS.get(0))
                    .id(COLUMN_5_1_ID)
                    .build())))
            .build();

    public final ConstraintsDto TABLE_5_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_5_BRIEF_DTO)
                    .column(TABLE_5_COLUMNS_BRIEF_0_DTO)
                    .id(COLUMN_5_1_ID)
                    .build())))
            .build();

    public final Constraints TABLE_6_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>(List.of()))
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_6)
                    .column(TABLE_6_COLUMNS.get(0))
                    .id(COLUMN_6_1_ID)
                    .build())))
            .build();

    public final ConstraintsDto TABLE_6_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_6_BRIEF_DTO)
                    .column(TABLE_6_COLUMNS_BRIEF_0_DTO)
                    .id(COLUMN_6_1_ID)
                    .build())))
            .build();

    public final Constraints TABLE_7_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>(List.of(ForeignKey.builder()
                            .id(UUID.fromString("421c3dd8-ae09-4c72-a6ca-09de009e755f"))
                            .name("fk_name_id")
                            .onDelete(ReferenceType.NO_ACTION)
                            .references(new LinkedList<>(List.of(ForeignKeyReference.builder()
                                    .id(UUID.fromString("7c0e4a3c-88b8-4276-8924-403fd122fbf1"))
                                    .column(TABLE_6_COLUMNS.get(0))
                                    .referencedColumn(TABLE_7_COLUMNS.get(0))
                                    .foreignKey(null) // set later
                                    .build())))
                            .table(TABLE_7)
                            .referencedTable(TABLE_6)
                            .onUpdate(ReferenceType.NO_ACTION)
                            .build(),
                    ForeignKey.builder()
                            .id(UUID.fromString("fce75207-6009-49ff-a646-d3e18aed787a"))
                            .name("fk_zoo_id")
                            .onDelete(ReferenceType.NO_ACTION)
                            .references(new LinkedList<>(List.of(ForeignKeyReference.builder()
                                    .id(UUID.fromString("e6cb1daa-a210-41c4-bb79-2c98ef25a02c"))
                                    .column(TABLE_5_COLUMNS.get(0))
                                    .referencedColumn(TABLE_7_COLUMNS.get(1))
                                    .foreignKey(null) // set later
                                    .build())))
                            .table(TABLE_7)
                            .referencedTable(TABLE_5)
                            .onUpdate(ReferenceType.NO_ACTION)
                            .build())))
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_7)
                    .column(TABLE_7_COLUMNS.get(0))
                    .id(COLUMN_7_1_ID)
                    .build())))
            .build();

    public final ForeignKeyDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_0_DTO = ForeignKeyDto.builder()
            .id(UUID.fromString("561b4933-54e5-4dad-a536-39836da87fe3"))
            .name("fk_name_id")
            .onDelete(ReferenceTypeDto.NO_ACTION)
            .references(new LinkedList<>(List.of(ForeignKeyReferenceDto.builder()
                    .id(UUID.fromString("0f4b00c0-f2a8-4929-8619-bdc941b5dc8c"))
                    .column(TABLE_6_COLUMNS_BRIEF_0_DTO)
                    .referencedColumn(TABLE_7_COLUMNS_BRIEF_0_DTO)
                    .foreignKey(null) // set later
                    .build())))
            .table(TABLE_7_BRIEF_DTO)
            .referencedTable(TABLE_6_BRIEF_DTO)
            .onUpdate(ReferenceTypeDto.NO_ACTION)
            .build();

    public final ForeignKeyBriefDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_0_DTO = ForeignKeyBriefDto.builder()
            .id(UUID.fromString("a92f09c5-9bce-4f77-8f7b-a9afc1d30ec2"))
            .build();

    public final ForeignKeyDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_1_DTO = ForeignKeyDto.builder()
            .id(UUID.fromString("f2e82566-ddc3-4b76-8d27-adc3c51780a9"))
            .name("fk_zoo_id")
            .onDelete(ReferenceTypeDto.NO_ACTION)
            .references(new LinkedList<>(List.of(ForeignKeyReferenceDto.builder()
                    .id(UUID.fromString("7a393166-25d2-4b8c-a5e7-7d1b3b33b823"))
                    .column(TABLE_5_COLUMNS_BRIEF_0_DTO)
                    .referencedColumn(TABLE_7_COLUMNS_BRIEF_1_DTO)
                    .foreignKey(null) // set later
                    .build())))
            .table(TABLE_7_BRIEF_DTO)
            .referencedTable(TABLE_5_BRIEF_DTO)
            .onUpdate(ReferenceTypeDto.NO_ACTION)
            .build();

    public final ForeignKeyBriefDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_1_DTO = ForeignKeyBriefDto.builder()
            .id(UUID.fromString("6ce1f707-0bdf-4930-be77-157801d2735a"))
            .build();

    public final ConstraintsDto TABLE_7_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>(List.of(TABLE_7_CONSTRAINTS_FOREIGN_KEY_0_DTO,
                    TABLE_7_CONSTRAINTS_FOREIGN_KEY_1_DTO)))
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_7_BRIEF_DTO)
                    .column(TABLE_7_COLUMNS_BRIEF_0_DTO)
                    .id(UUID.fromString("9969e13f-2a2f-45c7-bccf-a7df0ac813a8"))
                    .build())))
            .build();

    public final Constraints TABLE_8_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_8)
                    .column(TABLE_8_COLUMNS.get(0))
                    .id(UUID.fromString("cd23b601-966c-4aa7-9722-6bcb009200cc"))
                    .build())))
            .build();

    public final ConstraintsDto TABLE_8_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_8_BRIEF_DTO)
                    .column(TABLE_8_COLUMNS_BRIEF_0_DTO)
                    .id(UUID.fromString("c61196d1-a902-405c-a825-0781c0c94df1"))
                    .build())))
            .build();

    public final ExportResourceDto EXPORT_RESOURCE_DTO = ExportResourceDto.builder()
            .filename("68b329da9893e34099c7d8ad5cb9c940")
            .resource(new InputStreamResource(InputStream.nullInputStream()))
            .build();

    public final QueryDto QUERY_1_DTO = QueryDto.builder()
            .id(QUERY_1_ID)
            .databaseId(DATABASE_1_ID)
            .query(QUERY_1_STATEMENT)
            .queryNormalized(QUERY_1_STATEMENT_NORMALIZED)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .execution(QUERY_1_EXECUTION)
            .owner(USER_1_BRIEF_DTO)
            .isPersisted(QUERY_1_PERSISTED)
            .resultNumber(3L)
            .build();

    public final Subset QUERY_1_CACHE = Subset.builder()
            .id(QUERY_1_ID)
            .databaseId(DATABASE_1_ID)
            .query(QUERY_1_STATEMENT)
            .queryNormalized(QUERY_1_STATEMENT_NORMALIZED)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .execution(QUERY_1_EXECUTION)
            .ownedBy(USER_1_USERNAME)
            .isPersisted(QUERY_1_PERSISTED)
            .resultNumber(3L)
            .build();

    public final QueryDto QUERY_2_DTO = QueryDto.builder()
            .id(QUERY_2_ID)
            .databaseId(DATABASE_1_ID)
            .query(QUERY_2_STATEMENT)
            .queryNormalized(QUERY_2_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_2_RESULT_NUMBER)
            .resultHash(QUERY_2_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_2_QUERY_HASH)
            .execution(QUERY_2_EXECUTION)
            .isPersisted(QUERY_2_PERSISTED)
            .resultNumber(3L)
            .build();

    public final QueryDto QUERY_3_DTO = QueryDto.builder()
            .id(QUERY_3_ID)
            .databaseId(DATABASE_1_ID)
            .query(QUERY_3_STATEMENT)
            .queryNormalized(QUERY_3_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_3_RESULT_NUMBER)
            .resultHash(QUERY_3_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_3_QUERY_HASH)
            .execution(QUERY_3_EXECUTION)
            .isPersisted(QUERY_3_PERSISTED)
            .resultNumber(2L)
            .build();

    public final Subset QUERY_3_CACHE = Subset.builder()
            .id(QUERY_3_ID)
            .databaseId(DATABASE_1_ID)
            .query(QUERY_3_STATEMENT)
            .queryNormalized(QUERY_3_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_3_RESULT_NUMBER)
            .resultHash(QUERY_3_RESULT_HASH)
            .ownedBy(USER_1_USERNAME)
            .queryHash(QUERY_3_QUERY_HASH)
            .execution(QUERY_3_EXECUTION)
            .isPersisted(QUERY_3_PERSISTED)
            .resultNumber(2L)
            .build();

    public final QueryDto QUERY_7_DTO = QueryDto.builder()
            .id(QUERY_7_ID)
            .databaseId(DATABASE_4_ID)
            .query(QUERY_7_STATEMENT)
            .queryNormalized(QUERY_7_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_7_RESULT_NUMBER)
            .resultHash(QUERY_7_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_7_QUERY_HASH)
            .execution(QUERY_7_EXECUTION)
            .isPersisted(QUERY_7_PERSISTED)
            .resultNumber(2L)
            .build();

    public final QueryDto QUERY_6_DTO = QueryDto.builder()
            .id(QUERY_6_ID)
            .databaseId(DATABASE_1_ID)
            .query(QUERY_6_STATEMENT)
            .queryNormalized(QUERY_6_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_6_RESULT_NUMBER)
            .resultHash(QUERY_6_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_6_QUERY_HASH)
            .execution(QUERY_6_EXECUTION)
            .isPersisted(QUERY_6_PERSISTED)
            .build();

    public final QueryDto QUERY_8_DTO = QueryDto.builder()
            .id(QUERY_8_ID)
            .databaseId(DATABASE_2_ID)
            .query(QUERY_8_STATEMENT)
            .queryNormalized(QUERY_8_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_8_RESULT_NUMBER)
            .resultHash(QUERY_8_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_8_QUERY_HASH)
            .execution(QUERY_8_EXECUTION)
            .isPersisted(QUERY_8_PERSISTED)
            .resultNumber(3L)
            .build();

    public final Subset QUERY_8_CACHE = Subset.builder()
            .id(QUERY_8_ID)
            .databaseId(DATABASE_2_ID)
            .query(QUERY_8_STATEMENT)
            .queryNormalized(QUERY_8_STATEMENT_NORMALIZED)
            .resultNumber(QUERY_8_RESULT_NUMBER)
            .resultHash(QUERY_8_RESULT_HASH)
            .ownedBy(USER_1_USERNAME)
            .queryHash(QUERY_8_QUERY_HASH)
            .execution(QUERY_8_EXECUTION)
            .isPersisted(QUERY_8_PERSISTED)
            .resultNumber(3L)
            .build();

    public BaseTest() {
        IMAGE_1.setOperators(new LinkedList<>(IMAGE_1_OPERATORS));
        IMAGE_1.setDataTypes(new LinkedList<>(IMAGE_1_DATA_TYPES));
        IMAGE_1_DTO.setOperators(IMAGE_1_OPERATORS_DTO);
        IMAGE_1_DTO.setDataTypes(IMAGE_1_DATA_TYPES_DTO);
        IMAGE_1_CACHE.setOperators(IMAGE_1_OPERATORS_CACHE);
        IMAGE_1_CACHE.setDataTypes(IMAGE_1_DATA_TYPES_CACHE);
        CONTAINER_1.setImage(IMAGE_1);
        CONTAINER_1_DTO.setImage(IMAGE_1_DTO);
        CONTAINER_1_CACHE.setImage(IMAGE_1_CACHE);
        CONTAINER_1.setDatabases(new LinkedList<>(List.of(DATABASE_1, DATABASE_2, DATABASE_3)));
        CONTAINER_4.setDatabases(new LinkedList<>(List.of(DATABASE_4)));
        /* DATABASE 1 */
        DATABASE_1.setSubsets(new LinkedList<>());
        DATABASE_1.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS, DATABASE_1_USER_2_WRITE_OWN_ACCESS, DATABASE_1_USER_3_WRITE_ALL_ACCESS)));
        DATABASE_1_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS_DTO, DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO, DATABASE_1_USER_3_WRITE_ALL_ACCESS_DTO)));
        DATABASE_1_CACHE.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS_CACHE, DATABASE_1_USER_2_WRITE_OWN_ACCESS_CACHE, DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE)));
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_1.setColumns(new LinkedList<>(TABLE_1_COLUMNS));
        TABLE_1.setConstraints(TABLE_1_CONSTRAINTS);
        TABLE_1_CACHE.setColumns(TABLE_1_COLUMNS_CACHE);
        DATABASE_1.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4)));
        IDENTIFIER_1.setDatabase(DATABASE_1);
        IDENTIFIER_2.setDatabase(DATABASE_1);
        IDENTIFIER_3.setDatabase(DATABASE_1);
        IDENTIFIER_4.setDatabase(DATABASE_1);
        DATABASE_1.setTables(new LinkedList<>(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_4)));
        DATABASE_1.setViews(new LinkedList<>(List.of(VIEW_1, VIEW_2, VIEW_3)));
        DATABASE_1_DTO.setContainer(CONTAINER_1_DTO);
        DATABASE_1_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_1_DTO, IDENTIFIER_2_DTO, IDENTIFIER_3_DTO, IDENTIFIER_4_DTO)));
        DATABASE_1_DTO.setTables(new LinkedList<>(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO)));
        DATABASE_1_DTO.setViews(new LinkedList<>(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO)));
        TABLE_1_DTO.setConstraints(TABLE_1_CONSTRAINTS_DTO);
        TABLE_1_DTO.setColumns(new LinkedList<>(TABLE_1_COLUMNS_DTO));
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_2.setColumns(new LinkedList<>(TABLE_2_COLUMNS));
        TABLE_2.setConstraints(TABLE_2_CONSTRAINTS);
        TABLE_2_CACHE.setColumns(TABLE_2_COLUMNS_CACHE);
        TABLE_2_CONSTRAINTS.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_2_CONSTRAINTS.getForeignKeys().get(0));
        TABLE_2_DTO.setColumns(new LinkedList<>(TABLE_2_COLUMNS_DTO));
        TABLE_2_DTO.setConstraints(TABLE_2_CONSTRAINTS_DTO);
        TABLE_3.setDatabase(DATABASE_1);
        TABLE_3.setColumns(new LinkedList<>(TABLE_3_COLUMNS));
        TABLE_3.setConstraints(TABLE_3_CONSTRAINTS);
        TABLE_3_CACHE.setColumns(TABLE_3_COLUMNS_CACHE);
        TABLE_3_DTO.setColumns(new LinkedList<>(TABLE_3_COLUMNS_DTO));
        TABLE_3_DTO.setConstraints(TABLE_3_CONSTRAINTS_DTO);
        TABLE_4.setDatabase(DATABASE_1);
        TABLE_4.setColumns(new LinkedList<>(TABLE_4_COLUMNS));
        TABLE_4.setConstraints(TABLE_4_CONSTRAINTS);
        TABLE_4_CACHE.setColumns(TABLE_4_COLUMNS_CACHE);
        TABLE_4_DTO.setColumns(TABLE_4_COLUMNS_DTO);
        TABLE_4_DTO.setConstraints(TABLE_4_CONSTRAINTS_DTO);
        VIEW_1.setDatabase(DATABASE_1);
        VIEW_1.setColumns(new LinkedList<>(VIEW_1_COLUMNS));
        VIEW_1.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_3)));
        VIEW_1_DTO.setIdentifiers(VIEW_1_DTO_IDENTIFIERS);
        VIEW_1_CACHE.setColumns(VIEW_1_COLUMNS_CACHE);
        VIEW_2.setDatabase(DATABASE_1);
        VIEW_2.setColumns(new LinkedList<>(VIEW_2_COLUMNS));
        VIEW_2_CACHE.setColumns(new LinkedList<>(VIEW_2_COLUMNS_CACHE));
        VIEW_3.setDatabase(DATABASE_1);
        VIEW_3.setColumns(new LinkedList<>(VIEW_3_COLUMNS));
        VIEW_3_CACHE.setColumns(new LinkedList<>(VIEW_3_COLUMNS_CACHE));
        IDENTIFIER_1.setDatabase(DATABASE_1);
        IDENTIFIER_1_CREATOR_1.setIdentifier(IDENTIFIER_1);
        IDENTIFIER_1_TITLE_1.setIdentifier(IDENTIFIER_1);
        IDENTIFIER_1_TITLE_2.setIdentifier(IDENTIFIER_1);
        IDENTIFIER_1_DESCRIPTION_1.setIdentifier(IDENTIFIER_1);
        IDENTIFIER_1_FUNDER_1.setIdentifier(IDENTIFIER_1);
        IDENTIFIER_2.setDatabase(DATABASE_1);
        IDENTIFIER_2_CREATOR_1.setIdentifier(IDENTIFIER_2);
        IDENTIFIER_2_TITLE_1.setIdentifier(IDENTIFIER_2);
        IDENTIFIER_2_DESCRIPTION_1.setIdentifier(IDENTIFIER_2);
        IDENTIFIER_3.setDatabase(DATABASE_1);
        IDENTIFIER_3_CREATOR_1.setIdentifier(IDENTIFIER_3);
        IDENTIFIER_3_TITLE_1.setIdentifier(IDENTIFIER_3);
        IDENTIFIER_3_DESCRIPTION_1.setIdentifier(IDENTIFIER_3);
        IDENTIFIER_4.setDatabase(DATABASE_1);
        IDENTIFIER_4_CREATOR_1.setIdentifier(IDENTIFIER_4);
        IDENTIFIER_4_TITLE_1.setIdentifier(IDENTIFIER_4);
        IDENTIFIER_4_DESCRIPTION_1.setIdentifier(IDENTIFIER_4);
        /* DATABASE 2 */
        VIEW_4.setDatabase(DATABASE_2);
        VIEW_4.setColumns(new LinkedList<>(VIEW_4_COLUMNS));
        VIEW_4_CACHE.setColumns(new LinkedList<>(VIEW_4_COLUMNS_CACHE));
        DATABASE_2.setAccesses(new LinkedList<>(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS, DATABASE_2_USER_3_READ_ACCESS)));
        DATABASE_2.setTables(new LinkedList<>(List.of(TABLE_5, TABLE_6, TABLE_7)));
        DATABASE_2.setViews(new LinkedList<>(List.of(VIEW_4)));
        DATABASE_2.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_5)));
        DATABASE_2_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS_DTO, DATABASE_2_USER_3_READ_ACCESS_DTO)));
        DATABASE_2_DTO.setTables(new LinkedList<>(List.of(TABLE_5_DTO, TABLE_6_DTO, TABLE_7_DTO)));
        DATABASE_2_DTO.setViews(new LinkedList<>(List.of(VIEW_4_DTO)));
        DATABASE_2_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_5_DTO)));
        DATABASE_2_CACHE.setAccesses(new LinkedList<>(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS_CACHE, DATABASE_2_USER_3_READ_ACCESS_CACHE)));
        TABLE_5.setDatabase(DATABASE_2);
        TABLE_5.setColumns(new LinkedList<>(TABLE_5_COLUMNS));
        TABLE_5.setConstraints(TABLE_5_CONSTRAINTS);
        TABLE_5_DTO.setColumns(new LinkedList<>(TABLE_5_COLUMNS_DTO));
        TABLE_5_DTO.setConstraints(TABLE_5_CONSTRAINTS_DTO);
        TABLE_5_CACHE.setColumns(TABLE_5_COLUMNS_CACHE);
        TABLE_6.setDatabase(DATABASE_2);
        TABLE_6.setColumns(new LinkedList<>(TABLE_6_COLUMNS));
        TABLE_6.setConstraints(TABLE_6_CONSTRAINTS);
        TABLE_6_CACHE.setColumns(new LinkedList<>(TABLE_6_COLUMNS_CACHE));
        TABLE_7.setDatabase(DATABASE_2);
        TABLE_7.setColumns(new LinkedList<>(TABLE_7_COLUMNS));
        TABLE_7.setConstraints(TABLE_7_CONSTRAINTS);
        TABLE_7_CONSTRAINTS.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS.getForeignKeys().get(0));
        TABLE_7_CONSTRAINTS.getForeignKeys().get(1).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS.getForeignKeys().get(1));
        TABLE_7_DTO.setColumns(TABLE_7_COLUMNS_DTO);
        TABLE_7_DTO.setConstraints(TABLE_7_CONSTRAINTS_DTO);
        TABLE_7_CACHE.setColumns(new LinkedList<>(TABLE_7_COLUMNS_CACHE));
        TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_0_DTO);
        TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(1).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_1_DTO);
        IDENTIFIER_5.setDatabase(DATABASE_2);
        IDENTIFIER_5_CREATOR_1.setIdentifier(IDENTIFIER_5);
        IDENTIFIER_5_CREATOR_2.setIdentifier(IDENTIFIER_5);
        IDENTIFIER_5_TITLE_1.setIdentifier(IDENTIFIER_5);
        IDENTIFIER_5_DESCRIPTION_1.setIdentifier(IDENTIFIER_5);
        /* DATABASE 3 */
        DATABASE_3.setTables(new LinkedList<>(List.of(TABLE_8)));
        DATABASE_3.setViews(new LinkedList<>(List.of(VIEW_5)));
        DATABASE_3.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_6)));
        DATABASE_3.setAccesses(new LinkedList<>(List.of(DATABASE_3_USER_1_WRITE_ALL_ACCESS)));
        DATABASE_3_DTO.setTables(new LinkedList<>(List.of(TABLE_8_DTO)));
        DATABASE_3_DTO.setViews(new LinkedList<>(List.of(VIEW_5_DTO)));
        DATABASE_3_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_6_DTO)));
        DATABASE_3_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_3_USER_1_WRITE_ALL_ACCESS_DTO)));
        DATABASE_3_CACHE.setAccesses(new LinkedList<>(List.of(DATABASE_3_USER_1_WRITE_ALL_ACCESS_CACHE)));
        TABLE_8.setDatabase(DATABASE_3);
        TABLE_8.setColumns(new LinkedList<>(TABLE_8_COLUMNS));
        TABLE_8.setConstraints(TABLE_8_CONSTRAINTS);
        TABLE_8_DTO.setColumns(new LinkedList<>(TABLE_8_COLUMNS_DTO));
        TABLE_8_DTO.setConstraints(TABLE_8_CONSTRAINTS_DTO);
        TABLE_8_CACHE.setColumns(new LinkedList<>(TABLE_8_COLUMNS_CACHE));
        VIEW_5.setDatabase(DATABASE_3);
        VIEW_5.setColumns(VIEW_5_COLUMNS);
        VIEW_5_DTO.setColumns(VIEW_5_COLUMNS_DTO);
        IDENTIFIER_6.setDatabase(DATABASE_3);
        IDENTIFIER_6_CREATOR_1.setIdentifier(IDENTIFIER_6);
        IDENTIFIER_6_CREATOR_2.setIdentifier(IDENTIFIER_6);
        IDENTIFIER_6_TITLE_1.setIdentifier(IDENTIFIER_6);
        IDENTIFIER_6_DESCRIPTION_1.setIdentifier(IDENTIFIER_6);
        /* DATABASE 4 */
        DATABASE_4.setAccesses(new LinkedList<>(List.of(DATABASE_4_USER_1_READ_ACCESS, DATABASE_4_USER_2_WRITE_OWN_ACCESS, DATABASE_4_USER_3_WRITE_ALL_ACCESS)));
        DATABASE_4.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_7)));
        DATABASE_4_DTO.setTables(new LinkedList<>(List.of(TABLE_9_DTO)));
        DATABASE_4_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_7_DTO)));
        DATABASE_4_CACHE.setAccesses(new LinkedList<>(List.of(DATABASE_4_USER_1_READ_ACCESS_CACHE, DATABASE_4_USER_2_WRITE_OWN_ACCESS_CACHE, DATABASE_4_USER_3_WRITE_ALL_ACCESS_CACHE)));
        TABLE_9.setDatabase(DATABASE_4);
        TABLE_9.setColumns(TABLE_9_COLUMNS);
        TABLE_9.setConstraints(TABLE_9_CONSTRAINTS);
        TABLE_9_DTO.setColumns(TABLE_9_COLUMNS_DTO);
        TABLE_9_DTO.setConstraints(TABLE_9_CONSTRAINTS_DTO);
        IDENTIFIER_7.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_7.setDatabase(DATABASE_4);
        IDENTIFIER_7_CREATOR_1.setIdentifier(IDENTIFIER_7);
        IDENTIFIER_7_TITLE_1.setIdentifier(IDENTIFIER_7);
        IDENTIFIER_7_DESCRIPTION_1.setIdentifier(IDENTIFIER_7);
    }

}