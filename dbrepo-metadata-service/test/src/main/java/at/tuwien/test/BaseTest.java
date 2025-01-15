package at.tuwien.test;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.amqp.QueueDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.auth.RefreshTokenRequestDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.*;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.*;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.api.database.table.columns.*;
import at.tuwien.api.database.table.columns.concepts.*;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.foreign.*;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.DataCiteData;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.api.identifier.*;
import at.tuwien.api.keycloak.*;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageDto;
import at.tuwien.api.maintenance.BannerMessageTypeDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.api.orcid.activities.OrcidActivitiesSummaryDto;
import at.tuwien.api.orcid.activities.employments.OrcidEmploymentsDto;
import at.tuwien.api.orcid.activities.employments.affiliation.OrcidAffiliationGroupDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.OrcidEmploymentSummaryDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.OrcidSummaryDto;
import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.OrcidOrganizationDto;
import at.tuwien.api.orcid.person.OrcidPersonDto;
import at.tuwien.api.orcid.person.name.OrcidNameDto;
import at.tuwien.api.orcid.person.name.OrcidValueDto;
import at.tuwien.api.semantics.*;
import at.tuwien.api.user.UserAttributesDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.api.user.*;
import at.tuwien.api.user.internal.PrivilegedUserDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.Operator;
import at.tuwien.entities.database.*;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnType;
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
import at.tuwien.test.utils.ArrayUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Database 1 (Private Data, Private Schema, User 1) -> Container 1
 * <ul>
 * <li>Table 1 (Private Data, Private Schema)</li>
 * <li>Table 2 (Private Data, Public Schema)</li>
 * <li>Table 3</li>
 * <li>Table 4</li>
 * <li>Query 1</li>
 * <li>View 1</li>
 * <li>View 2</li>
 * <li>View 3</li>
 * <li>Identifier 1 (Title=en, Description=en, type=database)</li>
 * <li>Identifier 2 (Title=en, Description=en, type=subset, queryId=1)</li>
 * <li>Identifier 3 (Title=en, Description=en, type=view, viewId=1)</li>
 * <li>Identifier 4 (Title=en, Description=en, type=table, tableId=1)</li>
 * </ul>
 * <p>
 * Database 2 (Private Data, Public Schema, User 2) -> Container 1
 * <ul>
 * <li>Table 5</li>
 * <li>Table 6</li>
 * <li>Table 7</li>
 * <li>Query 2</li>
 * <li>Query 6</li>
 * <li>View 4</li>
 * <li>Identifier 5 (Title=de, Description=de)</li>
 * </ul>
 * <p>
 * Database 3 (Public Data, Private Schema, User 3) -> Container 1
 * <ul>
 * <li>Table 8</li>
 * <li>Query 3</li>
 * <li>Query 4</li>
 * <li>Query 5</li>
 * <li>View 5</li>
 * <li>Identifier 6 (Title=en, Description=en, Query=3)</li>
 * </ul>
 * <p>
 * Database 4 (Public Data, Public Schema, User 4) -> Container 4
 * <li>Table 9</li>
 * <li>Identifier 7</li>
 * <li>Query 7</li>
 * <ul>
 * </ul>
 * <br />
 * User 1 (read)
 * <br />
 * User 2 (write-own)
 * <br />
 * User 3 (write-all)
 */
public abstract class BaseTest {

    public final static String MINIO_IMAGE = "minio/minio:RELEASE.2024-06-06T09-36-42Z";

    public final static String MARIADB_IMAGE = "mariadb:11.3.2";

    public final static String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:24.0";

    public final static String[] DEFAULT_SEMANTICS_HANDLING = new String[]{"default-semantics-handling",
            "create-semantic-unit", "execute-semantic-query", "table-semantic-analyse", "create-semantic-concept"};

    public final static String[] DEFAULT_VIEW_HANDLING = new String[]{"update-database-view", "create-database-view",
            "delete-database-view", "list-database-views", "modify-view-visibility", "find-database-view"};

    public final static String[] ESCALATED_SEMANTICS_HANDLING = new String[]{"escalated-semantics-handling",
            "update-semantic-concept", "modify-foreign-table-column-semantics", "delete-ontology", "list-ontologies",
            "update-semantic-unit", "create-ontology", "update-ontology"};

    public final static String[] DEFAULT_CONTAINER_HANDLING = new String[]{"default-container-handling",
            "create-container", "list-containers", "modify-container-state", "find-container"};

    public final static String[] ESCALATED_CONTAINER_HANDLING = new String[]{"escalated-container-handling",
            "modify-foreign-container-state", "delete-container"};

    public final static String[] DEFAULT_DATABASE_HANDLING = new String[]{"default-database-handling",
            "update-database-access", "modify-database-visibility", "create-database", "modify-database-owner",
            "delete-database-access", "check-database-access", "list-databases", "modify-database-image",
            "create-database-access", "find-database", "import-database-data"};

    public final static String[] ESCALATED_DATABASE_HANDLING = new String[]{"escalated-database-handling",
            "delete-database"};

    public final static String[] DEFAULT_IDENTIFIER_HANDLING = new String[]{"default-identifier-handling",
            "create-identifier", "find-identifier", "list-identifiers", "publish-identifier", "delete-identifier"};

    public final static String[] ESCALATED_IDENTIFIER_HANDLING = new String[]{"escalated-identifier-handling",
            "modify-identifier-metadata", "update-foreign-identifier", "create-foreign-identifier"};

    public final static String[] DEFAULT_QUERY_HANDLING = new String[]{"default-query-handling", "view-table-data",
            "execute-query", "view-table-history", "list-database-views", "list-queries", "view-database-view-data",
            "export-query-data", "find-query", "create-database-view", "delete-database-view", "delete-table-data",
            "export-table-data", "persist-query", "re-execute-query", "insert-table-data", "find-database-view"};

    public final static String[] ESCALATED_QUERY_HANDLING = new String[]{"escalated-query-handling"};

    public final static String[] DEFAULT_TABLE_HANDLING = new String[]{"default-table-handling",
            "list-tables", "create-table", "modify-table-column-semantics", "find-table", "delete-table",
            "update-table-statistic", "update-table"};

    public final static String[] ESCALATED_TABLE_HANDLING = new String[]{"escalated-table-handling",
            "delete-foreign-table"};

    public final static String[] DEFAULT_USER_HANDLING = new String[]{"default-user-handling", "modify-user-theme",
            "modify-user-information"};

    public final static String[] ESCALATED_USER_HANDLING = new String[]{"escalated-user-handling", "find-user"};

    public final static String[] DEFAULT_RESEARCHER_ROLES = ArrayUtils.merge(List.of(new String[]{"default-researcher-roles"},
            DEFAULT_CONTAINER_HANDLING, DEFAULT_DATABASE_HANDLING, DEFAULT_IDENTIFIER_HANDLING, DEFAULT_QUERY_HANDLING,
            DEFAULT_TABLE_HANDLING, DEFAULT_USER_HANDLING, DEFAULT_SEMANTICS_HANDLING, DEFAULT_VIEW_HANDLING));

    public final static String[] DEFAULT_DEVELOPER_ROLES = ArrayUtils.merge(List.of(new String[]{"default-developer-roles"},
            DEFAULT_CONTAINER_HANDLING, DEFAULT_DATABASE_HANDLING, DEFAULT_IDENTIFIER_HANDLING, DEFAULT_QUERY_HANDLING,
            DEFAULT_TABLE_HANDLING, DEFAULT_USER_HANDLING, ESCALATED_USER_HANDLING, ESCALATED_CONTAINER_HANDLING,
            ESCALATED_DATABASE_HANDLING, ESCALATED_IDENTIFIER_HANDLING, ESCALATED_QUERY_HANDLING,
            ESCALATED_TABLE_HANDLING, DEFAULT_VIEW_HANDLING));

    public final static String[] DEFAULT_DATA_STEWARD_ROLES = ArrayUtils.merge(List.of(new String[]{"default-data-steward-roles"},
            ESCALATED_IDENTIFIER_HANDLING, DEFAULT_SEMANTICS_HANDLING, ESCALATED_SEMANTICS_HANDLING, DEFAULT_VIEW_HANDLING));

    public final static String[] DEFAULT_LOCAL_ADMIN_ROLES = new String[]{"admin"};

    public final static List<GrantedAuthorityDto> AUTHORITY_LOCAL_ADMIN_ROLES = Arrays.stream(DEFAULT_LOCAL_ADMIN_ROLES)
            .map(GrantedAuthorityDto::new)
            .collect(Collectors.toList());

    public final static List<GrantedAuthorityDto> AUTHORITY_DEFAULT_RESEARCHER_ROLES = Arrays.stream(DEFAULT_RESEARCHER_ROLES)
            .map(GrantedAuthorityDto::new)
            .collect(Collectors.toList());

    public final static List<GrantedAuthorityDto> AUTHORITY_DEFAULT_DEVELOPER_ROLES = Arrays.stream(DEFAULT_DEVELOPER_ROLES)
            .map(GrantedAuthorityDto::new)
            .collect(Collectors.toList());

    public final static List<GrantedAuthorityDto> AUTHORITY_DEFAULT_DATA_STEWARD_ROLES = Arrays.stream(DEFAULT_DATA_STEWARD_ROLES)
            .map(GrantedAuthorityDto::new)
            .collect(Collectors.toList());

    public final static List<GrantedAuthority> AUTHORITY_DEFAULT_LOCAL_ADMIN_AUTHORITIES = AUTHORITY_LOCAL_ADMIN_ROLES.stream()
            .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
            .collect(Collectors.toList());

    public final static List<GrantedAuthority> AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES = AUTHORITY_DEFAULT_RESEARCHER_ROLES.stream()
            .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
            .collect(Collectors.toList());

    public final static List<GrantedAuthority> AUTHORITY_DEFAULT_DEVELOPER_AUTHORITIES = AUTHORITY_DEFAULT_DEVELOPER_ROLES.stream()
            .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
            .collect(Collectors.toList());

    public final static List<GrantedAuthority> AUTHORITY_DEFAULT_DATA_STEWARD_AUTHORITIES = AUTHORITY_DEFAULT_DATA_STEWARD_ROLES.stream()
            .map(a -> new SimpleGrantedAuthority(a.getAuthority()))
            .collect(Collectors.toList());

    public final static UUID REALM_DBREPO_ID = UUID.fromString("6264bf7b-d1d3-4562-9c07-ce4364a8f9d3");
    public final static String REALM_DBREPO_NAME = "dbrepo";
    public final static Boolean REALM_DBREPO_ENABLED = true;

    public final static UUID ROLE_DEFAULT_REALM_DBREPO_ROLES_ID = UUID.fromString("c74cbbe7-3ab1-4472-9211-cc904567268");
    public final static String ROLE_DEFAULT_REALM_DBREPO_ROLES_NAME = "default-dbrepo-roles";
    public final static UUID ROLE_DEFAULT_REALM_DBREPO_ROLES_REALM_ID = REALM_DBREPO_ID;

    public final static UUID ROLE_DEFAULT_RESEARCHER_ROLES_ID = UUID.fromString("c74cbbe7-3ab1-4472-9211-cc9045672682");
    public final static String ROLE_DEFAULT_RESEARCHER_ROLES_NAME = "default-researcher-roles";
    public final static UUID ROLE_DEFAULT_RESEARCHER_ROLES_REALM_ID = REALM_DBREPO_ID;

    public final static UpdateDatabaseAccessDto UPDATE_DATABASE_ACCESS_READ_DTO = UpdateDatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .build();

    public final static UpdateDatabaseAccessDto UPDATE_DATABASE_ACCESS_WRITE_OWN_DTO = UpdateDatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .build();

    public final static UpdateDatabaseAccessDto UPDATE_DATABASE_ACCESS_WRITE_ALL_DTO = UpdateDatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .build();

    public final static String TOKEN_ACCESS_TOKEN = "ey.yee.skrr";
    public final static String TOKEN_ACCESS_SCOPE = "openid";

    public final static TokenDto TOKEN_DTO = TokenDto.builder()
            .accessToken(TOKEN_ACCESS_TOKEN)
            .scope(TOKEN_ACCESS_SCOPE)
            .build();

    public final static RefreshTokenRequestDto REFRESH_TOKEN_REQUEST_DTO = RefreshTokenRequestDto.builder()
            .refreshToken("ey.yee.skrr")
            .build();

    public final static Long CONCEPT_1_ID = 1L;
    public final static String CONCEPT_1_NAME = "precipitation";
    public final static String CONCEPT_1_URI = "http://www.wikidata.org/entity/Q25257";
    public final static String CONCEPT_1_DESCRIPTION = null;
    public final static Instant CONCEPT_1_CREATED = Instant.ofEpochSecond(1701976048L) /* 2023-12-07 19:07:27 (UTC) */;

    public final static ConceptSaveDto CONCEPT_1_SAVE_DTO = ConceptSaveDto.builder()
            .uri(CONCEPT_1_URI)
            .name(CONCEPT_1_NAME)
            .description(CONCEPT_1_DESCRIPTION)
            .build();

    public final static ConceptDto CONCEPT_1_DTO = ConceptDto.builder()
            .id(CONCEPT_1_ID)
            .uri(CONCEPT_1_URI)
            .name(CONCEPT_1_NAME)
            .description(CONCEPT_1_DESCRIPTION)
            .build();

    public final static ConceptBriefDto CONCEPT_1_BRIEF_DTO = ConceptBriefDto.builder()
            .id(CONCEPT_1_ID)
            .uri(CONCEPT_1_URI)
            .name(CONCEPT_1_NAME)
            .description(CONCEPT_1_DESCRIPTION)
            .build();

    public final static TableColumnConcept CONCEPT_1 = TableColumnConcept.builder()
            .id(CONCEPT_1_ID)
            .uri(CONCEPT_1_URI)
            .name(CONCEPT_1_NAME)
            .description(CONCEPT_1_DESCRIPTION)
            .created(CONCEPT_1_CREATED)
            .build();

    public final static EntityDto CONCEPT_1_ENTITY_DTO = EntityDto.builder()
            .uri(CONCEPT_1_URI)
            .description(CONCEPT_1_DESCRIPTION)
            .label(CONCEPT_1_NAME)
            .build();

    public final static Long CONCEPT_2_ID = 2L;
    public final static String CONCEPT_2_NAME = "FAIR data";
    public final static String CONCEPT_2_URI = "http://www.wikidata.org/entity/Q29032648";
    public final static String CONCEPT_2_DESCRIPTION = "data compliant with the terms of the FAIR Data Principles";
    public final static Instant CONCEPT_2_CREATED = Instant.now();

    public final static ConceptSaveDto CONCEPT_2_SAVE_DTO = ConceptSaveDto.builder()
            .uri(CONCEPT_2_URI)
            .name(CONCEPT_2_NAME)
            .description(CONCEPT_2_DESCRIPTION)
            .build();

    public final static ConceptDto CONCEPT_2_DTO = ConceptDto.builder()
            .id(CONCEPT_2_ID)
            .uri(CONCEPT_2_URI)
            .name(CONCEPT_2_NAME)
            .description(CONCEPT_2_DESCRIPTION)
            .build();

    public final static ConceptBriefDto CONCEPT_2_BRIEF_DTO = ConceptBriefDto.builder()
            .id(CONCEPT_2_ID)
            .uri(CONCEPT_2_URI)
            .name(CONCEPT_2_NAME)
            .description(CONCEPT_2_DESCRIPTION)
            .build();

    public final static TableColumnConcept CONCEPT_2 = TableColumnConcept.builder()
            .id(CONCEPT_2_ID)
            .uri(CONCEPT_2_URI)
            .name(CONCEPT_2_NAME)
            .description(CONCEPT_2_DESCRIPTION)
            .created(CONCEPT_2_CREATED)
            .build();

    public final static Long UNIT_1_ID = 1L;
    public final static String UNIT_1_NAME = "millimetre";
    public final static String UNIT_1_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/millimetre";
    public final static String UNIT_1_DESCRIPTION = "The millimetre is a unit of length defined as 1.0e-3 metre.";
    public final static Instant UNIT_1_CREATED = Instant.ofEpochSecond(1701976282L) /* 2023-12-07 19:11:22 */;

    public final static UnitSaveDto UNIT_1_SAVE_DTO = UnitSaveDto.builder()
            .uri(UNIT_1_URI)
            .name(UNIT_1_NAME)
            .description(UNIT_1_DESCRIPTION)
            .build();

    public final static UnitDto UNIT_1_DTO = UnitDto.builder()
            .id(UNIT_1_ID)
            .uri(UNIT_1_URI)
            .name(UNIT_1_NAME)
            .description(UNIT_1_DESCRIPTION)
            .build();

    public final static UnitBriefDto UNIT_1_BRIEF_DTO = UnitBriefDto.builder()
            .id(UNIT_1_ID)
            .uri(UNIT_1_URI)
            .name(UNIT_1_NAME)
            .description(UNIT_1_DESCRIPTION)
            .build();

    public final static TableColumnUnit UNIT_1 = TableColumnUnit.builder()
            .id(UNIT_1_ID)
            .uri(UNIT_1_URI)
            .name(UNIT_1_NAME)
            .description(UNIT_1_DESCRIPTION)
            .created(UNIT_1_CREATED)
            .build();

    public final static EntityDto UNIT_1_ENTITY_DTO = EntityDto.builder()
            .uri(UNIT_1_URI)
            .description(UNIT_1_DESCRIPTION)
            .label(UNIT_1_NAME)
            .build();

    public final static Long UNIT_2_ID = 2L;
    public final static String UNIT_2_NAME = "tonne";
    public final static String UNIT_2_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/tonne";
    public final static String UNIT_2_DESCRIPTION = "The tonne is a unit of mass defined as 1000 kilogram.";
    public final static Instant UNIT_2_CREATED = Instant.ofEpochSecond(1701976462L) /* 2023-12-07 19:14:22 */;

    public final static UnitSaveDto UNIT_2_SAVE_DTO = UnitSaveDto.builder()
            .uri(UNIT_2_URI)
            .name(UNIT_2_NAME)
            .description(UNIT_2_DESCRIPTION)
            .build();

    public final static UnitDto UNIT_2_DTO = UnitDto.builder()
            .id(UNIT_2_ID)
            .uri(UNIT_2_URI)
            .name(UNIT_2_NAME)
            .description(UNIT_2_DESCRIPTION)
            .build();

    public final static UnitBriefDto UNIT_2_BRIEF_DTO = UnitBriefDto.builder()
            .id(UNIT_2_ID)
            .uri(UNIT_2_URI)
            .name(UNIT_2_NAME)
            .description(UNIT_2_DESCRIPTION)
            .build();

    public final static TableColumnUnit UNIT_2 = TableColumnUnit.builder()
            .id(UNIT_2_ID)
            .uri(UNIT_2_URI)
            .name(UNIT_2_NAME)
            .description(UNIT_2_DESCRIPTION)
            .created(UNIT_2_CREATED)
            .build();

    public final static String USER_BROKER_USERNAME = "guest";
    @SuppressWarnings("java:S2068")
    public final static String USER_BROKER_PASSWORD = "guest";

    public final static UUID USER_LOCAL_ADMIN_ID = UUID.fromString("a54dcb2e-a644-4e82-87e7-05a96413983d");
    public final static String USER_LOCAL_ADMIN_USERNAME = "admin";
    @SuppressWarnings("java:S2068")
    public final static String USER_LOCAL_ADMIN_PASSWORD = "admin";
    public final static String USER_LOCAL_ADMIN_THEME = "dark";
    public final static Boolean USER_LOCAL_ADMIN_IS_INTERNAL = true;
    public final static Boolean USER_LOCAL_ADMIN_ENABLED = true;
    public final static String USER_LOCAL_ADMIN_EMAIL = "admin@local";
    @SuppressWarnings("java:S2068")
    public final static String USER_LOCAL_ADMIN_MARIADB_PASSWORD = "*440BA4FD1A87A0999647DB67C0EE258198B247BA";

    public final static LoginRequestDto USER_LOCAL_ADMIN_LOGIN_REQUEST_DTO = LoginRequestDto.builder()
            .username(USER_LOCAL_ADMIN_USERNAME)
            .password(USER_LOCAL_ADMIN_PASSWORD)
            .build();

    public final static UserDetails USER_LOCAL_ADMIN_DETAILS = UserDetailsDto.builder()
            .id(String.valueOf(USER_LOCAL_ADMIN_ID))
            .username(USER_LOCAL_ADMIN_USERNAME)
            .password(USER_LOCAL_ADMIN_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_LOCAL_ADMIN_AUTHORITIES)
            .build();

    public final static User USER_LOCAL = User.builder()
            .id(USER_LOCAL_ADMIN_ID)
            .username(USER_LOCAL_ADMIN_USERNAME)
            .email(USER_LOCAL_ADMIN_EMAIL)
            .mariadbPassword(USER_LOCAL_ADMIN_MARIADB_PASSWORD)
            .theme(USER_LOCAL_ADMIN_THEME)
            .isInternal(USER_LOCAL_ADMIN_IS_INTERNAL)
            .build();

    public final static Principal USER_LOCAL_ADMIN_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_LOCAL_ADMIN_DETAILS,
            USER_LOCAL_ADMIN_PASSWORD, USER_LOCAL_ADMIN_DETAILS.getAuthorities());

    public final static UUID USER_1_ID = UUID.fromString("cd5bab0d-7799-4069-85fb-c5d738572a0b");
    public final static UUID USER_1_LDAP_ID = UUID.fromString("cd5bab0d-7799-4069-85fb-c5d738572a0b");
    public final static String USER_1_EMAIL = "john.doe@example.com";
    public final static String USER_1_USERNAME = "junit1";
    @SuppressWarnings("java:S2068")
    public final static String USER_1_PASSWORD = "junit1";
    @SuppressWarnings("java:S2068")
    public final static String USER_1_PASSWORD_ENCODED = "$2a$10$0dtdedA/RLTrFbUsvpbUw.I73AXOKeQP3t5UXj96OvnDEaDb3d3M6";
    @SuppressWarnings("java:S2068")
    public final static String USER_1_DATABASE_PASSWORD = "*440BA4FD1A87A0999647DB67C0EE258198B247BA" /* junit1 */;
    public final static String USER_1_FIRSTNAME = "John";
    public final static String USER_1_LASTNAME = "Doe";
    public final static String USER_1_QUALIFIED_NAME = USER_1_FIRSTNAME + " " + USER_1_LASTNAME + " — @" + USER_1_USERNAME;
    public final static String USER_1_NAME = "John Doe";
    public final static String USER_1_AFFILIATION = "TU Graz";
    public final static String USER_1_ORCID_URL = "https://orcid.org/0000-0003-4216-302X";
    public final static String USER_1_TITLES_BEFORE = "Dr.";
    public final static String USER_1_TITLES_AFTER = "MSc BSc";
    public final static Boolean USER_1_VERIFIED = false;
    public final static Boolean USER_1_TOTP = false;
    public final static Long USER_1_NOT_BEFORE = 0L;
    public final static Boolean USER_1_ENABLED = true;
    public final static Boolean USER_1_IS_INTERNAL = false;
    public final static String USER_1_THEME = "light";
    public final static String USER_1_LANGUAGE = "en";
    public final static Instant USER_1_CREATED = Instant.ofEpochSecond(1677399441L) /* 2023-02-26 08:17:21 (UTC) */;
    public final static Instant USER_1_LAST_MODIFIED = USER_1_CREATED;
    public final static UUID USER_1_REALM_ID = REALM_DBREPO_ID;

    public final static UpdateUserPasswordDto USER_1_UPDATE_PASSWORD_DTO = UpdateUserPasswordDto.builder()
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .build();

    public final static UserAttributesDto USER_1_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_1_THEME)
            .orcid(USER_1_ORCID_URL)
            .affiliation(USER_1_AFFILIATION)
            .mariadbPassword(USER_1_DATABASE_PASSWORD)
            .language(USER_1_LANGUAGE)
            .build();

    public final static CredentialDto USER_1_KEYCLOAK_CREDENTIAL_1 = CredentialDto.builder()
            .type(CredentialTypeDto.PASSWORD)
            .temporary(false)
            .value(USER_1_PASSWORD)
            .build();

    public final static CredentialDto USER_LOCAL_KEYCLOAK_CREDENTIAL_1 = CredentialDto.builder()
            .type(CredentialTypeDto.PASSWORD)
            .temporary(false)
            .value(USER_LOCAL_ADMIN_PASSWORD)
            .build();

    public final static UserCreateDto USER_1_KEYCLOAK_SIGNUP_REQUEST = UserCreateDto.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .enabled(USER_1_ENABLED)
            .credentials(new LinkedList<>(List.of(USER_1_KEYCLOAK_CREDENTIAL_1)))
            .attributes(UserCreateAttributesDto.builder()
                    .ldapId(String.valueOf(USER_1_ID))
                    .build())
            .build();

    public final static UserCreateDto USER_LOCAL_KEYCLOAK_SIGNUP_REQUEST = UserCreateDto.builder()
            .username(USER_LOCAL_ADMIN_USERNAME)
            .email(USER_LOCAL_ADMIN_EMAIL)
            .enabled(USER_LOCAL_ADMIN_ENABLED)
            .credentials(new LinkedList<>(List.of(USER_LOCAL_KEYCLOAK_CREDENTIAL_1)))
            .groups(new LinkedList<>(List.of("system")))
            .attributes(UserCreateAttributesDto.builder()
                    .ldapId(String.valueOf(USER_LOCAL_ADMIN_ID))
                    .build())
            .build();

    public final static PrivilegedUserDto USER_1_PRIVILEGED_DTO = PrivilegedUserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .attributes(USER_1_ATTRIBUTES_DTO)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .qualifiedName(USER_1_QUALIFIED_NAME)
            .lastRetrieved(Instant.now())
            .build();

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .affiliation(USER_1_AFFILIATION)
            .orcid(USER_1_ORCID_URL)
            .theme(USER_1_THEME)
            .mariadbPassword(USER_1_DATABASE_PASSWORD)
            .language(USER_1_LANGUAGE)
            .isInternal(USER_1_IS_INTERNAL)
            .build();

    public final static UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .attributes(USER_1_ATTRIBUTES_DTO)
            .name(USER_1_NAME)
            .qualifiedName(USER_1_QUALIFIED_NAME)
            .build();

    public final static UserUpdateDto USER_1_UPDATE_DTO = UserUpdateDto.builder()
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .affiliation(USER_1_AFFILIATION)
            .orcid(USER_1_ORCID_URL)
            .theme(USER_1_THEME)
            .language(USER_1_LANGUAGE)
            .build();

    public final static UserPasswordDto USER_1_PASSWORD_DTO = UserPasswordDto.builder()
            .password(USER_1_PASSWORD)
            .build();

    public final static at.tuwien.api.keycloak.UserDto USER_1_KEYCLOAK_DTO = at.tuwien.api.keycloak.UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(USER_1_VERIFIED)
            .notBefore(USER_1_NOT_BEFORE)
            .totp(USER_1_TOTP)
            .attributes(at.tuwien.api.keycloak.UserAttributesDto.builder()
                    .ldapEntryDn(new String[]{"cn=" + USER_1_USERNAME + ",dn=dbrepo,dn=at"})
                    .ldapId(new UUID[]{USER_1_LDAP_ID})
                    .build())
            .build();

    public final static UserBriefDto USER_1_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .name(USER_1_NAME)
            .qualifiedName(USER_1_QUALIFIED_NAME)
            .orcid(USER_1_ORCID_URL)
            .build();

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
            .id(USER_1_ID.toString())
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final static Principal USER_1_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_1_DETAILS,
            USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());

    public final static SignupRequestDto USER_1_SIGNUP_REQUEST_DTO = SignupRequestDto.builder()
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .email(USER_1_EMAIL)
            .build();

    public final static LoginRequestDto USER_1_LOGIN_REQUEST_DTO = LoginRequestDto.builder()
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .build();

    public final static UUID USER_2_ID = UUID.fromString("eeb9a51b-4cd8-4039-90bf-e24f17372f7c");
    public final static UUID USER_2_LDAP_ID = UUID.fromString("eeb9a51b-4cd8-4039-90bf-e24f17372f7c");
    public final static String USER_2_EMAIL = "jane.doe@example.com";
    public final static String USER_2_USERNAME = "junit2";
    public final static String USER_2_FIRSTNAME = "Jane";
    public final static String USER_2_LASTNAME = "Doe";
    public final static String USER_2_NAME = "Jane Doe";
    public final static String USER_2_AFFILIATION = "TU Wien";
    public final static String USER_2_ORCID_URL = "https://orcid.org/0000-0002-9272-6225";
    @SuppressWarnings("java:S2068")
    public final static String USER_2_PASSWORD = "junit2";
    @SuppressWarnings("java:S2068")
    public final static String USER_2_DATABASE_PASSWORD = "*9AA70A8B0EEFAFCB5BED5BDEF6EE264D5DA915AE" /* junit2 */;
    public final static String USER_2_QUALIFIED_NAME = USER_2_FIRSTNAME + " " + USER_2_LASTNAME + " — @" + USER_2_USERNAME;
    public final static Boolean USER_2_VERIFIED = true;
    public final static Boolean USER_2_TOTP = false;
    public final static Long USER_2_NOT_BEFORE = 0L;
    public final static Boolean USER_2_ENABLED = true;
    public final static Boolean USER_2_IS_INTERNAL = false;
    public final static String USER_2_THEME = "light";
    public final static String USER_2_LANGUAGE = "de";
    public final static Instant USER_2_CREATED = Instant.ofEpochSecond(1677399528L) /* 2023-02-26 08:18:48 (UTC) */;
    public final static Instant USER_2_LAST_MODIFIED = USER_1_CREATED;
    public final static UUID USER_2_REALM_ID = REALM_DBREPO_ID;

    public final static UserAttributesDto USER_2_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_2_THEME)
            .orcid(USER_2_ORCID_URL)
            .affiliation(USER_2_AFFILIATION)
            .mariadbPassword(USER_2_DATABASE_PASSWORD)
            .language(USER_2_LANGUAGE)
            .build();

    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .affiliation(USER_2_AFFILIATION)
            .orcid(USER_2_ORCID_URL)
            .theme(USER_2_THEME)
            .mariadbPassword(USER_2_DATABASE_PASSWORD)
            .language(USER_2_LANGUAGE)
            .isInternal(USER_2_IS_INTERNAL)
            .build();

    public final static UserDto USER_2_DTO = UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .name(USER_2_NAME)
            .qualifiedName(USER_2_QUALIFIED_NAME)
            .attributes(USER_2_ATTRIBUTES_DTO)
            .build();

    public final static UserBriefDto USER_2_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .name(USER_2_NAME)
            .orcid(USER_2_ORCID_URL)
            .qualifiedName(USER_2_QUALIFIED_NAME)
            .build();

    public final static SignupRequestDto USER_2_SIGNUP_REQUEST_DTO = SignupRequestDto.builder()
            .username(USER_2_USERNAME)
            .password(USER_2_PASSWORD)
            .email(USER_2_EMAIL)
            .build();

    public final static UserDetails USER_2_DETAILS = UserDetailsDto.builder()
            .id(USER_2_ID.toString())
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final static at.tuwien.api.keycloak.UserDto USER_2_KEYCLOAK_DTO = at.tuwien.api.keycloak.UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(USER_2_VERIFIED)
            .notBefore(USER_2_NOT_BEFORE)
            .totp(USER_2_TOTP)
            .build();

    public final static at.tuwien.api.amqp.UserDetailsDto USER_2_DETAILS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_2_USERNAME)
            .tags(new String[]{})
            .build();

    public final static PrivilegedUserDto USER_2_PRIVILEGED_DTO = PrivilegedUserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .password(USER_2_PASSWORD)
            .attributes(USER_2_ATTRIBUTES_DTO)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .qualifiedName(USER_2_QUALIFIED_NAME)
            .lastRetrieved(Instant.now())
            .build();

    public final static Principal USER_2_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_2_DETAILS,
            USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());

    public final static UUID USER_3_ID = UUID.fromString("7b080e33-d8db-4276-9d53-47208e657006");
    public final static UUID USER_3_LDAP_ID = UUID.fromString("7b080e33-d8db-4276-9d53-47208e657006");
    public final static String USER_3_USERNAME = "junit3";
    public final static String USER_3_FIRSTNAME = "System";
    public final static String USER_3_LASTNAME = "System";
    public final static String USER_3_NAME = "System System";
    public final static String USER_3_AFFILIATION = "TU Wien";
    public final static String USER_3_ORCID_URL = null;
    public final static String USER_3_ORCID_UNCOMPRESSED = null;
    public final static String USER_3_EMAIL = "system@example.com";
    @SuppressWarnings("java:S2068")
    public final static String USER_3_PASSWORD = "password";
    @SuppressWarnings("java:S2068")
    public final static String USER_3_DATABASE_PASSWORD = "*D65FCA043964B63E849DD6334699ECB065905DA4" /* junit3 */;
    public final static String USER_3_QUALIFIED_NAME = USER_3_FIRSTNAME + " " + USER_3_LASTNAME + " — @" + USER_3_USERNAME;
    public final static Boolean USER_3_VERIFIED = true;
    public final static Boolean USER_3_TOTP = false;
    public final static Long USER_3_NOT_BEFORE = 0L;
    public final static Boolean USER_3_ENABLED = true;
    public final static Boolean USER_3_IS_INTERNAL = false;
    public final static String USER_3_THEME = "light";
    public final static Instant USER_3_CREATED = Instant.ofEpochSecond(1677399559L) /* 2023-02-26 08:19:19 (UTC) */;
    public final static UUID USER_3_REALM_ID = REALM_DBREPO_ID;

    public final static UserAttributesDto USER_3_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_3_THEME)
            .orcid(USER_3_ORCID_UNCOMPRESSED)
            .affiliation(USER_3_AFFILIATION)
            .mariadbPassword(USER_3_DATABASE_PASSWORD)
            .build();

    public final static User USER_3 = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .affiliation(USER_3_AFFILIATION)
            .orcid(USER_3_ORCID_URL)
            .theme(USER_3_THEME)
            .mariadbPassword(USER_3_DATABASE_PASSWORD)
            .isInternal(USER_3_IS_INTERNAL)
            .build();

    public final static UserDto USER_3_DTO = UserDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .name(USER_3_NAME)
            .qualifiedName(USER_3_QUALIFIED_NAME)
            .attributes(USER_3_ATTRIBUTES_DTO)
            .build();

    public final static UserBriefDto USER_3_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .name(USER_3_NAME)
            .qualifiedName(USER_3_QUALIFIED_NAME)
            .build();

    public final static UserDetails USER_3_DETAILS = UserDetailsDto.builder()
            .id(USER_3_ID.toString())
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final static at.tuwien.api.keycloak.UserDto USER_3_KEYCLOAK_DTO = at.tuwien.api.keycloak.UserDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .emailVerified(USER_3_VERIFIED)
            .notBefore(USER_3_NOT_BEFORE)
            .totp(USER_3_TOTP)
            .build();

    public final static Principal USER_3_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_3_DETAILS,
            USER_3_PASSWORD, USER_3_DETAILS.getAuthorities());

    public final static at.tuwien.api.amqp.UserDetailsDto USER_3_DETAILS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_3_USERNAME)
            .tags(new String[]{})
            .build();

    public final static PrivilegedUserDto USER_3_PRIVILEGED_DTO = PrivilegedUserDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .password(USER_3_PASSWORD)
            .attributes(USER_3_ATTRIBUTES_DTO)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .qualifiedName(USER_3_QUALIFIED_NAME)
            .lastRetrieved(Instant.now())
            .build();

    public final static UUID USER_4_ID = UUID.fromString("791d58c5-bfab-4520-b4fc-b44d4ab9feb0");
    public final static UUID USER_4_LDAP_ID = UUID.fromString("791d58c5-bfab-4520-b4fc-b44d4ab9feb0");
    public final static String USER_4_USERNAME = "junit4";
    public final static String USER_4_FIRSTNAME = "JUnit";
    public final static String USER_4_LASTNAME = "4";
    public final static String USER_4_NAME = "JUnit 4";
    public final static String USER_4_AFFILIATION = "TU Wien";
    public final static String USER_4_ORCID_URL = null;
    @SuppressWarnings("java:S2068")
    public final static String USER_4_PASSWORD = "junit4";
    @SuppressWarnings("java:S2068")
    public final static String USER_4_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit4 */;
    public final static String USER_4_QUALIFIED_NAME = USER_4_FIRSTNAME + " " + USER_4_LASTNAME + " — @" + USER_4_USERNAME;
    public final static String USER_4_EMAIL = "junit4@ossdip.at";
    public final static Boolean USER_4_VERIFIED = true;
    public final static Boolean USER_4_ENABLED = true;
    public final static Boolean USER_4_IS_INTERNAL = false;
    public final static String USER_4_THEME = "light";
    public final static Instant USER_4_CREATED = Instant.ofEpochSecond(1677399592L) /* 2023-02-26 08:19:52 (UTC) */;
    public final static UUID USER_4_REALM_ID = REALM_DBREPO_ID;

    public final static UserAttributesDto USER_4_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_4_THEME)
            .orcid(USER_4_ORCID_URL)
            .affiliation(USER_4_AFFILIATION)
            .mariadbPassword(USER_4_DATABASE_PASSWORD)
            .build();

    public final static User USER_4 = User.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .affiliation(USER_4_AFFILIATION)
            .orcid(USER_4_ORCID_URL)
            .theme(USER_4_THEME)
            .mariadbPassword(USER_4_DATABASE_PASSWORD)
            .isInternal(USER_4_IS_INTERNAL)
            .build();

    public final static UserDto USER_4_DTO = UserDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .name(USER_4_NAME)
            .attributes(USER_4_ATTRIBUTES_DTO)
            .qualifiedName(USER_4_QUALIFIED_NAME)
            .build();

    public final static UserBriefDto USER_4_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .name(USER_4_NAME)
            .qualifiedName(USER_4_QUALIFIED_NAME)
            .build();

    public final static UserDetails USER_4_DETAILS = UserDetailsDto.builder()
            .id(USER_4_ID.toString())
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .password(USER_4_PASSWORD)
            .authorities(new LinkedList<>())
            .build();

    public final static Principal USER_4_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_4_DETAILS,
            USER_4_PASSWORD, USER_4_DETAILS.getAuthorities());

    public final static PrivilegedUserDto USER_4_PRIVILEGED_DTO = PrivilegedUserDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .password(USER_4_PASSWORD)
            .attributes(USER_4_ATTRIBUTES_DTO)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .qualifiedName(USER_4_QUALIFIED_NAME)
            .lastRetrieved(Instant.now())
            .build();

    public final static UUID USER_5_ID = UUID.fromString("28ff851d-d7bc-4422-959c-edd7a5b15630");
    public final static UUID USER_5_LDAP_ID = UUID.fromString("28ff851d-d7bc-4422-959c-edd7a5b15630");
    public final static String USER_5_USERNAME = "nobody";
    public final static String USER_5_FIRSTNAME = "No";
    public final static String USER_5_LASTNAME = "Body";
    public final static String USER_5_NAME = "No Body";
    public final static String USER_5_AFFILIATION = "TU Wien";
    public final static String USER_5_ORCID = null;
    @SuppressWarnings("java:S2068")
    public final static String USER_5_PASSWORD = "junit5";
    @SuppressWarnings("java:S2068")
    public final static String USER_5_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit5 */;
    public final static String USER_5_QUALIFIED_NAME = USER_5_FIRSTNAME + " " + USER_5_LASTNAME + " — @" + USER_5_USERNAME;
    public final static String USER_5_EMAIL = "system@ossdip.at";
    public final static Boolean USER_5_VERIFIED = true;
    public final static Boolean USER_5_ENABLED = true;
    public final static Boolean USER_5_IS_INTERNAL = false;
    public final static String USER_5_THEME = "dark";
    public final static Instant USER_5_CREATED = Instant.ofEpochSecond(1677399592L) /* 2023-02-26 08:19:52 (UTC) */;
    public final static UUID USER_5_REALM_ID = REALM_DBREPO_ID;

    public final static UserAttributesDto USER_5_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_5_THEME)
            .affiliation(USER_5_AFFILIATION)
            .mariadbPassword(USER_5_DATABASE_PASSWORD)
            .build();

    public final static UserDto USER_5_DTO = UserDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .name(USER_5_NAME)
            .qualifiedName(USER_5_QUALIFIED_NAME)
            .attributes(USER_5_ATTRIBUTES_DTO)
            .build();

    public final static PrivilegedUserDto USER_5_PRIVILEGED_DTO = PrivilegedUserDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .qualifiedName(USER_5_QUALIFIED_NAME)
            .password(USER_5_PASSWORD)
            .attributes(USER_5_ATTRIBUTES_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static UserBriefDto USER_5_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .qualifiedName(USER_5_QUALIFIED_NAME)
            .build();

    public final static UserDetails USER_5_DETAILS = UserDetailsDto.builder()
            .id(USER_5_ID.toString())
            .username(USER_5_USERNAME)
            .email(USER_5_EMAIL)
            .password(USER_5_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_DEVELOPER_AUTHORITIES)
            .build();

    public final static Principal USER_5_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_5_DETAILS,
            USER_5_PASSWORD, USER_5_DETAILS.getAuthorities());

    public final static User USER_5 = User.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .email(USER_5_EMAIL)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .affiliation(USER_5_AFFILIATION)
            .theme(USER_5_THEME)
            .mariadbPassword(USER_5_DATABASE_PASSWORD)
            .isInternal(USER_5_IS_INTERNAL)
            .build();

    public final static UUID USER_6_ID = UUID.fromString("28ff851d-d7bc-4422-959c-edd7a5b15630");
    public final static String USER_6_USERNAME = "system";
    public final static String USER_6_FIRSTNAME = "System";
    public final static String USER_6_LASTNAME = "System";
    public final static String USER_6_NAME = "System System";
    public final static String USER_6_AFFILIATION = "TU Wien";
    public final static String USER_6_ORCID = null;
    @SuppressWarnings("java:S2068")
    public final static String USER_6_PASSWORD = "junit5";
    @SuppressWarnings("java:S2068")
    public final static String USER_6_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit5 */;
    public final static String USER_6_EMAIL = "system@ossdip.at";
    public final static Boolean USER_6_VERIFIED = true;
    public final static Boolean USER_6_ENABLED = true;
    public final static Boolean USER_6_IS_INTERNAL = false;
    public final static Boolean USER_6_THEME_DARK = false;
    public final static Instant USER_6_CREATED = Instant.ofEpochSecond(1677399592L) /* 2023-02-26 08:19:52 (UTC) */;
    public final static UUID USER_6_REALM_ID = REALM_DBREPO_ID;

    public final static UserDto USER_6_DTO = UserDto.builder()
            .id(USER_6_ID)
            .username(USER_6_USERNAME)
            .firstname(USER_6_FIRSTNAME)
            .lastname(USER_6_LASTNAME)
            .build();

    public final static UserDetails USER_6_DETAILS = UserDetailsDto.builder()
            .id(USER_6_ID.toString())
            .username(USER_6_USERNAME)
            .email(USER_6_EMAIL)
            .password(USER_6_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final static Principal USER_6_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_6_DETAILS,
            USER_6_PASSWORD, USER_6_DETAILS.getAuthorities());

    public final static Long IMAGE_1_ID = 1L;
    public final static String IMAGE_1_REGISTRY = "docker.io";
    public final static String IMAGE_1_NAME = "mariadb";
    public final static String IMAGE_1_VERSION = "11.1.3";
    public final static String IMAGE_1_DIALECT = "org.hibernate.dialect.MariaDBDialect";
    public final static String IMAGE_1_DRIVER = "org.mariadb.jdbc.Driver";
    public final static String IMAGE_1_JDBC = "mariadb";
    public final static Integer IMAGE_1_PORT = 3306;
    public final static Boolean IMAGE_1_IS_DEFAULT = true;

    public final static ImageCreateDto IMAGE_1_CREATE_DTO = ImageCreateDto.builder()
            .registry(IMAGE_1_REGISTRY)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_PORT)
            .build();

    public final static ImageChangeDto IMAGE_1_CHANGE_DTO = ImageChangeDto.builder()
            .registry(IMAGE_1_REGISTRY)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_PORT)
            .build();

    public final static ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .registry(IMAGE_1_REGISTRY)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_PORT)
            .isDefault(IMAGE_1_IS_DEFAULT)
            .operators(new LinkedList<>()) /* IMAGE_1_OPERATORS */
            .build();

    public final static ImageDto IMAGE_1_DTO = ImageDto.builder()
            .id(IMAGE_1_ID)
            .registry(IMAGE_1_REGISTRY)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_PORT)
            .isDefault(IMAGE_1_IS_DEFAULT)
            .operators(null)
            .build();

    public final static ImageBriefDto IMAGE_1_BRIEF_DTO = ImageBriefDto.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .isDefault(IMAGE_1_IS_DEFAULT)
            .jdbcMethod(IMAGE_1_JDBC)
            .build();

    public final static Long IMAGE_1_OPERATORS_1_ID = 1L;
    public final static String IMAGE_1_OPERATORS_1_DISPLAY_NAME = "XOR";
    public final static String IMAGE_1_OPERATORS_1_VALUE = "XOR";
    public final static String IMAGE_1_OPERATORS_1_DOCUMENTATION = "https://mariadb.com/kb/en/xor/";

    public final static List<Operator> IMAGE_1_OPERATORS = new LinkedList<>(List.of(
            Operator.builder()
                    .id(IMAGE_1_OPERATORS_1_ID)
                    .image(IMAGE_1)
                    .displayName(IMAGE_1_OPERATORS_1_DISPLAY_NAME)
                    .value(IMAGE_1_OPERATORS_1_VALUE)
                    .documentation(IMAGE_1_OPERATORS_1_DOCUMENTATION)
                    .build()));

    public final static List<OperatorDto> IMAGE_1_OPERATORS_DTO = new LinkedList<>(List.of(
            OperatorDto.builder()
                    .id(IMAGE_1_OPERATORS_1_ID)
                    .displayName(IMAGE_1_OPERATORS_1_DISPLAY_NAME)
                    .value(IMAGE_1_OPERATORS_1_VALUE)
                    .documentation(IMAGE_1_OPERATORS_1_DOCUMENTATION)
                    .build()));

    public final static Long CONTAINER_1_ID = 1L;
    public final static ContainerImage CONTAINER_1_IMAGE = IMAGE_1;
    public final static ImageDto CONTAINER_1_IMAGE_DTO = IMAGE_1_DTO;
    public final static String CONTAINER_1_NAME = "u01";
    public final static String CONTAINER_1_INTERNALNAME = "dbrepo-userdb-u01";
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
    public final static Instant CONTAINER_1_CREATED = Instant.ofEpochSecond(1677399629L) /* 2023-02-26 08:20:29 (UTC) */;

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .image(CONTAINER_1_IMAGE)
            .created(CONTAINER_1_CREATED)
            .host(CONTAINER_1_HOST)
            .port(CONTAINER_1_PORT)
            .uiHost(CONTAINER_1_UI_HOST)
            .uiPort(CONTAINER_1_UI_PORT)
            .quota(CONTAINER_1_QUOTA)
            .uiAdditionalFlags(CONTAINER_1_UI_ADDITIONAL_FLAGS)
            .privilegedUsername(CONTAINER_1_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_1_PRIVILEGED_PASSWORD)
            .build();

    public final static ContainerDto CONTAINER_1_DTO = ContainerDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .image(CONTAINER_1_IMAGE_DTO)
            .host(CONTAINER_1_HOST)
            .port(CONTAINER_1_PORT)
            .build();

    public final static ContainerBriefDto CONTAINER_1_BRIEF_DTO = ContainerBriefDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .quota(CONTAINER_1_QUOTA)
            .count(CONTAINER_1_COUNT)
            .image(IMAGE_1_BRIEF_DTO)
            .build();

    public final static PrivilegedContainerDto CONTAINER_1_PRIVILEGED_DTO = PrivilegedContainerDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .image(CONTAINER_1_IMAGE_DTO)
            .host(CONTAINER_1_HOST)
            .port(CONTAINER_1_PORT)
            .username(CONTAINER_1_PRIVILEGED_USERNAME)
            .password(CONTAINER_1_PRIVILEGED_PASSWORD)
            .lastRetrieved(Instant.now())
            .build();

    public final static Long CONTAINER_2_ID = 2L;
    public final static ContainerImage CONTAINER_2_IMAGE = IMAGE_1;
    public final static ImageDto CONTAINER_2_IMAGE_DTO = IMAGE_1_DTO;
    public final static String CONTAINER_2_NAME = "u02";
    public final static String CONTAINER_2_INTERNALNAME = "dbrepo-userdb-u02";
    public final static String CONTAINER_2_HOST = "localhost";
    public final static Integer CONTAINER_2_PORT = 3309;
    public final static Integer CONTAINER_2_QUOTA = 3;
    public final static Integer CONTAINER_2_COUNT = 3;
    public final static String CONTAINER_2_PRIVILEGED_USERNAME = "root";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_2_PRIVILEGED_PASSWORD = "dbrepo";
    public final static Instant CONTAINER_2_CREATED = Instant.ofEpochSecond(1677399655L) /* 2023-02-26 08:20:55 (UTC) */;

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .image(CONTAINER_2_IMAGE)
            .created(CONTAINER_2_CREATED)
            .host(CONTAINER_2_HOST)
            .port(CONTAINER_2_PORT)
            .quota(CONTAINER_2_QUOTA)
            .databases(new LinkedList<>(List.of()))
            .privilegedUsername(CONTAINER_2_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_2_PRIVILEGED_PASSWORD)
            .build();

    public final static ContainerDto CONTAINER_2_DTO = ContainerDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .image(CONTAINER_2_IMAGE_DTO)
            .host(CONTAINER_2_HOST)
            .port(CONTAINER_2_PORT)
            .build();

    public final static ContainerBriefDto CONTAINER_2_DTO_BRIEF = ContainerBriefDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .quota(CONTAINER_2_QUOTA)
            .build();

    public final static PrivilegedContainerDto CONTAINER_2_PRIVILEGED_DTO = PrivilegedContainerDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .image(CONTAINER_2_IMAGE_DTO)
            .host(CONTAINER_2_HOST)
            .port(CONTAINER_2_PORT)
            .username(CONTAINER_2_PRIVILEGED_USERNAME)
            .password(CONTAINER_2_PRIVILEGED_PASSWORD)
            .lastRetrieved(Instant.now())
            .build();

    public final static Long CONTAINER_3_ID = 3L;
    public final static ContainerImage CONTAINER_3_IMAGE = IMAGE_1;
    public final static String CONTAINER_3_NAME = "u03";
    public final static String CONTAINER_3_INTERNALNAME = "dbrepo-userdb-u03";
    public final static String CONTAINER_3_HOST = "localhost";
    public final static Integer CONTAINER_3_PORT = 3310;
    public final static Integer CONTAINER_3_QUOTA = 20;
    public final static String CONTAINER_3_PRIVILEGED_USERNAME = "root";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_3_PRIVILEGED_PASSWORD = "dbrepo";
    public final static Instant CONTAINER_3_CREATED = Instant.ofEpochSecond(1677399672L) /* 2023-02-26 08:21:12 (UTC) */;

    public final static Container CONTAINER_3 = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .internalName(CONTAINER_3_INTERNALNAME)
            .image(CONTAINER_3_IMAGE)
            .created(CONTAINER_3_CREATED)
            .host(CONTAINER_3_HOST)
            .port(CONTAINER_3_PORT)
            .quota(CONTAINER_3_QUOTA)
            .databases(new LinkedList<>(List.of()))
            .privilegedUsername(CONTAINER_3_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_3_PRIVILEGED_PASSWORD)
            .build();

    public final static Long CONTAINER_4_ID = 4L;
    public final static ContainerImage CONTAINER_4_IMAGE = IMAGE_1;
    public final static String CONTAINER_4_NAME = "u04";
    public final static String CONTAINER_4_INTERNALNAME = "dbrepo-userdb-u04";
    public final static String CONTAINER_4_HOST = "localhost";
    public final static Integer CONTAINER_4_PORT = 3311;
    public final static Integer CONTAINER_4_QUOTA = 0;
    public final static String CONTAINER_4_PRIVILEGED_USERNAME = "root";
    @SuppressWarnings("java:S2068")
    public final static String CONTAINER_4_PRIVILEGED_PASSWORD = "dbrepo";
    public final static Instant CONTAINER_4_CREATED = Instant.ofEpochSecond(1677399688L) /* 2023-02-26 08:21:28 (UTC) */;

    public final static Container CONTAINER_4 = Container.builder()
            .id(CONTAINER_4_ID)
            .name(CONTAINER_4_NAME)
            .internalName(CONTAINER_4_INTERNALNAME)
            .image(CONTAINER_4_IMAGE)
            .created(CONTAINER_4_CREATED)
            .host(CONTAINER_4_HOST)
            .port(CONTAINER_4_PORT)
            .quota(CONTAINER_4_QUOTA)
            .privilegedUsername(CONTAINER_4_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_4_PRIVILEGED_PASSWORD)
            .build();

    public final static String EXCHANGE_DBREPO_NAME = "dbrepo";
    public final static Boolean EXCHANGE_DBREPO_AUTO_DELETE = true;
    public final static Boolean EXCHANGE_DBREPO_DURABLE = true;
    public final static Boolean EXCHANGE_DBREPO_INTERNAL = true;
    public final static String EXCHANGE_DBREPO_TYPE = "topic";
    public final static String EXCHANGE_DBREPO_VHOST = "dbrepo";

    public final static ExchangeDto EXCHANGE_DBREPO_DTO = ExchangeDto.builder()
            .autoDelete(EXCHANGE_DBREPO_AUTO_DELETE)
            .type(EXCHANGE_DBREPO_TYPE)
            .name(EXCHANGE_DBREPO_NAME)
            .durable(EXCHANGE_DBREPO_DURABLE)
            .vhost(EXCHANGE_DBREPO_VHOST)
            .internal(EXCHANGE_DBREPO_INTERNAL)
            .build();

    public final static Long DATABASE_1_ID = 1L;
    public final static String DATABASE_1_NAME = "Weather";
    public final static String DATABASE_1_DESCRIPTION = "Weather in Australia";
    public final static String DATABASE_1_INTERNALNAME = "weather";
    public final static Boolean DATABASE_1_PUBLIC = false;
    public final static Boolean DATABASE_1_SCHEMA_PUBLIC = false;
    public final static String DATABASE_1_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_1_CREATED = Instant.ofEpochSecond(1677399741L) /* 2023-02-26 08:22:21 (UTC) */;
    public final static Instant DATABASE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399741L) /* 2023-02-26 08:22:21 (UTC) */;
    public final static UUID DATABASE_1_OWNER = USER_1_ID;
    public final static UUID DATABASE_1_CREATED_BY = USER_1_ID;
    public final static UserDto DATABASE_1_CREATOR_DTO = USER_1_DTO;
    public final static UserDto DATABASE_1_OWNER_DTO = USER_1_DTO;

    public final static DatabaseCreateDto DATABASE_1_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_1_NAME)
            .isPublic(DATABASE_1_PUBLIC)
            .cid(CONTAINER_1_ID)
            .build();

    public final static CreateDatabaseDto DATABASE_1_CREATE_INTERNAL = CreateDatabaseDto.builder()
            .internalName(DATABASE_1_INTERNALNAME)
            .containerId(CONTAINER_1_ID)
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .userId(USER_1_ID)
            .privilegedUsername(CONTAINER_1_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_1_PRIVILEGED_PASSWORD)
            .build();

    public final static Long DATABASE_2_ID = 2L;
    public final static String DATABASE_2_NAME = "Zoo";
    public final static String DATABASE_2_DESCRIPTION = "Zoo data";
    public final static String DATABASE_2_INTERNALNAME = "zoo";
    public final static Boolean DATABASE_2_PUBLIC = false;
    public final static Boolean DATABASE_2_SCHEMA_PUBLIC = true;
    public final static String DATABASE_2_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_2_CREATED = Instant.ofEpochSecond(1677399772L) /* 2023-02-26 08:22:52 (UTC) */;
    public final static Instant DATABASE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677399772L) /* 2023-02-26 08:22:52 (UTC) */;
    public final static UUID DATABASE_2_OWNER = USER_2_ID;
    public final static UUID DATABASE_2_CREATOR = USER_2_ID;

    public final static DatabaseCreateDto DATABASE_2_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_2_NAME)
            .isPublic(DATABASE_2_PUBLIC)
            .cid(CONTAINER_1_ID)
            .build();

    public final static Long DATABASE_3_ID = 3L;
    public final static String DATABASE_3_NAME = "Musicology";
    public final static String DATABASE_3_DESCRIPTION = "Musicology data";
    public final static String DATABASE_3_INTERNALNAME = "musicology";
    public final static Boolean DATABASE_3_PUBLIC = true;
    public final static Boolean DATABASE_3_SCHEMA_PUBLIC = false;
    public final static String DATABASE_3_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_3_CREATED = Instant.ofEpochSecond(1677399792L) /* 2023-02-26 08:23:12 (UTC) */;
    public final static Instant DATABASE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677399792L) /* 2023-02-26 08:23:12 (UTC) */;
    public final static UUID DATABASE_3_OWNER = USER_3_ID;

    public final static DatabaseDto DATABASE_3_DTO = DatabaseDto.builder()
            .id(DATABASE_3_ID)
            .isPublic(DATABASE_3_PUBLIC)
            .name(DATABASE_3_NAME)
            .container(CONTAINER_1_BRIEF_DTO)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .tables(new LinkedList<>()) /* TABLE_3, TABLE_3, TABLE_3 */
            .views(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final static DatabaseCreateDto DATABASE_3_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_3_NAME)
            .isPublic(DATABASE_3_PUBLIC)
            .cid(CONTAINER_1_ID)
            .build();

    public final static Long DATABASE_4_ID = 4L;
    public final static String DATABASE_4_NAME = "Weather AT";
    public final static String DATABASE_4_DESCRIPTION = "Weather data";
    public final static Boolean DATABASE_4_PUBLIC = true;
    public final static Boolean DATABASE_4_SCHEMA_PUBLIC = true;
    public final static String DATABASE_4_INTERNALNAME = "weather_at";
    public final static String DATABASE_4_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_4_CREATED = Instant.ofEpochSecond(1677399813L) /* 2023-02-26 08:23:33 (UTC) */;
    public final static Instant DATABASE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677399813L) /* 2023-02-26 08:23:33 (UTC) */;
    public final static UUID DATABASE_4_OWNER = USER_4_ID;
    public final static UUID DATABASE_4_CREATOR = USER_4_ID;

    public final static DatabaseDto DATABASE_4_DTO = DatabaseDto.builder()
            .id(DATABASE_4_ID)
            .isPublic(DATABASE_4_PUBLIC)
            .isSchemaPublic(DATABASE_4_SCHEMA_PUBLIC)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .owner(USER_4_BRIEF_DTO)
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final static TableCreateDto TABLE_0_CREATE_DTO = TableCreateDto.builder()
            .name("full")
            .description("full example")
            .constraints(ConstraintsCreateDto.builder()
                    .uniques(new LinkedList<>())
                    .foreignKeys(new LinkedList<>())
                    .build())
            .columns(List.of(ColumnCreateDto.builder()
                            .name("col1a")
                            .type(ColumnTypeDto.CHAR)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col1b")
                            .type(ColumnTypeDto.CHAR)
                            .nullAllowed(true)
                            .size(50L)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col2a")
                            .type(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col2b")
                            .type(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .size(1024L)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col3")
                            .type(ColumnTypeDto.BINARY)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col4")
                            .type(ColumnTypeDto.VARBINARY)
                            .nullAllowed(true)
                            .size(200L)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col5")
                            .type(ColumnTypeDto.TINYBLOB)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col6")
                            .type(ColumnTypeDto.TINYTEXT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col7")
                            .type(ColumnTypeDto.TEXT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col8")
                            .type(ColumnTypeDto.BLOB)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col9")
                            .type(ColumnTypeDto.MEDIUMTEXT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col10")
                            .type(ColumnTypeDto.MEDIUMBLOB)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col11")
                            .type(ColumnTypeDto.LONGTEXT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col12")
                            .type(ColumnTypeDto.LONGBLOB)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col13")
                            .type(ColumnTypeDto.ENUM)
                            .nullAllowed(true)
                            .enums(new LinkedList<>(List.of("val1", "val2")))
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col14")
                            .type(ColumnTypeDto.SET)
                            .nullAllowed(true)
                            .sets(new LinkedList<>(List.of("val1", "val2")))
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col15")
                            .type(ColumnTypeDto.BIT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col16")
                            .type(ColumnTypeDto.TINYINT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col17")
                            .type(ColumnTypeDto.BOOL)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col18")
                            .type(ColumnTypeDto.SMALLINT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col19")
                            .type(ColumnTypeDto.MEDIUMINT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col20")
                            .type(ColumnTypeDto.INT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col21")
                            .type(ColumnTypeDto.BIGINT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col22")
                            .type(ColumnTypeDto.FLOAT)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col23")
                            .type(ColumnTypeDto.DOUBLE)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col24")
                            .type(ColumnTypeDto.DECIMAL)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col25")
                            .type(ColumnTypeDto.DATE)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col26")
                            .type(ColumnTypeDto.DATETIME)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col27")
                            .type(ColumnTypeDto.TIMESTAMP)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col28")
                            .type(ColumnTypeDto.TIME)
                            .nullAllowed(true)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col29")
                            .type(ColumnTypeDto.YEAR)
                            .nullAllowed(true)
                            .build()))
            .build();

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "Weather AUS";
    public final static String TABLE_1_INTERNAL_NAME = "weather_aus";
    public final static Boolean TABLE_1_VERSIONED = true;
    public final static Boolean TABLE_1_IS_PUBLIC = false;
    public final static Boolean TABLE_1_SCHEMA_PUBLIC = false;
    public final static Boolean TABLE_1_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_1_DESCRIPTION = "Weather in Australia";
    public final static String TABLE_1_QUEUE_NAME = TABLE_1_INTERNAL_NAME;
    public final static String TABLE_1_ROUTING_KEY = "dbrepo\\." + DATABASE_1_ID + "\\." + TABLE_1_ID;
    public final static Long TABLE_1_DATABASE_ID = DATABASE_1_ID;
    public final static Long TABLE_1_AVG_ROW_LENGTH = 3L;
    public final static Long TABLE_1_NUM_ROWS = 3L;
    public final static Long TABLE_1_DATA_LENGTH = 2000L;
    public final static Long TABLE_1_MAX_DATA_LENGTH = Long.MAX_VALUE;
    public final static Instant TABLE_1_CREATED = Instant.ofEpochSecond(1677399975L) /* 2023-02-26 08:26:15 (UTC) */;
    public final static Instant TABLE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399975L) /* 2023-02-26 08:26:15 (UTC) */;

    public final static PrivilegedTableDto TABLE_1_PRIVILEGED_DTO = PrivilegedTableDto.builder()
            .id(TABLE_1_ID)
            .tdbid(DATABASE_1_ID)
            .database(null) /* DATABASE_1_PRIVILEGED_DTO */
            .internalName(TABLE_1_INTERNAL_NAME)
            .isVersioned(TABLE_1_VERSIONED)
            .isPublic(TABLE_1_SCHEMA_PUBLIC)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .identifiers(new LinkedList<>())
            .columns(new LinkedList<>() /* TABLE_1_COLUMNS_DTO */)
            .constraints(null) /* TABLE_1_CONSTRAINTS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .isPublic(DATABASE_1_PUBLIC)
            .avgRowLength(TABLE_1_AVG_ROW_LENGTH)
            .numRows(TABLE_1_NUM_ROWS)
            .dataLength(TABLE_1_DATA_LENGTH)
            .maxDataLength(TABLE_1_MAX_DATA_LENGTH)
            .lastRetrieved(Instant.now())
            .build();

    public final static Table TABLE_1 = Table.builder()
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
            .columns(new LinkedList<>() /* TABLE_1_COLUMNS */)
            .constraints(null) /* TABLE_1_CONSTRAINTS */
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .avgRowLength(TABLE_1_AVG_ROW_LENGTH)
            .numRows(TABLE_1_NUM_ROWS)
            .dataLength(TABLE_1_DATA_LENGTH)
            .maxDataLength(TABLE_1_MAX_DATA_LENGTH)
            .build();

    public final static TableDto TABLE_1_DTO = TableDto.builder()
            .id(TABLE_1_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_1_INTERNAL_NAME)
            .isVersioned(TABLE_1_VERSIONED)
            .isPublic(TABLE_1_IS_PUBLIC)
            .isSchemaPublic(TABLE_1_SCHEMA_PUBLIC)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .identifiers(new LinkedList<>())
            .columns(new LinkedList<>() /* TABLE_1_COLUMNS_DTO */)
            .constraints(null) /* TABLE_1_CONSTRAINT_DTO */
            .owner(USER_1_BRIEF_DTO)
            .avgRowLength(TABLE_1_AVG_ROW_LENGTH)
            .numRows(TABLE_1_NUM_ROWS)
            .dataLength(TABLE_1_DATA_LENGTH)
            .maxDataLength(TABLE_1_MAX_DATA_LENGTH)
            .build();

    public final static List<ColumnDto> TABLE_1_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(1L)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .name("id")
                    .internalName("id")
                    .columnType(ColumnTypeDto.SERIAL)
                    .isNullAllowed(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(2L)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .name("Date")
                    .internalName("date")
                    .columnType(ColumnTypeDto.DATE)
                    .isNullAllowed(true)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(3L)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .name("Location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(4L)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(5L)
                    .tableId(TABLE_1_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(4)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .concept(CONCEPT_1_BRIEF_DTO)
                    .unit(UNIT_1_BRIEF_DTO)
                    .isNullAllowed(true)
                    .enums(null)
                    .sets(null)
                    .build());

    public final static TableBriefDto TABLE_1_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_1_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_1_INTERNAL_NAME)
            .isVersioned(TABLE_1_VERSIONED)
            .isPublic(TABLE_1_IS_PUBLIC)
            .isSchemaPublic(TABLE_1_SCHEMA_PUBLIC)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .ownedBy(USER_1_ID)
            .build();

    public final static Long TABLE_1_DATA_COUNT = 3L;
    @SuppressWarnings("java:S3599")
    public final static List<Map<String, Object>> TABLE_1_DATA_DTO = new LinkedList<>(List.of(
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

    public final static Long TABLE_2_ID = 2L;
    public final static String TABLE_2_NAME = "Weather Location";
    public final static String TABLE_2_INTERNALNAME = "weather_location";
    public final static Boolean TABLE_2_VERSIONED = true;
    public final static Boolean TABLE_2_IS_PUBLIC = false;
    public final static Boolean TABLE_2_SCHEMA_PUBLIC = true;
    public final static Boolean TABLE_2_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_2_DESCRIPTION = "Weather location";
    public final static String TABLE_2_QUEUE_NAME = TABLE_2_INTERNALNAME;
    public final static String TABLE_2_ROUTING_KEY = "dbrepo\\." + DATABASE_1_ID + "\\." + TABLE_2_ID;
    public final static Instant TABLE_2_CREATED = Instant.ofEpochSecond(1677400007L) /* 2023-02-26 08:26:47 (UTC) */;
    public final static Instant TABLE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677400007L) /* 2023-02-26 08:26:47 (UTC) */;
    public final static Long TABLE_2_AVG_ROW_LENGTH = 3L;
    public final static Long TABLE_2_NUM_ROWS = 3L;
    public final static Long TABLE_2_DATA_LENGTH = 2000L;
    public final static Long TABLE_2_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final static Table TABLE_2 = Table.builder()
            .id(TABLE_2_ID)
            .tdbid(DATABASE_1_ID)
            .database(null /* DATABASE_1 */)
            .created(TABLE_2_CREATED)
            .internalName(TABLE_2_INTERNALNAME)
            .isVersioned(TABLE_2_VERSIONED)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .queueName(TABLE_2_QUEUE_NAME)
            .columns(new LinkedList<>() /* TABLE_2_COLUMNS */)
            .constraints(null) /* TABLE_2_CONSTRAINTS */
            .owner(USER_2)
            .ownedBy(USER_2_ID)
            .avgRowLength(TABLE_2_AVG_ROW_LENGTH)
            .numRows(TABLE_2_NUM_ROWS)
            .dataLength(TABLE_2_DATA_LENGTH)
            .maxDataLength(TABLE_2_MAX_DATA_LENGTH)
            .build();

    public final static PrivilegedTableDto TABLE_2_PRIVILEGED_DTO = PrivilegedTableDto.builder()
            .id(TABLE_2_ID)
            .tdbid(DATABASE_1_ID)
            .database(null) /* DATABASE_1_PRIVILEGED_DTO */
            .internalName(TABLE_2_INTERNALNAME)
            .isVersioned(TABLE_2_VERSIONED)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .identifiers(new LinkedList<>())
            .columns(new LinkedList<>() /* TABLE_2_COLUMNS_DTO */)
            .constraints(null) /* TABLE_2_CONSTRAINTS_DTO */
            .owner(USER_2_BRIEF_DTO)
            .avgRowLength(TABLE_2_AVG_ROW_LENGTH)
            .numRows(TABLE_2_NUM_ROWS)
            .dataLength(TABLE_2_DATA_LENGTH)
            .maxDataLength(TABLE_2_MAX_DATA_LENGTH)
            .lastRetrieved(Instant.now())
            .build();

    public final static TableDto TABLE_2_DTO = TableDto.builder()
            .id(TABLE_2_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_2_INTERNALNAME)
            .isVersioned(TABLE_2_VERSIONED)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .columns(new LinkedList<>() /* TABLE_2_COLUMNS_DTO */)
            .constraints(null) /* TABLE_2_CONSTRAINTS_DTO */
            .owner(USER_2_BRIEF_DTO)
            .avgRowLength(TABLE_2_AVG_ROW_LENGTH)
            .numRows(TABLE_2_NUM_ROWS)
            .dataLength(TABLE_2_DATA_LENGTH)
            .maxDataLength(TABLE_2_MAX_DATA_LENGTH)
            .build();

    public final static TableBriefDto TABLE_2_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_2_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_2_INTERNALNAME)
            .isVersioned(TABLE_2_VERSIONED)
            .isPublic(TABLE_2_IS_PUBLIC)
            .isSchemaPublic(TABLE_2_SCHEMA_PUBLIC)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .ownedBy(USER_2_ID)
            .build();

    public final static Long TABLE_3_ID = 3L;
    public final static String TABLE_3_NAME = "Sensor";
    public final static String TABLE_3_INTERNALNAME = "sensor";
    public final static Boolean TABLE_3_VERSIONED = true;
    public final static Boolean TABLE_3_IS_PUBLIC = false;
    public final static Boolean TABLE_3_SCHEMA_PUBLIC = false;
    public final static Boolean TABLE_3_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_3_DESCRIPTION = "Some sensor data";
    public final static String TABLE_3_QUEUE_NAME = TABLE_3_INTERNALNAME;
    public final static String TABLE_3_ROUTING_KEY = "dbrepo\\." + DATABASE_1_ID + "\\." + TABLE_3_ID;
    public final static Instant TABLE_3_CREATED = Instant.ofEpochSecond(1677400031L) /* 2023-02-26 08:27:11 (UTC) */;
    public final static Instant TABLE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677400031L) /* 2023-02-26 08:27:11 (UTC) */;
    public final static Long TABLE_3_AVG_ROW_LENGTH = 6L;
    public final static Long TABLE_3_NUM_ROWS = 6L;
    public final static Long TABLE_3_DATA_LENGTH = 1800L;
    public final static Long TABLE_3_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final static Table TABLE_3 = Table.builder()
            .id(TABLE_3_ID)
            .tdbid(DATABASE_1_ID)
            .database(null /* DATABASE_1 */)
            .created(TABLE_3_CREATED)
            .internalName(TABLE_3_INTERNALNAME)
            .isVersioned(TABLE_3_VERSIONED)
            .isPublic(TABLE_3_IS_PUBLIC)
            .isSchemaPublic(TABLE_3_SCHEMA_PUBLIC)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .queueName(TABLE_3_QUEUE_NAME)
            .columns(new LinkedList<>() /* TABLE_3_COLUMNS */)
            .constraints(null) /* TABLE_3_CONSTRAINTS */
            .owner(USER_3)
            .ownedBy(USER_3_ID)
            .avgRowLength(TABLE_3_AVG_ROW_LENGTH)
            .numRows(TABLE_3_NUM_ROWS)
            .dataLength(TABLE_3_DATA_LENGTH)
            .maxDataLength(TABLE_3_MAX_DATA_LENGTH)
            .build();

    public final static TableDto TABLE_3_DTO = TableDto.builder()
            .id(TABLE_3_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_3_INTERNALNAME)
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

    public final static TableBriefDto TABLE_3_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_3_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_3_INTERNALNAME)
            .isVersioned(TABLE_3_VERSIONED)
            .isPublic(TABLE_3_IS_PUBLIC)
            .isSchemaPublic(TABLE_3_SCHEMA_PUBLIC)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .ownedBy(USER_3_ID)
            .build();

    public final static ConstraintsCreateDto TABLE_3_CONSTRAINTS_CREATE_DTO = ConstraintsCreateDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .build();

    public final static ConstraintsCreateDto TABLE_3_CONSTRAINTS_INVALID_CREATE_DTO = ConstraintsCreateDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>()) // <<<<
            .uniques(new LinkedList<>())
            .foreignKeys(List.of(ForeignKeyCreateDto.builder()
                    .referencedTable("weather_location")
                    .columns(new LinkedList<>(List.of("fahrzeug")))
                    .referencedColumns(new LinkedList<>(List.of("doesnotexist")))
                    .build()))
            .build();

    public final static TableCreateDto TABLE_3_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(new LinkedList<>())
            .constraints(TABLE_3_CONSTRAINTS_CREATE_DTO)
            .build();

    public final static TableCreateDto TABLE_3_INVALID_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(new LinkedList<>())
            .constraints(TABLE_3_CONSTRAINTS_INVALID_CREATE_DTO)
            .build();

    public final static Long TABLE_5_ID = 5L;
    public final static String TABLE_5_NAME = "zoo";
    public final static String TABLE_5_INTERNALNAME = "zoo";
    public final static Boolean TABLE_5_VERSIONED = true;
    public final static Boolean TABLE_5_IS_PUBLIC = true;
    public final static Boolean TABLE_5_SCHEMA_PUBLIC = true;
    public final static Boolean TABLE_5_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_5_DESCRIPTION = "Some Kaggle dataset";
    public final static String TABLE_5_QUEUE_NAME = TABLE_5_INTERNALNAME;
    public final static String TABLE_5_ROUTING_KEY = "dbrepo\\." + DATABASE_2_ID + "\\." + TABLE_5_ID;
    public final static Instant TABLE_5_CREATED = Instant.ofEpochSecond(1677400067L) /* 2023-02-26 08:27:47 (UTC) */;
    public final static Instant TABLE_5_LAST_MODIFIED = Instant.ofEpochSecond(1677400067L) /* 2023-02-26 08:27:47 (UTC) */;
    public final static Long TABLE_5_AVG_ROW_LENGTH = 1080L;
    public final static Long TABLE_5_NUM_ROWS = 101L;
    public final static Long TABLE_5_DATA_LENGTH = 15200L;
    public final static Long TABLE_5_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final static Table TABLE_5 = Table.builder()
            .id(TABLE_5_ID)
            .tdbid(DATABASE_2_ID)
            .created(Instant.now())
            .internalName(TABLE_5_INTERNALNAME)
            .isVersioned(TABLE_5_VERSIONED)
            .isPublic(TABLE_5_IS_PUBLIC)
            .isSchemaPublic(TABLE_5_SCHEMA_PUBLIC)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .queueName(TABLE_5_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_5_COLUMNS */
            .constraints(null) /* TABLE_5_CONSTRAINTS */
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .build();

    public final static TableDto TABLE_5_DTO = TableDto.builder()
            .id(TABLE_5_ID)
            .tdbid(DATABASE_2_ID)
            .internalName(TABLE_5_INTERNALNAME)
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

    public final static PrivilegedTableDto TABLE_5_PRIVILEGED_DTO = PrivilegedTableDto.builder()
            .id(TABLE_5_ID)
            .tdbid(DATABASE_2_ID)
            .database(null) /* DATABASE_2_PRIVILEGED_DTO */
            .internalName(TABLE_5_INTERNALNAME)
            .isVersioned(TABLE_5_VERSIONED)
            .isPublic(TABLE_5_IS_PUBLIC)
            .isSchemaPublic(TABLE_5_SCHEMA_PUBLIC)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .queueName(TABLE_5_QUEUE_NAME)
            .routingKey(TABLE_5_ROUTING_KEY)
            .identifiers(new LinkedList<>())
            .columns(new LinkedList<>() /* TABLE_5_COLUMNS_DTO */)
            .constraints(null) /* TABLE_5_CONSTRAINTS_DTO */
            .owner(USER_5_BRIEF_DTO)
            .isPublic(DATABASE_2_PUBLIC)
            .avgRowLength(TABLE_5_AVG_ROW_LENGTH)
            .numRows(TABLE_5_NUM_ROWS)
            .dataLength(TABLE_5_DATA_LENGTH)
            .maxDataLength(TABLE_5_MAX_DATA_LENGTH)
            .lastRetrieved(Instant.now())
            .build();

    public final static TableBriefDto TABLE_5_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_5_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_5_INTERNALNAME)
            .isVersioned(TABLE_5_VERSIONED)
            .isPublic(TABLE_5_IS_PUBLIC)
            .isSchemaPublic(TABLE_5_SCHEMA_PUBLIC)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .ownedBy(USER_1_ID)
            .build();

    public final static Long TABLE_6_ID = 6L;
    public final static String TABLE_6_NAME = "names";
    public final static String TABLE_6_INTERNALNAME = "names";
    public final static Boolean TABLE_6_VERSIONED = true;
    public final static Boolean TABLE_6_IS_PUBLIC = true;
    public final static Boolean TABLE_6_SCHEMA_PUBLIC = true;
    public final static Boolean TABLE_6_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_6_DESCRIPTION = "Some names dataset";
    public final static String TABLE_6_QUEUE_NAME = TABLE_6_INTERNALNAME;
    public final static String TABLE_6_ROUTING_KEY = "dbrepo\\." + DATABASE_2_ID + "\\." + TABLE_6_ID;
    public final static Instant TABLE_6_CREATED = Instant.ofEpochSecond(1677400117L) /* 2023-02-26 08:28:37 (UTC) */;
    public final static Instant TABLE_6_LAST_MODIFIED = Instant.ofEpochSecond(1677400117L) /* 2023-02-26 08:28:37 (UTC) */;

    public final static Table TABLE_6 = Table.builder()
            .id(TABLE_6_ID)
            .tdbid(DATABASE_2_ID)
            .created(TABLE_6_CREATED)
            .internalName(TABLE_6_INTERNALNAME)
            .isVersioned(TABLE_6_VERSIONED)
            .isPublic(TABLE_6_IS_PUBLIC)
            .isSchemaPublic(TABLE_6_SCHEMA_PUBLIC)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .queueName(TABLE_6_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_6_COLUMNS */
            .constraints(null) /* TABLE_6_CONSTRAINTS */
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_6_CREATED)
            .build();

    public final static TableDto TABLE_6_DTO = TableDto.builder()
            .id(TABLE_6_ID)
            .tdbid(DATABASE_2_ID)
            .internalName(TABLE_6_INTERNALNAME)
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

    public final static TableBriefDto TABLE_6_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_6_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_6_INTERNALNAME)
            .isVersioned(TABLE_6_VERSIONED)
            .isPublic(TABLE_6_IS_PUBLIC)
            .isSchemaPublic(TABLE_6_SCHEMA_PUBLIC)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .ownedBy(USER_1_ID)
            .build();

    public final static Long TABLE_7_ID = 7L;
    public final static String TABLE_7_NAME = "likes";
    public final static String TABLE_7_INTERNAL_NAME = "likes";
    public final static Boolean TABLE_7_VERSIONED = true;
    public final static Boolean TABLE_7_IS_PUBLIC = true;
    public final static Boolean TABLE_7_SCHEMA_PUBLIC = true;
    public final static Boolean TABLE_7_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_7_DESCRIPTION = "Some likes dataset";
    public final static String TABLE_7_QUEUE_NAME = TABLE_7_INTERNAL_NAME;
    public final static String TABLE_7_ROUTING_KEY = "dbrepo\\." + DATABASE_2_ID + "\\." + TABLE_7_ID;
    public final static Instant TABLE_7_CREATED = Instant.ofEpochSecond(1677400147L) /* 2023-02-26 08:29:07 (UTC) */;
    public final static Instant TABLE_7_LAST_MODIFIED = Instant.ofEpochSecond(1677400147L) /* 2023-02-26 08:29:07 (UTC) */;

    public final static Table TABLE_7 = Table.builder()
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
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_7_CREATED)
            .build();

    public final static TableDto TABLE_7_DTO = TableDto.builder()
            .id(TABLE_7_ID)
            .tdbid(DATABASE_2_ID)
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

    public final static TableBriefDto TABLE_7_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_7_ID)
            .databaseId(DATABASE_2_ID)
            .internalName(TABLE_7_INTERNAL_NAME)
            .isVersioned(TABLE_7_VERSIONED)
            .isPublic(TABLE_7_IS_PUBLIC)
            .isSchemaPublic(TABLE_7_SCHEMA_PUBLIC)
            .description(TABLE_7_DESCRIPTION)
            .name(TABLE_7_NAME)
            .ownedBy(USER_1_ID)
            .build();

    public final static Long TABLE_4_ID = 4L;
    public final static String TABLE_4_NAME = "Sensor 2";
    public final static String TABLE_4_INTERNALNAME = "sensor_2";
    public final static Boolean TABLE_4_VERSIONED = true;
    public final static Boolean TABLE_4_IS_PUBLIC = false;
    public final static Boolean TABLE_4_SCHEMA_PUBLIC = false;
    public final static Boolean TABLE_4_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_4_DESCRIPTION = "Hello sensor";
    public final static String TABLE_4_QUEUE_NAME = TABLE_4_INTERNALNAME;
    public final static String TABLE_4_ROUTING_KEY = "dbrepo\\." + DATABASE_1_ID + "\\." + TABLE_4_ID;
    public final static Instant TABLE_4_CREATED = Instant.ofEpochSecond(1677400175L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677400175L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Long TABLE_4_AVG_ROW_LENGTH = 0L;
    public final static Long TABLE_4_NUM_ROWS = 0L;
    public final static Long TABLE_4_DATA_LENGTH = 1000L;
    public final static Long TABLE_4_MAX_DATA_LENGTH = Long.MAX_VALUE;

    public final static Table TABLE_4 = Table.builder()
            .id(TABLE_4_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNALNAME)
            .description(TABLE_4_DESCRIPTION)
            .database(null /* DATABASE_1 */)
            .name(TABLE_4_NAME)
            .queueName(TABLE_4_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_4_COLUMNS */
            .constraints(null) /* TABLE_4_CONSTRAINTS */
            .isVersioned(TABLE_4_VERSIONED)
            .isPublic(TABLE_4_IS_PUBLIC)
            .isSchemaPublic(TABLE_4_SCHEMA_PUBLIC)
            .owner(USER_1)
            .ownedBy(USER_1_ID)
            .created(TABLE_4_CREATED)
            .lastModified(TABLE_4_LAST_MODIFIED)
            .avgRowLength(TABLE_4_AVG_ROW_LENGTH)
            .numRows(TABLE_4_NUM_ROWS)
            .dataLength(TABLE_4_DATA_LENGTH)
            .maxDataLength(TABLE_4_MAX_DATA_LENGTH)
            .build();

    public final static TableDto TABLE_4_DTO = TableDto.builder()
            .id(TABLE_4_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNALNAME)
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

    public final static TableBriefDto TABLE_4_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_4_ID)
            .databaseId(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNALNAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .isVersioned(TABLE_4_VERSIONED)
            .isPublic(TABLE_4_IS_PUBLIC)
            .isSchemaPublic(TABLE_4_SCHEMA_PUBLIC)
            .ownedBy(USER_1_ID)
            .build();

    public final static ColumnBriefDto TABLE_4_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(44L)
            .name("Timestamp")
            .internalName("timestamp")
            .columnType(ColumnTypeDto.TIMESTAMP)
            .build();

    public final static Long COLUMN_4_1_ID = 44L;

    public final static Long COLUMN_4_2_ID = 45L;

    public final static List<TableColumn> TABLE_4_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_4_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_4)
                    .name("Timestamp")
                    .internalName("timestamp")
                    .columnType(TableColumnType.TIMESTAMP)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_4)
                    .name("Value")
                    .internalName("value")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .build());

    public final static List<ColumnCreateDto> TABLE_4_COLUMNS_CREATE_DTO = List.of(ColumnCreateDto.builder()
                    .name("Timestamp")
                    .type(ColumnTypeDto.TIMESTAMP)
                    .nullAllowed(false)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Value")
                    .type(ColumnTypeDto.DECIMAL)
                    .nullAllowed(true)
                    .size(10L)
                    .d(10L)
                    .build());

    public final static ConstraintsCreateDto TABLE_4_CONSTRAINTS_CREATE_DTO = ConstraintsCreateDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>(Set.of("Timestamp")))
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>(List.of(List.of("Timestamp"))))
            .build();

    public final static TableCreateDto TABLE_4_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_4_NAME)
            .description(TABLE_4_DESCRIPTION)
            .columns(TABLE_4_COLUMNS_CREATE_DTO)
            .constraints(TABLE_4_CONSTRAINTS_CREATE_DTO)
            .build();

    public final static at.tuwien.api.database.table.internal.TableCreateDto TABLE_4_CREATE_INTERNAL_DTO = at.tuwien.api.database.table.internal.TableCreateDto.builder()
            .name(TABLE_4_NAME)
            .description(TABLE_4_DESCRIPTION)
            .columns(TABLE_4_COLUMNS_CREATE_DTO)
            .constraints(TABLE_4_CONSTRAINTS_CREATE_DTO)
            .build();

    public final static List<ColumnDto> TABLE_4_COLUMNS_DTO = List.of(ColumnDto.builder()
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

    public final static Long TABLE_8_ID = 8L;
    public final static Long TABLE_8_DATABASE_ID = DATABASE_3_ID;
    public final static String TABLE_8_NAME = "location";
    public final static String TABLE_8_INTERNAL_NAME = "mfcc";
    public final static Boolean TABLE_8_VERSIONED = true;
    public final static Boolean TABLE_8_IS_PUBLIC = false;
    public final static Boolean TABLE_8_SCHEMA_PUBLIC = false;
    public final static Boolean TABLE_8_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_8_DESCRIPTION = "Hello mfcc";
    public final static String TABLE_8_QUEUE_NAME = TABLE_8_INTERNAL_NAME;
    public final static String TABLE_8_ROUTING_KEY = "dbrepo\\." + DATABASE_3_ID + "\\." + TABLE_8_ID;
    public final static Instant TABLE_8_CREATED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_8_LAST_MODIFIED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;

    public final static Table TABLE_8 = Table.builder()
            .id(TABLE_8_ID)
            .tdbid(TABLE_8_DATABASE_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .isVersioned(TABLE_8_VERSIONED)
            .isPublic(TABLE_8_IS_PUBLIC)
            .isSchemaPublic(TABLE_8_SCHEMA_PUBLIC)
            .database(null /* DATABASE_1 */)
            .name(TABLE_8_NAME)
            .queueName(TABLE_8_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_8_COLUMNS */
            .constraints(null) /* TABLE_8_CONSTRAINTS */
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_8_CREATED)
            .lastModified(TABLE_8_LAST_MODIFIED)
            .build();

    public final static TableDto TABLE_8_DTO = TableDto.builder()
            .id(TABLE_8_ID)
            .tdbid(TABLE_8_DATABASE_ID)
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

    public final static TableBriefDto TABLE_8_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_8_ID)
            .databaseId(DATABASE_3_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .isVersioned(TABLE_8_VERSIONED)
            .isPublic(TABLE_8_IS_PUBLIC)
            .isSchemaPublic(TABLE_8_SCHEMA_PUBLIC)
            .name(TABLE_8_NAME)
            .ownedBy(USER_1_ID)
            .build();

    public final static PrivilegedTableDto TABLE_8_PRIVILEGED_DTO = PrivilegedTableDto.builder()
            .id(TABLE_8_ID)
            .tdbid(TABLE_8_DATABASE_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .isVersioned(TABLE_8_VERSIONED)
            .isPublic(TABLE_8_IS_PUBLIC)
            .isSchemaPublic(TABLE_8_SCHEMA_PUBLIC)
            .name(TABLE_8_NAME)
            .queueName(TABLE_8_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_8_COLUMNS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .isPublic(DATABASE_3_PUBLIC)
            .lastRetrieved(Instant.now())
            .build();

    public final static Long TABLE_9_ID = 9L;
    public final static Long TABLE_9_DATABASE_ID = DATABASE_4_ID;
    public final static String TABLE_9_NAME = "mfcc";
    public final static String TABLE_9_INTERNAL_NAME = "mfcc";
    public final static Boolean TABLE_9_VERSIONED = true;
    public final static Boolean TABLE_9_IS_PUBLIC = false;
    public final static Boolean TABLE_9_SCHEMA_PUBLIC = true;
    public final static Boolean TABLE_9_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_9_DESCRIPTION = "Hello mfcc";
    public final static String TABLE_9_QUEUE_NAME = TABLE_9_INTERNAL_NAME;
    public final static String TABLE_9_ROUTING_KEY = "dbrepo\\." + DATABASE_3_ID + "\\." + TABLE_9_ID;
    public final static Instant TABLE_9_CREATED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_9_LAST_MODIFIED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;

    public final static Table TABLE_9 = Table.builder()
            .id(TABLE_9_ID)
            .tdbid(TABLE_9_DATABASE_ID)
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
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_9_CREATED)
            .lastModified(TABLE_9_LAST_MODIFIED)
            .build();

    public final static TableDto TABLE_9_DTO = TableDto.builder()
            .id(TABLE_9_ID)
            .tdbid(TABLE_9_DATABASE_ID)
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

    public final static TableBriefDto TABLE_9_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_9_ID)
            .databaseId(DATABASE_4_ID)
            .internalName(TABLE_9_INTERNAL_NAME)
            .description(TABLE_9_DESCRIPTION)
            .isVersioned(TABLE_9_VERSIONED)
            .isPublic(TABLE_9_IS_PUBLIC)
            .isSchemaPublic(TABLE_9_SCHEMA_PUBLIC)
            .name(TABLE_9_NAME)
            .ownedBy(USER_1_ID)
            .build();

    public final static PrivilegedTableDto TABLE_9_PRIVILEGED_DTO = PrivilegedTableDto.builder()
            .id(TABLE_9_ID)
            .tdbid(TABLE_9_DATABASE_ID)
            .internalName(TABLE_9_INTERNAL_NAME)
            .description(TABLE_9_DESCRIPTION)
            .isVersioned(TABLE_9_VERSIONED)
            .isPublic(TABLE_9_IS_PUBLIC)
            .isSchemaPublic(TABLE_9_SCHEMA_PUBLIC)
            .name(TABLE_9_NAME)
            .queueName(TABLE_9_QUEUE_NAME)
            .columns(new LinkedList<>()) /* TABLE_9_COLUMNS_DTO */
            .owner(USER_1_BRIEF_DTO)
            .isPublic(DATABASE_3_PUBLIC)
            .lastRetrieved(Instant.now())
            .build();

    public final static Long COLUMN_9_1_ID = 78L;
    public final static String COLUMN_9_1_NAME = "location";
    public final static String COLUMN_9_1_INTERNAL_NAME = "location";

    public final static ColumnBriefDto TABLE_9_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_9_1_ID)
            .name(COLUMN_9_1_NAME)
            .internalName(COLUMN_9_1_INTERNAL_NAME)
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static Long COLUMN_9_2_ID = 79L;

    public final static Long COLUMN_9_3_ID = 80L;

    public final static List<TableColumn> TABLE_9_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_9_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_9)
                    .name(COLUMN_9_1_NAME)
                    .internalName(COLUMN_9_1_INTERNAL_NAME)
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
                    .build());

    public final static List<ColumnDto> TABLE_9_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_9_1_ID)
                    .ordinalPosition(0)
                    .name(COLUMN_9_1_NAME)
                    .internalName(COLUMN_9_1_INTERNAL_NAME)
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
                    .build());

    public final static Constraints TABLE_9_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_9)
                    .column(TABLE_9_COLUMNS.get(0))
                    .id(9L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_9_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_9_BRIEF_DTO)
                    .column(TABLE_9_COLUMNS_BRIEF_0_DTO)
                    .id(9L)
                    .build())))
            .build();

    public final static String QUEUE_NAME = "dbrepo";
    public final static String QUEUE_VHOST = "dbrepo";
    public final static Boolean QUEUE_AUTO_DELETE = false;
    public final static Boolean QUEUE_DURABLE = true;
    public final static Boolean QUEUE_EXCLUSIVE = false;
    public final static String QUEUE_TYPE = "quorum";

    public final static QueueDto QUEUE_DTO = QueueDto.builder()
            .name(QUEUE_NAME)
            .vhost(QUEUE_VHOST)
            .autoDelete(QUEUE_AUTO_DELETE)
            .durable(QUEUE_DURABLE)
            .exclusive(QUEUE_EXCLUSIVE)
            .type(QUEUE_TYPE)
            .build();

    public final static Long ONTOLOGY_1_ID = 1L;
    public final static String ONTOLOGY_1_PREFIX = "om2";
    public final static String ONTOLOGY_1_NEW_PREFIX = "om-2";
    public final static String ONTOLOGY_1_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/";
    public final static String ONTOLOGY_1_URI_PATTERN = "http://www.ontology-of-units-of-measure.org/resource/om-2/.*";
    public final static String ONTOLOGY_1_SPARQL_ENDPOINT = null;
    public final static Boolean ONTOLOGY_1_SPARQL = false;
    public final static String ONTOLOGY_1_RDF_PATH = "rdf/om-2.0.rdf";
    public final static Boolean ONTOLOGY_1_RDF = true;
    public final static UUID ONTOLOGY_1_CREATED_BY = USER_1_ID;

    public final static Ontology ONTOLOGY_1 = Ontology.builder()
            .id(ONTOLOGY_1_ID)
            .prefix(ONTOLOGY_1_PREFIX)
            .uri(ONTOLOGY_1_URI)
            .uriPattern(ONTOLOGY_1_URI_PATTERN)
            .sparqlEndpoint(ONTOLOGY_1_SPARQL_ENDPOINT)
            .rdfPath(ONTOLOGY_1_RDF_PATH)
            .build();

    public final static OntologyDto ONTOLOGY_1_DTO = OntologyDto.builder()
            .id(ONTOLOGY_1_ID)
            .prefix(ONTOLOGY_1_PREFIX)
            .uri(ONTOLOGY_1_URI)
            .uriPattern(ONTOLOGY_1_URI_PATTERN)
            .sparqlEndpoint(ONTOLOGY_1_SPARQL_ENDPOINT)
            .sparql(ONTOLOGY_1_SPARQL)
            .rdfPath(ONTOLOGY_1_RDF_PATH)
            .rdf(ONTOLOGY_1_RDF)
            .build();

    public final static OntologyBriefDto ONTOLOGY_1_BRIEF_DTO = OntologyBriefDto.builder()
            .id(ONTOLOGY_1_ID)
            .prefix(ONTOLOGY_1_PREFIX)
            .uri(ONTOLOGY_1_URI)
            .uriPattern(ONTOLOGY_1_URI_PATTERN)
            .sparql(ONTOLOGY_1_SPARQL)
            .rdf(ONTOLOGY_1_RDF)
            .build();

    public final static OntologyCreateDto ONTOLOGY_1_CREATE_DTO = OntologyCreateDto.builder()
            .prefix(ONTOLOGY_1_PREFIX)
            .uri(ONTOLOGY_1_URI)
            .sparqlEndpoint(ONTOLOGY_1_SPARQL_ENDPOINT)
            .build();

    public final static OntologyModifyDto ONTOLOGY_1_MODIFY_DTO = OntologyModifyDto.builder()
            .prefix(ONTOLOGY_1_NEW_PREFIX)
            .uri(ONTOLOGY_1_URI)
            .sparqlEndpoint(ONTOLOGY_1_SPARQL_ENDPOINT)
            .build();

    public final static Long ONTOLOGY_2_ID = 2L;
    public final static String ONTOLOGY_2_PREFIX = "wd";
    public final static String ONTOLOGY_2_URI = "http://www.wikidata.org/";
    public final static String ONTOLOGY_2_SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";
    public final static UUID ONTOLOGY_2_CREATED_BY = USER_1_ID;

    public final static Ontology ONTOLOGY_2 = Ontology.builder()
            .id(ONTOLOGY_2_ID)
            .prefix(ONTOLOGY_2_PREFIX)
            .uri(ONTOLOGY_2_URI)
            .sparqlEndpoint(ONTOLOGY_2_SPARQL_ENDPOINT)
            .build();

    public final static OntologyCreateDto ONTOLOGY_2_CREATE_DTO = OntologyCreateDto.builder()
            .prefix(ONTOLOGY_2_PREFIX)
            .uri(ONTOLOGY_2_URI)
            .sparqlEndpoint(ONTOLOGY_2_SPARQL_ENDPOINT)
            .build();

    public final static Long ONTOLOGY_3_ID = 3L;
    public final static String ONTOLOGY_3_PREFIX = "rdfs";
    public final static String ONTOLOGY_3_URI = "http://www.w3.org/2000/01/rdf-schema#";
    public final static String ONTOLOGY_3_SPARQL_ENDPOINT = null;
    public final static UUID ONTOLOGY_3_CREATED_BY = USER_1_ID;

    public final static Ontology ONTOLOGY_3 = Ontology.builder()
            .id(ONTOLOGY_3_ID)
            .prefix(ONTOLOGY_3_PREFIX)
            .uri(ONTOLOGY_3_URI)
            .sparqlEndpoint(ONTOLOGY_3_SPARQL_ENDPOINT)
            .build();

    public final static OntologyCreateDto ONTOLOGY_3_CREATE_DTO = OntologyCreateDto.builder()
            .prefix(ONTOLOGY_3_PREFIX)
            .uri(ONTOLOGY_3_URI)
            .sparqlEndpoint(ONTOLOGY_3_SPARQL_ENDPOINT)
            .build();

    public final static Long ONTOLOGY_4_ID = 4L;
    public final static String ONTOLOGY_4_PREFIX = "schema";
    public final static String ONTOLOGY_4_URI = "http://schema.org/";
    public final static String ONTOLOGY_4_SPARQL_ENDPOINT = null;
    public final static UUID ONTOLOGY_4_CREATED_BY = USER_1_ID;

    public final static Ontology ONTOLOGY_4 = Ontology.builder()
            .id(ONTOLOGY_4_ID)
            .prefix(ONTOLOGY_4_PREFIX)
            .uri(ONTOLOGY_4_URI)
            .sparqlEndpoint(ONTOLOGY_4_SPARQL_ENDPOINT)
            .build();

    public final static OntologyCreateDto ONTOLOGY_4_CREATE_DTO = OntologyCreateDto.builder()
            .prefix(ONTOLOGY_4_PREFIX)
            .uri(ONTOLOGY_4_URI)
            .sparqlEndpoint(ONTOLOGY_4_SPARQL_ENDPOINT)
            .build();

    public final static Long ONTOLOGY_5_ID = 5L;
    public final static String ONTOLOGY_5_PREFIX = "db";
    public final static String ONTOLOGY_5_URI = "http://dbpedia.org";
    public final static String ONTOLOGY_5_SPARQL_ENDPOINT = "http://dbpedia.org/sparql";
    public final static UUID ONTOLOGY_5_CREATED_BY = USER_1_ID;

    public final static Ontology ONTOLOGY_5 = Ontology.builder()
            .id(ONTOLOGY_5_ID)
            .prefix(ONTOLOGY_5_PREFIX)
            .uri(ONTOLOGY_5_URI)
            .sparqlEndpoint(ONTOLOGY_5_SPARQL_ENDPOINT)
            .build();

    public final static OntologyCreateDto ONTOLOGY_5_CREATE_DTO = OntologyCreateDto.builder()
            .prefix(ONTOLOGY_5_PREFIX)
            .uri(ONTOLOGY_5_URI)
            .sparqlEndpoint(ONTOLOGY_5_SPARQL_ENDPOINT)
            .build();

    public final static Long COLUMN_8_1_ID = 75L;
    public final static Integer COLUMN_8_1_ORDINALPOS = 0;
    public final static String COLUMN_8_1_NAME = "ID";
    public final static String COLUMN_8_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_8_1_TYPE = TableColumnType.BIGINT;
    public final static ColumnTypeDto COLUMN_8_1_TYPE_DTO = ColumnTypeDto.BIGINT;
    public final static Boolean COLUMN_8_1_NULL = false;
    public final static Boolean COLUMN_8_1_AUTO_GENERATED = true;

    public final static Long COLUMN_8_2_ID = 76L;
    public final static Integer COLUMN_8_2_ORDINALPOS = 1;
    public final static String COLUMN_8_2_NAME = "Value";
    public final static String COLUMN_8_2_INTERNAL_NAME = "value";
    public final static TableColumnType COLUMN_8_2_TYPE = TableColumnType.DECIMAL;
    public final static ColumnTypeDto COLUMN_8_2_TYPE_DTO = ColumnTypeDto.DECIMAL;
    public final static Long COLUMN_8_2_SIZE = 10L;
    public final static Long COLUMN_8_2_D = 10L;
    public final static Boolean COLUMN_8_2_NULL = false;
    public final static Boolean COLUMN_8_2_AUTO_GENERATED = false;

    public final static Long COLUMN_8_3_ID = 77L;
    public final static Integer COLUMN_8_3_ORDINALPOS = 2;
    public final static String COLUMN_8_3_NAME = "raw";
    public final static String COLUMN_8_3_INTERNAL_NAME = "raw";
    public final static TableColumnType COLUMN_8_3_TYPE = TableColumnType.LONGBLOB;
    public final static ColumnTypeDto COLUMN_8_3_TYPE_DTO = ColumnTypeDto.LONGBLOB;
    public final static Boolean COLUMN_8_3_NULL = true;
    public final static Boolean COLUMN_8_3_AUTO_GENERATED = false;

    public final static ColumnBriefDto TABLE_8_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_8_1_ID)
            .name(COLUMN_8_1_NAME)
            .internalName(COLUMN_8_1_INTERNAL_NAME)
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static List<TableColumn> TABLE_8_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_8_1_ID)
                    .ordinalPosition(COLUMN_8_1_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_1_NAME)
                    .internalName(COLUMN_8_1_INTERNAL_NAME)
                    .columnType(COLUMN_8_1_TYPE)
                    .isNullAllowed(COLUMN_8_1_NULL)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_8_2_ID)
                    .ordinalPosition(COLUMN_8_2_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_2_NAME)
                    .internalName(COLUMN_8_2_INTERNAL_NAME)
                    .columnType(COLUMN_8_2_TYPE)
                    .isNullAllowed(COLUMN_8_2_NULL)
                    .size(COLUMN_8_2_SIZE)
                    .d(COLUMN_8_2_D)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_8_3_ID)
                    .ordinalPosition(COLUMN_8_3_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_3_NAME)
                    .internalName(COLUMN_8_3_INTERNAL_NAME)
                    .columnType(COLUMN_8_3_TYPE)
                    .isNullAllowed(COLUMN_8_3_NULL)
                    .build());

    public final static List<ColumnDto> TABLE_8_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_8_1_ID)
                    .ordinalPosition(COLUMN_8_1_ORDINALPOS)
                    .name(COLUMN_8_1_NAME)
                    .internalName(COLUMN_8_1_INTERNAL_NAME)
                    .columnType(COLUMN_8_1_TYPE_DTO)
                    .isNullAllowed(COLUMN_8_1_NULL)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_8_2_ID)
                    .ordinalPosition(COLUMN_8_2_ORDINALPOS)
                    .name(COLUMN_8_2_NAME)
                    .internalName(COLUMN_8_2_INTERNAL_NAME)
                    .columnType(COLUMN_8_2_TYPE_DTO)
                    .isNullAllowed(COLUMN_8_2_NULL)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_8_3_ID)
                    .ordinalPosition(COLUMN_8_3_ORDINALPOS)
                    .name(COLUMN_8_3_NAME)
                    .internalName(COLUMN_8_3_INTERNAL_NAME)
                    .columnType(COLUMN_8_3_TYPE_DTO)
                    .isNullAllowed(COLUMN_8_3_NULL)
                    .build());

    public final static Long TABLE_8_DATA_COUNT = 6L;
    @SuppressWarnings("java:S3599")
    public final static List<Map<String, Object>> TABLE_8_DATA_DTO = new LinkedList<>(List.of(
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
    public final static TableStatisticDto TABLE_8_STATISTIC_DTO = TableStatisticDto.builder()
            .columns(new HashMap<>() {{
                put(COLUMN_8_1_INTERNAL_NAME, ColumnStatisticDto.builder()
                        .min(BigDecimal.valueOf(11.2))
                        .max(BigDecimal.valueOf(23.1))
                        .mean(BigDecimal.valueOf(13.5333))
                        .median(BigDecimal.valueOf(11.4))
                        .stdDev(BigDecimal.valueOf(4.2952))
                        .build());
            }})
            .build();

    public final static Long QUERY_1_ID = 1L;
    public final static String QUERY_1_STATEMENT = "SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` ORDER BY id ASC";
    public final static String QUERY_1_DOI = null;
    public final static Long QUERY_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long QUERY_1_DATABASE_ID = DATABASE_1_ID;
    public final static Long QUERY_1_RESULT_NUMBER = 2L;
    public final static String QUERY_1_QUERY_HASH = "a3b8ac39e38167d14cf3a9c20a69e4b6954d049525390b973a2c23064953a992";
    public final static String QUERY_1_RESULT_HASH = "8358c8ade4849d2094ab5bb29127afdae57e6bb5acb1db7af603813d406c467a";
    public final static Instant QUERY_1_CREATED = Instant.ofEpochSecond(1677648377L);
    public final static Instant QUERY_1_EXECUTION = Instant.now();
    public final static Boolean QUERY_1_PERSISTED = true;

    public final static QueryDto QUERY_1_DTO = QueryDto.builder()
            .id(QUERY_1_ID)
            .databaseId(QUERY_1_DATABASE_ID)
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .execution(QUERY_1_EXECUTION)
            .owner(USER_1_BRIEF_DTO)
            .isPersisted(QUERY_1_PERSISTED)
            .resultNumber(3L)
            .build();

    public final static QueryBriefDto QUERY_1_BRIEF_DTO = QueryBriefDto.builder()
            .id(QUERY_1_ID)
            .databaseId(QUERY_1_DATABASE_ID)
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .execution(QUERY_1_EXECUTION)
            .owner(USER_1_BRIEF_DTO)
            .isPersisted(QUERY_1_PERSISTED)
            .resultNumber(3L)
            .build();

    public final static Long QUERY_2_ID = 2L;
    public final static String QUERY_2_STATEMENT = "SELECT `location` FROM `weather_aus`";
    public final static String QUERY_2_QUERY_HASH = "a2d2dd94ebc7653bb5a3b55dd8ed5e91d3d13c225c6855a1eb4eb7ca14c36ced";
    public final static Long QUERY_2_RESULT_NUMBER = 2L;
    public final static String QUERY_2_RESULT_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static Instant QUERY_2_CREATED = Instant.now().minus(2, MINUTES);
    public final static Instant QUERY_2_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_2_LAST_MODIFIED = Instant.ofEpochSecond(1541588352L);
    public final static Boolean QUERY_2_PERSISTED = false;

    public final static QueryDto QUERY_2_DTO = QueryDto.builder()
            .id(QUERY_2_ID)
            .databaseId(DATABASE_2_ID)
            .query(QUERY_2_STATEMENT)
            .queryNormalized(QUERY_2_STATEMENT)
            .resultNumber(QUERY_2_RESULT_NUMBER)
            .resultHash(QUERY_2_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_2_QUERY_HASH)
            .execution(QUERY_2_EXECUTION)
            .isPersisted(QUERY_2_PERSISTED)
            .resultNumber(3L)
            .build();

    public final static Long QUERY_3_ID = 3L;
    public final static String QUERY_3_STATEMENT = "SELECT `location`, `mintemp` FROM `weather_aus` WHERE `mintemp` > 10";
    public final static String QUERY_3_QUERY_HASH = "a3d3dd94ebc7653bb5a3b55dd8ed5e91d3d13c335c6855a1eb4eb7ca14c36ced";
    public final static Long QUERY_3_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long QUERY_3_DATABASE_ID = DATABASE_1_ID;
    public final static String QUERY_3_RESULT_HASH = "ff3f7cbe1b96d396957f6e39e55b8b1b577fa3d305d4795af99594cfd30cb80d";
    public final static Instant QUERY_3_CREATED = Instant.now().minus(3, MINUTES);
    public final static Instant QUERY_3_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_3_LAST_MODIFIED = Instant.ofEpochSecond(1541588353L);
    public final static Long QUERY_3_RESULT_NUMBER = 2L;
    public final static Boolean QUERY_3_PERSISTED = true;

    public final static QueryDto QUERY_3_DTO = QueryDto.builder()
            .id(QUERY_3_ID)
            .databaseId(QUERY_3_DATABASE_ID)
            .query(QUERY_3_STATEMENT)
            .queryNormalized(QUERY_3_STATEMENT)
            .resultNumber(QUERY_3_RESULT_NUMBER)
            .resultHash(QUERY_3_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_3_QUERY_HASH)
            .execution(QUERY_3_EXECUTION)
            .isPersisted(QUERY_3_PERSISTED)
            .resultNumber(2L)
            .build();
    
    public final static Long QUERY_7_ID = 7L;
    public final static String QUERY_7_STATEMENT = "SELECT id, date, a.location, lat, lng FROM weather_aus a JOIN weather_location l on a.location = l.location WHERE date = '2008-12-01'";
    public final static String QUERY_7_QUERY_HASH = "df7da3801dfb5c191ff6711d79ce6455f3c09ec8323ce1ff7208ab85387263f5";
    public final static String QUERY_7_RESULT_HASH = "ff4f7cbe1b96d496957f6e49e55b8b1b577fa4d405d4795af99594cfd40cb80d";
    public final static Instant QUERY_7_CREATED = Instant.now().minus(4, MINUTES);
    public final static Instant QUERY_7_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_7_LAST_MODIFIED = Instant.ofEpochSecond(1541588454L);
    public final static Long QUERY_7_RESULT_NUMBER = 6L;
    public final static Long QUERY_7_RESULT_ID = 4L;
    public final static Boolean QUERY_7_PERSISTED = false;

    public final static QueryDto QUERY_7_DTO = QueryDto.builder()
            .id(QUERY_7_ID)
            .databaseId(DATABASE_4_ID)
            .query(QUERY_7_STATEMENT)
            .queryNormalized(QUERY_7_STATEMENT)
            .resultNumber(QUERY_7_RESULT_NUMBER)
            .resultHash(QUERY_7_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_7_QUERY_HASH)
            .execution(QUERY_7_EXECUTION)
            .isPersisted(QUERY_7_PERSISTED)
            .resultNumber(2L)
            .build();

    public final static Long QUERY_4_ID = 4L;
    public final static String QUERY_4_STATEMENT = "SELECT `id`, `value` FROM `mfcc`";
    public final static String QUERY_4_QUERY_HASH = "df7da3801dfb5c191ff6711d79ce6455f3c09ec8323ce1ff7208ab85387263f5";
    public final static Long QUERY_4_CONTAINER_ID = CONTAINER_3_ID;
    public final static Long QUERY_4_DATABASE_ID = DATABASE_3_ID;
    public final static String QUERY_4_RESULT_HASH = "ff4f7cbe1b96d496957f6e49e55b8b1b577fa4d405d4795af99594cfd40cb80d";
    public final static Instant QUERY_4_CREATED = Instant.now().minus(4, MINUTES);
    public final static Instant QUERY_4_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_4_LAST_MODIFIED = Instant.ofEpochSecond(1541588454L);
    public final static Long QUERY_4_RESULT_NUMBER = 6L;
    public final static Long QUERY_4_RESULT_ID = 4L;
    public final static Boolean QUERY_4_PERSISTED = false;

    public final static QueryDto QUERY_4 = QueryDto.builder()
            .id(QUERY_4_ID)
            .query(QUERY_4_STATEMENT)
            .queryHash(QUERY_4_QUERY_HASH)
            .resultHash(QUERY_4_RESULT_HASH)
            .execution(QUERY_4_EXECUTION)
            .isPersisted(QUERY_4_PERSISTED)
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .owner(USER_3_BRIEF_DTO)
            .isPersisted(QUERY_4_PERSISTED)
            .build();

    public final static List<Map<String, Object>> QUERY_4_RESULT_DTO = new LinkedList<>(List.of(
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

    public final static QueryDto QUERY_4_DTO = QueryDto.builder()
            .id(QUERY_4_ID)
            .databaseId(QUERY_4_DATABASE_ID)
            .query(QUERY_4_STATEMENT)
            .queryNormalized(QUERY_4_STATEMENT)
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .resultHash(QUERY_4_RESULT_HASH)
            .queryHash(QUERY_4_QUERY_HASH)
            .execution(QUERY_4_EXECUTION)
            .isPersisted(QUERY_4_PERSISTED)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final static Long QUERY_5_ID = 5L;
    public final static String QUERY_5_STATEMENT = "SELECT `id`, `value` FROM `mfcc` WHERE `value` > 0";
    public final static String QUERY_5_QUERY_HASH = "6d6dc48b12cdfd959d39a62887334a6bbd529b93eed4f211f3f671bd9e7d6225";
    public final static Long QUERY_5_CONTAINER_ID = CONTAINER_3_ID;
    public final static Long QUERY_5_DATABASE_ID = DATABASE_3_ID;
    public final static String QUERY_5_RESULT_HASH = "ff5f7cbe1b96d596957f6e59e55b8b1b577fa5d505d5795af99595cfd50cb80d";
    public final static Instant QUERY_5_CREATED = Instant.now().minus(5, MINUTES);
    public final static Instant QUERY_5_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_5_LAST_MODIFIED = Instant.ofEpochSecond(1551588555L);
    public final static Long QUERY_5_RESULT_NUMBER = 6L;
    public final static Boolean QUERY_5_PERSISTED = true;

    public final static QueryDto QUERY_5_DTO = QueryDto.builder()
            .id(QUERY_5_ID)
            .databaseId(QUERY_5_DATABASE_ID)
            .query(QUERY_5_STATEMENT)
            .queryNormalized(QUERY_5_STATEMENT)
            .resultNumber(QUERY_5_RESULT_NUMBER)
            .resultHash(QUERY_5_RESULT_HASH)
            .queryHash(QUERY_5_QUERY_HASH)
            .execution(QUERY_5_EXECUTION)
            .isPersisted(QUERY_5_PERSISTED)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final static List<Map<String, Object>> QUERY_5_RESULT_DTO = new LinkedList<>(List.of(
            Map.of("id", BigInteger.valueOf(1L), "value", 11.2),
            Map.of("id", BigInteger.valueOf(2L), "value", 11.3),
            Map.of("id", BigInteger.valueOf(3L), "value", 11.4),
            Map.of("id", BigInteger.valueOf(4L), "value", 11.9),
            Map.of("id", BigInteger.valueOf(5L), "value", 12.3),
            Map.of("id", BigInteger.valueOf(6L), "value", 23.1)
    ));

    public final static Long QUERY_6_ID = 6L;
    public final static String QUERY_6_STATEMENT = "SELECT `location` FROM `weather_aus` WHERE `id` = 1";
    public final static String QUERY_6_QUERY_HASH = "6d6dc48b12cdfd959d39a62887334a6bbd529b93eed4f211f3f671bd9e7d6225";
    public final static String QUERY_6_RESULT_HASH = "ff5f7cbe1b96d596957f6e59e55b8b1b577fa5d505d5795af99595cfd50cb80d";
    public final static Instant QUERY_6_CREATED = Instant.now().minus(5, MINUTES);
    public final static Instant QUERY_6_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_6_LAST_MODIFIED = Instant.ofEpochSecond(1551588555L);
    public final static Long QUERY_6_RESULT_NUMBER = 1L;
    public final static Boolean QUERY_6_PERSISTED = true;

    public final static QueryDto QUERY_6_DTO = QueryDto.builder()
            .id(QUERY_6_ID)
            .databaseId(DATABASE_2_ID)
            .query(QUERY_6_STATEMENT)
            .queryNormalized(QUERY_6_STATEMENT)
            .resultNumber(QUERY_6_RESULT_NUMBER)
            .resultHash(QUERY_6_RESULT_HASH)
            .owner(USER_1_BRIEF_DTO)
            .queryHash(QUERY_6_QUERY_HASH)
            .execution(QUERY_6_EXECUTION)
            .isPersisted(QUERY_6_PERSISTED)
            .build();

    public final static ColumnBriefDto TABLE_1_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(1L)
            .name("id")
            .internalName("id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static Long COLUMN_1_1_ID = 1L;

    public final static Long COLUMN_1_2_ID = 2L;

    public final static Long COLUMN_1_3_ID = 3L;

    public final static Long COLUMN_1_4_ID = 4L;

    public final static Long COLUMN_1_5_ID = 5L;

    public final static List<TableColumn> TABLE_1_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_1_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_1)
                    .name("id")
                    .internalName("id")
                    .columnType(TableColumnType.SERIAL)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_1)
                    .name("Date")
                    .internalName("date")
                    .columnType(TableColumnType.DATE)
                    .isNullAllowed(true)
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
                    .concept(CONCEPT_1)
                    .unit(UNIT_1)
                    .isNullAllowed(true)
                    .build());

    public final static List<ColumnCreateDto> TABLE_1_COLUMNS_CREATE_DTO = List.of(ColumnCreateDto.builder()
                    .name("id")
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Date")
                    .type(ColumnTypeDto.DATE)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Location")
                    .type(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("MinTemp")
                    .type(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Rainfall")
                    .type(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .nullAllowed(true)
                    .conceptUri(CONCEPT_1_URI)
                    .unitUri(UNIT_1_URI)
                    .build());

    public final static ConstraintsCreateDto TABLE_1_CONSTRAINTS_CREATE_DTO = ConstraintsCreateDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>(List.of("id")))
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>(List.of(List.of("date"))))
            .build();

    public final static ConstraintsCreateDto TABLE_1_CONSTRAINTS_CREATE_INVALID_DTO = ConstraintsCreateDto.builder()
            .checks(new LinkedHashSet<>())
            .primaryKey(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>(List.of(List.of("date"))))
            .build();

    public final static TableCreateDto TABLE_1_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_1_NAME)
            .description(TABLE_1_DESCRIPTION)
            .columns(TABLE_1_COLUMNS_CREATE_DTO)
            .constraints(TABLE_1_CONSTRAINTS_CREATE_DTO)
            .build();

    public final static at.tuwien.api.database.table.internal.TableCreateDto TABLE_1_CREATE_INTERNAL_DTO = at.tuwien.api.database.table.internal.TableCreateDto.builder()
            .name(TABLE_1_NAME)
            .description(TABLE_1_DESCRIPTION)
            .columns(TABLE_1_COLUMNS_CREATE_DTO)
            .constraints(TABLE_1_CONSTRAINTS_CREATE_DTO)
            .build();

    public final static at.tuwien.api.database.table.internal.TableCreateDto TABLE_1_CREATE_INTERNAL_INVALID_DTO = at.tuwien.api.database.table.internal.TableCreateDto.builder()
            .name(TABLE_1_NAME)
            .description(TABLE_1_DESCRIPTION)
            .columns(TABLE_1_COLUMNS_CREATE_DTO)
            .constraints(TABLE_1_CONSTRAINTS_CREATE_INVALID_DTO)
            .build();

    public final static Long COLUMN_2_1_ID = 6L;

    public final static Long COLUMN_2_2_ID = 7L;

    public final static Long COLUMN_2_3_ID = 8L;

    public final static List<TableColumn> TABLE_2_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_2_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_2)
                    .name("location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
                    .build());

    public final static ColumnBriefDto TABLE_2_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_2_1_ID)
            .name("location")
            .internalName("location")
            .columnType(ColumnTypeDto.VARCHAR)
            .build();

    public final static ColumnBriefDto TABLE_2_COLUMNS_BRIEF_2_DTO = ColumnBriefDto.builder()
            .id(COLUMN_2_3_ID)
            .name("lng")
            .internalName("lng")
            .columnType(ColumnTypeDto.DECIMAL)
            .build();

    public final static List<ColumnDto> TABLE_2_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_2_1_ID)
                    .tableId(TABLE_2_ID)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
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
                    .enums(null)
                    .sets(null)
                    .build());

    public final static Long COLUMN_3_1_ID = 9L;

    public final static Long COLUMN_3_2_ID = 10L;

    public final static Long COLUMN_3_3_ID = 11L;

    public final static Long COLUMN_3_4_ID = 12L;

    public final static Long COLUMN_3_5_ID = 13L;

    public final static Long COLUMN_3_6_ID = 14L;

    public final static Long COLUMN_3_7_ID = 15L;

    public final static Long COLUMN_3_8_ID = 16L;

    public final static Long COLUMN_3_9_ID = 17L;

    public final static Long COLUMN_3_10_ID = 18L;

    public final static Long COLUMN_3_11_ID = 19L;

    public final static Long COLUMN_3_12_ID = 20L;

    public final static Long COLUMN_3_13_ID = 21L;

    public final static Long COLUMN_3_14_ID = 22L;

    public final static Long COLUMN_3_15_ID = 23L;

    public final static Long COLUMN_3_16_ID = 24L;

    public final static Long COLUMN_3_17_ID = 25L;

    public final static Long COLUMN_3_18_ID = 26L;

    public final static Long COLUMN_3_19_ID = 27L;

    public final static Long COLUMN_3_20_ID = 28L;

    public final static Long COLUMN_3_21_ID = 29L;

    public final static Long COLUMN_3_22_ID = 30L;

    public final static Long COLUMN_3_23_ID = 31L;

    public final static Long COLUMN_3_24_ID = 32L;

    public final static Long COLUMN_3_25_ID = 33L;

    public final static Long COLUMN_3_26_ID = 34L;

    public final static Long COLUMN_3_27_ID = 35L;

    public final static Long COLUMN_3_28_ID = 36L;

    public final static Long COLUMN_3_29_ID = 37L;

    public final static Long COLUMN_3_30_ID = 38L;

    public final static Long COLUMN_3_31_ID = 39L;

    public final static Long COLUMN_3_32_ID = 40L;

    public final static Long COLUMN_3_33_ID = 41L;

    public final static Long COLUMN_3_34_ID = 42L;

    public final static Long COLUMN_3_35_ID = 43L;

    public final static ColumnBriefDto TABLE_3_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_3_1_ID)
            .columnType(ColumnTypeDto.BIGINT)
            .name("id")
            .internalName("id")
            .build();

    public final static List<TableColumn> TABLE_3_COLUMNS = List.of(TableColumn.builder()
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

    public final static List<ColumnDto> TABLE_3_COLUMNS_DTO = List.of(ColumnDto.builder()
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

    public final static Long COLUMN_5_1_ID = 46L;

    public final static Long COLUMN_5_2_ID = 47L;

    public final static Long COLUMN_5_3_ID = 48L;

    public final static Long COLUMN_5_4_ID = 49L;

    public final static Long COLUMN_5_5_ID = 50L;

    public final static Long COLUMN_5_6_ID = 51L;

    public final static Long COLUMN_5_7_ID = 52L;

    public final static Long COLUMN_5_8_ID = 53L;

    public final static Long COLUMN_5_9_ID = 54L;

    public final static Long COLUMN_5_10_ID = 55L;

    public final static Long COLUMN_5_11_ID = 56L;

    public final static Long COLUMN_5_12_ID = 57L;

    public final static Long COLUMN_5_13_ID = 58L;

    public final static Long COLUMN_5_14_ID = 59L;

    public final static Long COLUMN_5_15_ID = 60L;

    public final static Long COLUMN_5_16_ID = 61L;

    public final static Long COLUMN_5_17_ID = 62L;

    public final static Long COLUMN_5_18_ID = 63L;

    public final static Long COLUMN_5_19_ID = 64L;

    public final static Long COLUMN_5_20_ID = 65L;

    public final static Long COLUMN_5_21_ID = 66L;

    public final static ColumnBriefDto TABLE_5_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_5_1_ID)
            .name("id")
            .internalName("id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static List<TableColumn> TABLE_5_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_5_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_5)
                    .name("id")
                    .internalName("id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_5)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_3_ID)
                    .ordinalPosition(2)
                    .table(TABLE_5)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_4_ID)
                    .ordinalPosition(3)
                    .table(TABLE_5)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_5_ID)
                    .ordinalPosition(4)
                    .table(TABLE_5)
                    .name("Bread")
                    .internalName("bread")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_6_ID)
                    .ordinalPosition(5)
                    .table(TABLE_5)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_7_ID)
                    .ordinalPosition(6)
                    .table(TABLE_5)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_8_ID)
                    .ordinalPosition(7)
                    .table(TABLE_5)
                    .name("Water")
                    .internalName("water")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_9_ID)
                    .ordinalPosition(8)
                    .table(TABLE_5)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_10_ID)
                    .ordinalPosition(9)
                    .table(TABLE_5)
                    .name("Waterborne")
                    .internalName("waterborne")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_11_ID)
                    .ordinalPosition(10)
                    .table(TABLE_5)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
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
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_14_ID)
                    .ordinalPosition(13)
                    .table(TABLE_5)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_15_ID)
                    .ordinalPosition(14)
                    .table(TABLE_5)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_16_ID)
                    .ordinalPosition(15)
                    .table(TABLE_5)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_17_ID)
                    .ordinalPosition(16)
                    .table(TABLE_5)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(TableColumnType.INT)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_18_ID)
                    .ordinalPosition(17)
                    .table(TABLE_5)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_19_ID)
                    .ordinalPosition(18)
                    .table(TABLE_5)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_20_ID)
                    .ordinalPosition(19)
                    .table(TABLE_5)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_21_ID)
                    .ordinalPosition(20)
                    .table(TABLE_5)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .build());

    public final static List<ColumnDto> TABLE_5_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_5_1_ID)
                    .ordinalPosition(0)
                    .tableId(TABLE_5_ID)
                    .name("id")
                    .internalName("id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_2_ID)
                    .ordinalPosition(1)
                    .tableId(TABLE_5_ID)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_3_ID)
                    .ordinalPosition(2)
                    .tableId(TABLE_5_ID)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_4_ID)
                    .ordinalPosition(3)
                    .tableId(TABLE_5_ID)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_5_ID)
                    .ordinalPosition(4)
                    .tableId(TABLE_5_ID)
                    .name("Bread")
                    .internalName("bread")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_6_ID)
                    .ordinalPosition(5)
                    .tableId(TABLE_5_ID)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_7_ID)
                    .ordinalPosition(6)
                    .tableId(TABLE_5_ID)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_8_ID)
                    .ordinalPosition(7)
                    .tableId(TABLE_5_ID)
                    .name("Water")
                    .internalName("water")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_9_ID)
                    .ordinalPosition(8)
                    .tableId(TABLE_5_ID)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_10_ID)
                    .ordinalPosition(9)
                    .tableId(TABLE_5_ID)
                    .name("Waterborne")
                    .internalName("waterborne")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_11_ID)
                    .ordinalPosition(10)
                    .tableId(TABLE_5_ID)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_12_ID)
                    .ordinalPosition(11)
                    .tableId(TABLE_5_ID)
                    .name("Predator")
                    .internalName("predator")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_13_ID)
                    .ordinalPosition(12)
                    .tableId(TABLE_5_ID)
                    .name("Backbone")
                    .internalName("backbone")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_14_ID)
                    .ordinalPosition(13)
                    .tableId(TABLE_5_ID)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_15_ID)
                    .ordinalPosition(14)
                    .tableId(TABLE_5_ID)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_16_ID)
                    .ordinalPosition(15)
                    .tableId(TABLE_5_ID)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_17_ID)
                    .ordinalPosition(16)
                    .tableId(TABLE_5_ID)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(ColumnTypeDto.INT)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_18_ID)
                    .ordinalPosition(17)
                    .tableId(TABLE_5_ID)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_19_ID)
                    .ordinalPosition(18)
                    .tableId(TABLE_5_ID)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_20_ID)
                    .ordinalPosition(19)
                    .tableId(TABLE_5_ID)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_5_21_ID)
                    .ordinalPosition(20)
                    .tableId(TABLE_5_ID)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .build());

    public final static List<ForeignKeyCreateDto> TABLE_5_FOREIGN_KEYS_INVALID_CREATE = List.of(ForeignKeyCreateDto.builder()
            .columns(new LinkedList<>(List.of("somecolumn")))
            .referencedTable("sometable")
            .referencedColumns(new LinkedList<>(List.of("someothercolumn")))
            .build());

    public final static ConstraintsCreateDto TABLE_5_CONSTRAINTS_INVALID_CREATE = ConstraintsCreateDto.builder()
            .foreignKeys(TABLE_5_FOREIGN_KEYS_INVALID_CREATE)
            .build();

    public final static List<ColumnCreateDto> TABLE_5_COLUMNS_CREATE = List.of(ColumnCreateDto.builder()
                    .name("id")
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Animal Name")
                    .type(ColumnTypeDto.VARCHAR)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Hair")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Feathers")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Bread")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Eggs")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Milk")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Water")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Airborne")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Waterborne")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Aquantic")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Predator")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Backbone")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Breathes")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Venomous")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Fin")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Legs")
                    .type(ColumnTypeDto.INT)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Tail")
                    .type(ColumnTypeDto.DECIMAL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Domestic")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Catsize")
                    .type(ColumnTypeDto.BOOL)
                    .nullAllowed(true)
                    .build(),
            ColumnCreateDto.builder()
                    .name("Class Type")
                    .type(ColumnTypeDto.DECIMAL)
                    .nullAllowed(true)
                    .build());

    public final static ConstraintsCreateDto TABLE_5_CREATE_CONSTRAINTS_DTO = ConstraintsCreateDto.builder()
            .primaryKey(Set.of("id"))
            .uniques(new LinkedList<>(List.of(List.of("id"))))
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .build();

    public final static TableCreateDto TABLE_5_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_5_NAME)
            .description(TABLE_5_DESCRIPTION)
            .columns(TABLE_5_COLUMNS_CREATE)
            .constraints(TABLE_5_CREATE_CONSTRAINTS_DTO)
            .build();

    public final static TableCreateDto TABLE_5_INVALID_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_5_NAME)
            .description(TABLE_5_DESCRIPTION)
            .columns(TABLE_5_COLUMNS_CREATE)
            .constraints(TABLE_5_CONSTRAINTS_INVALID_CREATE)
            .build();

    public final static List<TableColumn> TABLE_6_COLUMNS = List.of(TableColumn.builder()
                    .id(67L)
                    .ordinalPosition(0)
                    .table(TABLE_6)
                    .name("id")
                    .internalName("id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(68L)
                    .ordinalPosition(1)
                    .table(TABLE_6)
                    .name("firstname")
                    .internalName("firstname")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(69L)
                    .ordinalPosition(2)
                    .table(TABLE_6)
                    .name("lastname")
                    .internalName("lastname")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(70L)
                    .ordinalPosition(3)
                    .table(TABLE_6)
                    .name("birth")
                    .internalName("birth")
                    .columnType(TableColumnType.YEAR)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(71L)
                    .ordinalPosition(4)
                    .table(TABLE_6)
                    .name("reminder")
                    .internalName("reminder")
                    .columnType(TableColumnType.TIME)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(72L)
                    .ordinalPosition(5)
                    .table(TABLE_6)
                    .name("ref_id")
                    .internalName("ref_id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(true)
                    .build());

    public final static ColumnBriefDto TABLE_6_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(67L)
            .name("id")
            .internalName("id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static List<ColumnDto> TABLE_6_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(67L)
                    .ordinalPosition(0)
                    .tableId(TABLE_6_ID)
                    .name("id")
                    .internalName("id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(68L)
                    .ordinalPosition(1)
                    .tableId(TABLE_6_ID)
                    .name("firstname")
                    .internalName("firstname")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(69L)
                    .ordinalPosition(2)
                    .tableId(TABLE_6_ID)
                    .name("lastname")
                    .internalName("lastname")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(70L)
                    .ordinalPosition(3)
                    .tableId(TABLE_6_ID)
                    .name("birth")
                    .internalName("birth")
                    .columnType(ColumnTypeDto.YEAR)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(71L)
                    .ordinalPosition(4)
                    .tableId(TABLE_6_ID)
                    .name("reminder")
                    .internalName("reminder")
                    .columnType(ColumnTypeDto.TIME)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(72L)
                    .ordinalPosition(5)
                    .tableId(TABLE_6_ID)
                    .name("ref_id")
                    .internalName("ref_id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(true)
                    .build());

    public final static List<List<String>> TABLE_6_UNIQUES_CREATE = List.of(
            List.of("firstname", "lastname"));

    public final static List<ForeignKeyCreateDto> TABLE_6_FOREIGN_KEYS_CREATE = List.of(ForeignKeyCreateDto.builder()
            .columns(new LinkedList<>(List.of("ref_id")))
            .referencedTable("zoo")
            .referencedColumns(new LinkedList<>(List.of("id")))
            .build());

    public final static Set<String> TABLE_6_CHECKS_CREATE = Set.of("firstname != lastname");

    public final static ConstraintsCreateDto TABLE_6_CONSTRAINTS_CREATE = ConstraintsCreateDto.builder()
            .uniques(TABLE_6_UNIQUES_CREATE)
            .foreignKeys(TABLE_6_FOREIGN_KEYS_CREATE)
            .checks(TABLE_6_CHECKS_CREATE)
            .primaryKey(Set.of("id"))
            .build();

    public final static List<ColumnCreateDto> TABLE_6_COLUMNS_CREATE = List.of(
            ColumnCreateDto.builder()
                    .name("name_id")
                    .type(ColumnTypeDto.BIGINT)
                    .nullAllowed(false)
                    .build(),
            ColumnCreateDto.builder()
                    .name("zoo_id")
                    .type(ColumnTypeDto.BIGINT)
                    .size(255L)
                    .nullAllowed(false)
                    .build());

    public final static TableCreateDto TABLE_6_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_6_NAME)
            .description(TABLE_6_DESCRIPTION)
            .columns(TABLE_6_COLUMNS_CREATE)
            .constraints(TABLE_6_CONSTRAINTS_CREATE)
            .build();

    public final static Long COLUMN_7_1_ID = 73L;

    public final static Long COLUMN_7_2_ID = 74L;

    public final static ColumnBriefDto TABLE_7_COLUMNS_BRIEF_0_DTO = ColumnBriefDto.builder()
            .id(COLUMN_7_1_ID)
            .name("name_id")
            .internalName("name_id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static ColumnBriefDto TABLE_7_COLUMNS_BRIEF_1_DTO = ColumnBriefDto.builder()
            .id(COLUMN_7_2_ID)
            .name("zoo_id")
            .internalName("zoo_id")
            .columnType(ColumnTypeDto.BIGINT)
            .build();

    public final static List<TableColumn> TABLE_7_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_7_1_ID)
                    .ordinalPosition(0)
                    .table(TABLE_7)
                    .name("name_id")
                    .internalName("name_id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_7_2_ID)
                    .ordinalPosition(1)
                    .table(TABLE_7)
                    .name("zoo_id")
                    .internalName("zoo_id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .build());

    public final static List<ColumnDto> TABLE_7_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_7_1_ID)
                    .ordinalPosition(0)
                    .tableId(TABLE_7_ID)
                    .name("name_id")
                    .internalName("name_id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_7_2_ID)
                    .ordinalPosition(1)
                    .tableId(TABLE_7_ID)
                    .name("zoo_id")
                    .internalName("zoo_id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .build());

    public final static Long VIEW_1_ID = 1L;
    public final static Boolean VIEW_1_INITIAL_VIEW = false;
    public final static String VIEW_1_NAME = "JUnit";
    public final static String VIEW_1_INTERNAL_NAME = "junit";
    public final static Long VIEW_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_1_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_1_PUBLIC = true;
    public final static Boolean VIEW_1_SCHEMA_PUBLIC = true;
    public final static String VIEW_1_QUERY = "select `location`, `lat`, `lng` from `weather_location`";
    public final static String VIEW_1_QUERY_HASH = "dc81a6877c7c51a6a6f406e1fc2a255e44a0d49a20548596e0d583c3eb849c23";

    public final static List<ViewColumnDto> VIEW_1_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(1L)
                    .ordinalPosition(0)
                    .databaseId(DATABASE_1_ID)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .build(),
            ViewColumnDto.builder()
                    .id(2L)
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
                    .id(3L)
                    .ordinalPosition(2)
                    .databaseId(DATABASE_1_ID)
                    .name("lng")
                    .internalName("lng")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build()
    );

    public final static View VIEW_1 = View.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .isSchemaPublic(VIEW_1_SCHEMA_PUBLIC)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .identifiers(new LinkedList<>()) /* IDENTIFIER_3 */
            .columns(null) /* VIEW_1_COLUMNS */
            .build();

    public final static Long VIEW_1_DATA_COUNT = 3L;
    public final static List<Map<String, Object>> VIEW_1_DATA_DTO = new LinkedList<>(List.of(
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

    public final static List<ViewColumn> VIEW_1_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(1L)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .view(VIEW_1)
                    .build(),
            ViewColumn.builder()
                    .id(2L)
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
                    .id(3L)
                    .ordinalPosition(2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_1)
                    .build()
    );

    public final static ViewDto VIEW_1_DTO = ViewDto.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .isSchemaPublic(VIEW_1_SCHEMA_PUBLIC)
            .identifiers(null /* VIEW_1_DTO_IDENTIFIERS */)
            .owner(USER_1_BRIEF_DTO)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .columns(VIEW_1_COLUMNS_DTO)
            .build();

    public final static PrivilegedViewDto VIEW_1_PRIVILEGED_DTO = PrivilegedViewDto.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .database(null) /* DATABASE_1_PRIVILEGED_DTO */
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .owner(USER_1_BRIEF_DTO)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .columns(VIEW_1_COLUMNS_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static ViewBriefDto VIEW_1_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .isSchemaPublic(VIEW_1_SCHEMA_PUBLIC)
            .ownedBy(USER_1_ID)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .build();

    public final static ViewCreateDto VIEW_1_CREATE_DTO = ViewCreateDto.builder()
            .isPublic(VIEW_1_PUBLIC)
            .name(VIEW_1_NAME)
            .query(VIEW_1_QUERY)
            .build();

    public final static Long VIEW_2_ID = 2L;
    public final static Boolean VIEW_2_INITIAL_VIEW = false;
    public final static String VIEW_2_NAME = "JUnit2";
    public final static String VIEW_2_INTERNAL_NAME = "junit2";
    public final static Long VIEW_2_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_2_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_2_PUBLIC = true;
    public final static Boolean VIEW_2_SCHEMA_PUBLIC = true;
    public final static String VIEW_2_QUERY = "select `date`, `location` as loc, `mintemp`, `rainfall` from `weather_aus` where `location` = 'Albury'";
    public final static String VIEW_2_QUERY_HASH = "987fc946772ffb6d85060262dcb5df419692a1f6772ea995e3dedb53c191e984";

    public final static List<ViewColumnDto> VIEW_2_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(4L)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .name("Date")
                    .internalName("date")
                    .columnType(ColumnTypeDto.DATE)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(5L)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .name("loc")
                    .internalName("loc")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(6L)
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
                    .id(7L)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .build()
    );

    public final static View VIEW_2 = View.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .columns(null)  /* VIEW_2_COLUMNS */
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .build();

    public final static List<ViewColumn> VIEW_2_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(4L)
                    .ordinalPosition(0)
                    .name("Date")
                    .internalName("date")
                    .columnType(TableColumnType.DATE)
                    .isNullAllowed(true)
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(5L)
                    .ordinalPosition(1)
                    .name("loc")
                    .internalName("loc")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(6L)
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
                    .id(7L)
                    .ordinalPosition(3)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_2)
                    .build()
    );

    public final static ViewDto VIEW_2_DTO = ViewDto.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .columns(VIEW_2_COLUMNS_DTO)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final static PrivilegedViewDto VIEW_2_PRIVILEGED_DTO = PrivilegedViewDto.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .database(null) /* DATABASE_1_PRIVILEGED_DTO */
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .owner(USER_2_BRIEF_DTO)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .columns(VIEW_2_COLUMNS_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static ViewBriefDto VIEW_2_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .isSchemaPublic(VIEW_2_SCHEMA_PUBLIC)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .build();

    public final static Long VIEW_3_ID = 3L;
    public final static Boolean VIEW_3_INITIAL_VIEW = false;
    public final static String VIEW_3_NAME = "JUnit3";
    public final static String VIEW_3_INTERNAL_NAME = "junit3";
    public final static Long VIEW_3_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_3_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_3_PUBLIC = false;
    public final static Boolean VIEW_3_SCHEMA_PUBLIC = false;
    public final static String VIEW_3_QUERY = "select w.`mintemp`, w.`rainfall`, w.`location`, m.`date` from `weather_aus` w join `junit2` m on m.`location` = w.`location` and m.`date` = w.`date`";
    public final static String VIEW_3_QUERY_HASH = "bbbaa56a5206b3dc3e6cf9301b0db9344eb6f19b100c7b88550ffb597a0bd255";

    public final static Long VIEW_3_DATA_COUNT = 3L;

    public final static List<ViewColumnDto> VIEW_3_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(8L)
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
                    .id(9L)
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
                    .id(10L)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .name("Location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(11L)
                    .databaseId(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .name("Date")
                    .internalName("date")
                    .columnType(ColumnTypeDto.DATE)
                    .isNullAllowed(true)
                    .build()
    );

    public final static View VIEW_3 = View.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .columns(null)  /* VIEW_3_COLUMNS */
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .build();

    public final static PrivilegedViewDto VIEW_3_PRIVILEGED_DTO = PrivilegedViewDto.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .database(null) /* DATABASE_1_PRIVILEGED_DTO */
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .owner(USER_1_BRIEF_DTO)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .columns(VIEW_3_COLUMNS_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static List<ViewColumn> VIEW_3_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(8L)
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
                    .id(9L)
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
                    .id(10L)
                    .ordinalPosition(2)
                    .name("Location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .view(VIEW_3)
                    .build(),
            ViewColumn.builder()
                    .id(11L)
                    .ordinalPosition(3)
                    .name("Date")
                    .internalName("date")
                    .columnType(TableColumnType.DATE)
                    .isNullAllowed(true)
                    .view(VIEW_3)
                    .build()
    );

    public final static ViewDto VIEW_3_DTO = ViewDto.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .columns(VIEW_3_COLUMNS_DTO)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final static ViewBriefDto VIEW_3_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .isSchemaPublic(VIEW_3_SCHEMA_PUBLIC)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .build();

    public final static Long VIEW_4_ID = 4L;
    public final static Boolean VIEW_4_INITIAL_VIEW = false;
    public final static String VIEW_4_NAME = "Mock View";
    public final static String VIEW_4_INTERNAL_NAME = "mock_view";
    public final static Long VIEW_4_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long VIEW_4_DATABASE_ID = DATABASE_2_ID;
    public final static Long VIEW_4_TABLE_ID = TABLE_5_ID;
    public final static Table VIEW_4_TABLE = TABLE_5;
    public final static Boolean VIEW_4_PUBLIC = true;
    public final static Boolean VIEW_4_SCHEMA_PUBLIC = true;
    public final static String VIEW_4_QUERY = "SELECT `animal_name`, `hair`, `feathers`, `eggs`, `milk`, `airborne`, `aquatic`, `predator`, `backbone`, `breathes`, `venomous`, `fins`, `legs`, `tail`, `domestic`, `catsize`, `class_type` FROM `zoo` WHERE `class_type` = 1";
    public final static String VIEW_4_QUERY_HASH = "3561cd0bb0b0e94d6f15ae602134252a5760d09d660a71a4fb015b6991c8ba0b";

    public final static List<ViewColumnDto> VIEW_4_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(12L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(0)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(13L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(1)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(14L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(2)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(15L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(3)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(16L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(4)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(17L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(5)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(18L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(6)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(19L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(7)
                    .name("Predator")
                    .internalName("predator")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(20L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(8)
                    .name("Backbone")
                    .internalName("backbone")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(21L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(9)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(22L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(10)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(23L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(11)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(24L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(12)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(ColumnTypeDto.INT)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(25L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(13)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(26L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(14)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(27L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(15)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(ColumnTypeDto.BOOL)
                    .isNullAllowed(true)
                    .build(),
            ViewColumnDto.builder()
                    .id(28L)
                    .databaseId(DATABASE_2_ID)
                    .ordinalPosition(16)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .isNullAllowed(true)
                    .build());

    public final static View VIEW_4 = View.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vdbid(VIEW_4_DATABASE_ID)
            .isPublic(VIEW_4_PUBLIC)
            .isSchemaPublic(VIEW_4_SCHEMA_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .columns(null) /* VIEW_4_COLUMNS */
            .build();

    public final static ViewDto VIEW_4_DTO = ViewDto.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vdbid(VIEW_4_DATABASE_ID)
            .isPublic(VIEW_4_PUBLIC)
            .isSchemaPublic(VIEW_4_SCHEMA_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .columns(VIEW_4_COLUMNS_DTO)
            .build();

    public final static ViewBriefDto VIEW_4_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vdbid(VIEW_4_DATABASE_ID)
            .isPublic(VIEW_4_PUBLIC)
            .isSchemaPublic(VIEW_4_SCHEMA_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .build();

    public final static List<ViewColumn> VIEW_4_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(12L)
                    .ordinalPosition(0)
                    .name("Animal Name")
                    .internalName("animal_name")
                    .columnType(TableColumnType.VARCHAR)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(13L)
                    .ordinalPosition(1)
                    .name("Hair")
                    .internalName("hair")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(14L)
                    .ordinalPosition(2)
                    .name("Feathers")
                    .internalName("feathers")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(15L)
                    .ordinalPosition(3)
                    .name("Eggs")
                    .internalName("eggs")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(16L)
                    .ordinalPosition(4)
                    .name("Milk")
                    .internalName("milk")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(17L)
                    .ordinalPosition(5)
                    .name("Airborne")
                    .internalName("airborne")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(18L)
                    .ordinalPosition(6)
                    .name("Aquantic")
                    .internalName("aquantic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(19L)
                    .ordinalPosition(7)
                    .name("Predator")
                    .internalName("predator")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(20L)
                    .ordinalPosition(8)
                    .name("Backbone")
                    .internalName("backbone")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(21L)
                    .ordinalPosition(9)
                    .name("Breathes")
                    .internalName("breathes")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(22L)
                    .ordinalPosition(10)
                    .name("Venomous")
                    .internalName("venomous")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(23L)
                    .ordinalPosition(11)
                    .name("Fin")
                    .internalName("fin")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(24L)
                    .ordinalPosition(12)
                    .name("Legs")
                    .internalName("legs")
                    .columnType(TableColumnType.INT)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(25L)
                    .ordinalPosition(13)
                    .name("Tail")
                    .internalName("tail")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(26L)
                    .ordinalPosition(14)
                    .name("Domestic")
                    .internalName("domestic")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(27L)
                    .ordinalPosition(15)
                    .name("Catsize")
                    .internalName("catsize")
                    .columnType(TableColumnType.BOOL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(28L)
                    .ordinalPosition(16)
                    .name("Class Type")
                    .internalName("class_type")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .view(VIEW_4)
                    .build());

    public final static Long VIEW_5_ID = 5L;
    public final static Boolean VIEW_5_INITIAL_VIEW = false;
    public final static String VIEW_5_NAME = "Mock View";
    public final static String VIEW_5_INTERNAL_NAME = "mock_view";
    public final static Long VIEW_5_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long VIEW_5_DATABASE_ID = DATABASE_3_ID;
    public final static Boolean VIEW_5_PUBLIC = true;
    public final static Boolean VIEW_5_SCHEMA_PUBLIC = true;
    public final static String VIEW_5_QUERY = "SELECT `location`, `lat`, `lng` FROM `weather_location` WHERE `location` = 'Albury'";
    public final static String VIEW_5_QUERY_HASH = "120f32478aaff874c25ab32eceb9f00b64cc9d422831046f2f5d43953aca01e7";

    public final static View VIEW_5 = View.builder()
            .id(VIEW_5_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .vdbid(VIEW_5_DATABASE_ID)
            .isPublic(VIEW_5_PUBLIC)
            .isSchemaPublic(VIEW_5_SCHEMA_PUBLIC)
            .query(VIEW_5_QUERY)
            .queryHash(VIEW_5_QUERY_HASH)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .columns(null)
            .build();

    public final static ViewDto VIEW_5_DTO = ViewDto.builder()
            .id(VIEW_5_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .vdbid(VIEW_5_DATABASE_ID)
            .isPublic(VIEW_5_PUBLIC)
            .isSchemaPublic(VIEW_5_SCHEMA_PUBLIC)
            .query(VIEW_5_QUERY)
            .queryHash(VIEW_5_QUERY_HASH)
            .owner(USER_1_BRIEF_DTO)
            .columns(new LinkedList<>())
            .build();

    public final static ViewBriefDto VIEW_5_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_5_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .vdbid(VIEW_5_DATABASE_ID)
            .isPublic(VIEW_5_PUBLIC)
            .isSchemaPublic(VIEW_5_SCHEMA_PUBLIC)
            .query(VIEW_5_QUERY)
            .queryHash(VIEW_5_QUERY_HASH)
            .build();

    public final static List<ViewColumn> VIEW_5_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(29L)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .view(VIEW_5)
                    .build(),
            ViewColumn.builder()
                    .id(30L)
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
                    .id(31L)
                    .ordinalPosition(2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .view(VIEW_5)
                    .build());

    public final static List<ViewColumnDto> VIEW_5_COLUMNS_DTO = List.of(
            ViewColumnDto.builder()
                    .id(29L)
                    .databaseId(DATABASE_3_ID)
                    .ordinalPosition(0)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .build(),
            ViewColumnDto.builder()
                    .id(30L)
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
                    .id(31L)
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
    public final static List<Map<String, Object>> QUERY_1_RESULT_DTO = new LinkedList<>(List.of(
            new HashMap<>() {{
                put("location", "Albury");
                put("lat", -36.0653583);
                put("lng", 146.9112214);
            }}, new HashMap<>() {{
                put("location", "Sydney");
                put("lat", -33.847927);
                put("lng", 150.6517942);
            }}));

    public final static String LICENSE_1_IDENTIFIER = "MIT";
    public final static String LICENSE_1_URI = "https://opensource.org/license/mit/";

    public final static License LICENSE_1 = License.builder()
            .identifier(LICENSE_1_IDENTIFIER)
            .uri(LICENSE_1_URI)
            .build();

    public final static LicenseDto LICENSE_1_DTO = LicenseDto.builder()
            .identifier(LICENSE_1_IDENTIFIER)
            .uri(LICENSE_1_URI)
            .build();

    public final static Long CREATOR_1_ID = 1L;
    public final static Long CREATOR_1_QUERY_ID = 1L;
    public final static String CREATOR_1_ORCID = "00000-00000-00000";
    public final static String CREATOR_1_AFFIL = "TU Graz";
    public final static String CREATOR_1_AFFIL_ROR = "https://ror.org/04wn28048";
    public final static String CREATOR_1_AFFIL_URI = "https://ror.org/";
    public final static AffiliationIdentifierSchemeType CREATOR_1_AFFIL_TYPE = AffiliationIdentifierSchemeType.ROR;
    public final static AffiliationIdentifierSchemeTypeDto CREATOR_1_AFFIL_TYPE_DTO = AffiliationIdentifierSchemeTypeDto.ROR;
    public final static String CREATOR_1_FIRSTNAME = "Max";
    public final static String CREATOR_1_LASTNAME = "Mustermann";
    public final static String CREATOR_1_NAME = CREATOR_1_LASTNAME + ", " + CREATOR_1_FIRSTNAME;
    public final static Instant CREATOR_1_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant CREATOR_1_MODIFIED = Instant.ofEpochSecond(1541588352L);

    public final static OrcidDto ORCID_1_DTO = OrcidDto.builder()
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

    public final static Long CREATOR_2_ID = 2L;
    public final static Long CREATOR_2_QUERY_ID = 1L;
    public final static String CREATOR_2_ORCID = "00000-00000-00000";
    public final static String CREATOR_2_AFFIL = "TU Wien";
    public final static String CREATOR_2_FIRSTNAME = "Martina";
    public final static String CREATOR_2_LASTNAME = "Mustermann";
    public final static String CREATOR_2_NAME = CREATOR_2_LASTNAME + ", " + CREATOR_2_FIRSTNAME;
    public final static Instant CREATOR_2_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant CREATOR_2_MODIFIED = Instant.ofEpochSecond(1541588352L);

    public final static Long CREATOR_3_ID = 3L;
    public final static Long CREATOR_3_QUERY_ID = 1L;
    public final static String CREATOR_3_ORCID = "00000-00000-00000";
    public final static String CREATOR_3_AFFIL = "TU Graz";
    public final static String CREATOR_3_AFFIL_ROR = "https://ror.org/04wn28048";
    public final static AffiliationIdentifierSchemeType CREATOR_3_AFFIL_SCHEME_TYPE = AffiliationIdentifierSchemeType.ROR;
    public final static AffiliationIdentifierSchemeTypeDto CREATOR_3_AFFIL_SCHEME_TYPE_DTO = AffiliationIdentifierSchemeTypeDto.ROR;
    public final static String CREATOR_3_AFFIL_URI = "https://ror.org/";
    public final static String CREATOR_3_FIRSTNAME = "Max";
    public final static String CREATOR_3_LASTNAME = "Mustermann";
    public final static String CREATOR_3_NAME = CREATOR_3_LASTNAME + ", " + CREATOR_3_FIRSTNAME;
    public final static Instant CREATOR_3_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant CREATOR_3_MODIFIED = Instant.ofEpochSecond(1541588352L);

    public final static Long CREATOR_4_ID = 4L;
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
    public final static Instant CREATOR_4_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant CREATOR_4_MODIFIED = Instant.ofEpochSecond(1541588352L);

    public final static Long IDENTIFIER_1_ID = 1L;
    public final static Long IDENTIFIER_1_QUERY_ID = null;
    public final static Long IDENTIFIER_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long IDENTIFIER_1_DATABASE_ID = DATABASE_1_ID;
    public final static String IDENTIFIER_1_DOI = "10.12345/183";
    public final static Instant IDENTIFIER_1_CREATED = Instant.ofEpochSecond(1641588352L) /* 2022-01-07 20:45:52 */;
    public final static Instant IDENTIFIER_1_MODIFIED = Instant.ofEpochSecond(1541588352L) /* 2022-01-07 20:45:52 */;
    public final static Instant IDENTIFIER_1_EXECUTION = Instant.ofEpochSecond(1541588352L) /* 2022-01-07 20:45:52 */;
    public final static Integer IDENTIFIER_1_PUBLICATION_MONTH = 5;
    public final static Integer IDENTIFIER_1_PUBLICATION_YEAR = 2022;
    public final static Integer IDENTIFIER_1_PUBLICATION_DAY = null;
    public final static String IDENTIFIER_1_QUERY_HASH = QUERY_1_QUERY_HASH;
    public final static String IDENTIFIER_1_RESULT_HASH = QUERY_1_RESULT_HASH;
    public final static String IDENTIFIER_1_QUERY = QUERY_1_STATEMENT;
    public final static String IDENTIFIER_1_NORMALIZED = QUERY_1_STATEMENT;
    public final static Long IDENTIFIER_1_RESULT_NUMBER = QUERY_1_RESULT_NUMBER;
    public final static String IDENTIFIER_1_PUBLISHER = "Austrian Government";
    public final static IdentifierType IDENTIFIER_1_TYPE = IdentifierType.DATABASE;
    public final static IdentifierTypeDto IDENTIFIER_1_TYPE_DTO = IdentifierTypeDto.DATABASE;
    public final static IdentifierStatusType IDENTIFIER_1_STATUS_TYPE = IdentifierStatusType.PUBLISHED;
    public final static IdentifierStatusTypeDto IDENTIFIER_1_STATUS_TYPE_DTO = IdentifierStatusTypeDto.PUBLISHED;

    public final static Long IDENTIFIER_1_TITLE_1_ID = 1L;
    public final static Long IDENTIFIER_1_TITLE_1_IDENTIFIER_ID = IDENTIFIER_1_ID;
    public final static String IDENTIFIER_1_TITLE_1_TITLE = "Austrian weather data";
    public final static String IDENTIFIER_1_TITLE_1_TITLE_MODIFY = "Austrian weather some data";
    public final static TitleType IDENTIFIER_1_TITLE_1_TYPE = null;
    public final static TitleTypeDto IDENTIFIER_1_TITLE_1_TYPE_DTO = null;
    public final static LanguageType IDENTIFIER_1_TITLE_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_1_TITLE_1_LANG_DTO = LanguageTypeDto.EN;

    public final static IdentifierTitle IDENTIFIER_1_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_1_TITLE_1_ID)
            .title(IDENTIFIER_1_TITLE_1_TITLE)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE)
            .language(IDENTIFIER_1_TITLE_1_LANG)
            .build();

    public final static IdentifierTitleDto IDENTIFIER_1_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_1_ID)
            .title(IDENTIFIER_1_TITLE_1_TITLE)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final static IdentifierTitleDto IDENTIFIER_1_TITLE_1_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_1_ID)
            .title(IDENTIFIER_1_TITLE_1_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final static IdentifierSaveTitleDto IDENTIFIER_1_TITLE_1_CREATE_DTO = IdentifierSaveTitleDto.builder()
            .title(IDENTIFIER_1_TITLE_1_TITLE)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final static IdentifierSaveTitleDto IDENTIFIER_1_TITLE_1_UPDATE_DTO = IdentifierSaveTitleDto.builder()
            .title(IDENTIFIER_1_TITLE_1_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_1_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_1_LANG_DTO)
            .build();

    public final static Long IDENTIFIER_1_TITLE_2_ID = 2L;
    public final static Long IDENTIFIER_1_TITLE_2_IDENTIFIER_ID = IDENTIFIER_1_ID;
    public final static String IDENTIFIER_1_TITLE_2_TITLE = "Österreichische Wetterdaten";
    public final static String IDENTIFIER_1_TITLE_2_TITLE_MODIFY = "Österreichische Wetterdaten übersetzt";
    public final static TitleType IDENTIFIER_1_TITLE_2_TYPE = TitleType.TRANSLATED_TITLE;
    public final static TitleTypeDto IDENTIFIER_1_TITLE_2_TYPE_DTO = TitleTypeDto.TRANSLATED_TITLE;
    public final static LanguageType IDENTIFIER_1_TITLE_2_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_1_TITLE_2_LANG_DTO = LanguageTypeDto.EN;

    public final static IdentifierTitle IDENTIFIER_1_TITLE_2 = IdentifierTitle.builder()
            .id(IDENTIFIER_1_TITLE_2_ID)
            .title(IDENTIFIER_1_TITLE_2_TITLE)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE)
            .language(IDENTIFIER_1_TITLE_2_LANG)
            .build();

    public final static IdentifierTitleDto IDENTIFIER_1_TITLE_2_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_2_ID)
            .title(IDENTIFIER_1_TITLE_2_TITLE)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final static IdentifierTitleDto IDENTIFIER_1_TITLE_2_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_1_TITLE_2_ID)
            .title(IDENTIFIER_1_TITLE_2_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final static IdentifierSaveTitleDto IDENTIFIER_1_TITLE_2_CREATE_DTO = IdentifierSaveTitleDto.builder()
            .title(IDENTIFIER_1_TITLE_2_TITLE)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final static IdentifierSaveTitleDto IDENTIFIER_1_TITLE_2_UPDATE_DTO = IdentifierSaveTitleDto.builder()
            .title(IDENTIFIER_1_TITLE_2_TITLE_MODIFY)
            .titleType(IDENTIFIER_1_TITLE_2_TYPE_DTO)
            .language(IDENTIFIER_1_TITLE_2_LANG_DTO)
            .build();

    public final static Long IDENTIFIER_1_DESCRIPTION_1_ID = 1L;
    public final static Long IDENTIFIER_1_DESCRIPTION_1_IDENTIFIER_ID = IDENTIFIER_1_ID;
    public final static String IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION = "Selecting all from the weather Austrian table";
    public final static String IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION_MODIFY = "Selecting some from the weather Austrian table";
    public final static DescriptionType IDENTIFIER_1_DESCRIPTION_1_TYPE = null;
    public final static DescriptionTypeDto IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO = null;
    public final static LanguageType IDENTIFIER_1_DESCRIPTION_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_1_DESCRIPTION_1_LANG_DTO = LanguageTypeDto.EN;

    public final static IdentifierDescription IDENTIFIER_1_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_1_DESCRIPTION_1_ID)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG)
            .build();

    public final static IdentifierDescriptionDto IDENTIFIER_1_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_1_DESCRIPTION_1_ID)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static IdentifierDescriptionDto IDENTIFIER_1_DESCRIPTION_1_DTO_MODIFY = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_1_DESCRIPTION_1_ID)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION_MODIFY)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static IdentifierSaveDescriptionDto IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO = IdentifierSaveDescriptionDto.builder()
            .id(null)
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static Long IDENTIFIER_1_CREATOR_1_ID = 1L;
    public final static String IDENTIFIER_1_CREATOR_1_FIRSTNAME = CREATOR_1_FIRSTNAME;
    public final static String IDENTIFIER_1_CREATOR_1_LASTNAME = CREATOR_1_LASTNAME;
    public final static String IDENTIFIER_1_CREATOR_1_NAME = CREATOR_1_NAME;
    public final static String IDENTIFIER_1_CREATOR_1_ORCID = CREATOR_1_ORCID;
    public final static NameIdentifierSchemeType IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE = NameIdentifierSchemeType.ORCID;
    public final static NameIdentifierSchemeTypeDto IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO = NameIdentifierSchemeTypeDto.ORCID;
    public final static String IDENTIFIER_1_CREATOR_1_AFFILIATION = CREATOR_1_AFFIL;
    public final static String IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER = CREATOR_1_AFFIL_ROR;
    public final static AffiliationIdentifierSchemeType IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME = CREATOR_1_AFFIL_TYPE;
    public final static AffiliationIdentifierSchemeTypeDto IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_DTO = CREATOR_1_AFFIL_TYPE_DTO;
    public final static String IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI = CREATOR_1_AFFIL_URI;

    public final static Creator IDENTIFIER_1_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_1_CREATOR_1_ID)
            .firstname(IDENTIFIER_1_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_1_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_1_CREATOR_1_NAME)
            .nameType(NameType.PERSONAL)
            .nameIdentifier(IDENTIFIER_1_CREATOR_1_ORCID)
            .nameIdentifierScheme(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .affiliationIdentifier(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER)
            .affiliationIdentifierScheme(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME)
            .affiliationIdentifierSchemeUri(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI)
            .build();

    public final static CreatorDto IDENTIFIER_1_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_1_CREATOR_1_ID)
            .firstname(IDENTIFIER_1_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_1_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_1_CREATOR_1_NAME)
            .nameType(NameTypeDto.PERSONAL)
            .nameIdentifier(IDENTIFIER_1_CREATOR_1_ORCID)
            .nameIdentifierScheme(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .affiliationIdentifier(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER)
            .affiliationIdentifierScheme(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_DTO)
            .affiliationIdentifierSchemeUri(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI)
            .build();

    public final static CreatorSaveDto IDENTIFIER_1_CREATOR_1_CREATE_DTO = CreatorSaveDto.builder()
            .id(null)
            .firstname(IDENTIFIER_1_CREATOR_1_FIRSTNAME)
            .lastname(IDENTIFIER_1_CREATOR_1_LASTNAME)
            .creatorName(IDENTIFIER_1_CREATOR_1_NAME)
            .nameType(NameTypeDto.PERSONAL)
            .nameIdentifier(IDENTIFIER_1_CREATOR_1_ORCID)
            .nameIdentifierScheme(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE_DTO)
            .affiliation(IDENTIFIER_1_CREATOR_1_AFFILIATION)
            .affiliationIdentifier(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER)
            .affiliationIdentifierScheme(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_DTO)
            .build();

    public final static Long FUNDER_1_ID = 1L;
    public final static String FUNDER_1_NAME = "European Commission";
    public final static String FUNDER_1_IDENTIFIER = "https://doi.org/10.13039/501100000780";
    public final static String FUNDER_1_IDENTIFIER_ID_ONLY = "10.13039/501100000780";
    public final static IdentifierFunderType FUNDER_1_IDENTIFIER_TYPE = IdentifierFunderType.CROSSREF_FUNDER_ID;
    public final static IdentifierFunderTypeDto FUNDER_1_IDENTIFIER_TYPE_DTO = IdentifierFunderTypeDto.CROSSREF_FUNDER_ID;
    public final static String FUNDER_1_AWARD_TITLE = "Institutionalizing global genetic-resource commons. Global Strategies for accessing and using essential public knowledge assets in the life science";

    public final static IdentifierFunder IDENTIFIER_1_FUNDER_1 = IdentifierFunder.builder()
            .id(FUNDER_1_ID)
            .funderName(FUNDER_1_NAME)
            .funderIdentifier(FUNDER_1_IDENTIFIER)
            .funderIdentifierType(FUNDER_1_IDENTIFIER_TYPE)
            .awardTitle(FUNDER_1_AWARD_TITLE)
            .build();

    public final static IdentifierFunderDto IDENTIFIER_1_FUNDER_1_DTO = IdentifierFunderDto.builder()
            .id(FUNDER_1_ID)
            .funderName(FUNDER_1_NAME)
            .funderIdentifier(FUNDER_1_IDENTIFIER)
            .funderIdentifierType(FUNDER_1_IDENTIFIER_TYPE_DTO)
            .awardTitle(FUNDER_1_AWARD_TITLE)
            .build();

    public final static IdentifierFunderSaveDto IDENTIFIER_1_FUNDER_1_CREATE_DTO = IdentifierFunderSaveDto.builder()
            .funderName(FUNDER_1_NAME)
            .funderIdentifier(FUNDER_1_IDENTIFIER)
            .funderIdentifierType(FUNDER_1_IDENTIFIER_TYPE_DTO)
            .awardTitle(FUNDER_1_AWARD_TITLE)
            .build();

    public final static DataCiteBody<DataCiteDoi> IDENTIFIER_1_DATA_CITE = DataCiteBody.<DataCiteDoi>builder()
            .data(DataCiteData.<DataCiteDoi>builder()
                    .type("dois")
                    .attributes(DataCiteDoi.builder()
                            .doi(IDENTIFIER_1_DOI)
                            .build())
                    .build())
            .build();

    public final static Identifier IDENTIFIER_1 = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1, IDENTIFIER_1_TITLE_2)))
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1)))
            .doi(IDENTIFIER_1_DOI)
            .database(null /* DATABASE_1 */)
            .created(IDENTIFIER_1_CREATED)
            .lastModified(IDENTIFIER_1_MODIFIED)
            .execution(IDENTIFIER_1_EXECUTION)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .queryHash(IDENTIFIER_1_QUERY_HASH)
            .resultHash(IDENTIFIER_1_RESULT_HASH)
            .query(IDENTIFIER_1_QUERY)
            .queryNormalized(IDENTIFIER_1_NORMALIZED)
            .resultNumber(IDENTIFIER_1_RESULT_NUMBER)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE)
            .owner(USER_1)
            .ownedBy(USER_1_ID)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1)))
            .status(IDENTIFIER_1_STATUS_TYPE)
            .build();

    public final static Identifier IDENTIFIER_1_WITH_DOI = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1, IDENTIFIER_1_TITLE_2)))
            .doi(IDENTIFIER_1_DOI)
            .database(null /* for jpa */)
            .created(IDENTIFIER_1_CREATED)
            .lastModified(IDENTIFIER_1_MODIFIED)
            .execution(IDENTIFIER_1_EXECUTION)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .queryHash(IDENTIFIER_1_QUERY_HASH)
            .resultHash(IDENTIFIER_1_RESULT_HASH)
            .query(IDENTIFIER_1_QUERY)
            .queryNormalized(IDENTIFIER_1_NORMALIZED)
            .resultNumber(IDENTIFIER_1_RESULT_NUMBER)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE)
            .owner(USER_1)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1)))
            .status(IDENTIFIER_1_STATUS_TYPE)
            .build();

    public final static IdentifierDto IDENTIFIER_1_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_DTO, IDENTIFIER_1_TITLE_2_DTO)))
            .doi(IDENTIFIER_1_DOI)
            .execution(IDENTIFIER_1_EXECUTION)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .queryHash(IDENTIFIER_1_QUERY_HASH)
            .resultHash(IDENTIFIER_1_RESULT_HASH)
            .query(IDENTIFIER_1_QUERY)
            .queryNormalized(IDENTIFIER_1_NORMALIZED)
            .resultNumber(IDENTIFIER_1_RESULT_NUMBER)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .owner(USER_1_BRIEF_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_DTO)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_DTO)))
            .status(IDENTIFIER_1_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierBriefDto IDENTIFIER_1_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_DTO, IDENTIFIER_1_TITLE_2_DTO)))
            .doi(IDENTIFIER_1_DOI)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .status(IDENTIFIER_1_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_1_CREATE_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_1_DATABASE_ID)
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
            .build();

    public final static IdentifierCreateDto IDENTIFIER_1_CREATE_WITH_DOI_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_1_DATABASE_ID)
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
            .build();

    public final static IdentifierSaveDto IDENTIFIER_1_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO, IDENTIFIER_1_TITLE_2_CREATE_DTO)))
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO)))
            .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_CREATE_DTO)))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .build();

    public final static IdentifierSaveDto IDENTIFIER_1_SAVE_MODIFY_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .descriptions(new LinkedList<>(List.of())) // <<<
            .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))) // <<<
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of())) // <<<
            .funders(new LinkedList<>(List.of())) // <<<
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .licenses(new LinkedList<>(List.of())) // <<<
            .build();

    public final static Long IDENTIFIER_5_ID = 5L;
    public final static Long IDENTIFIER_5_QUERY_ID = QUERY_2_ID;
    public final static Long IDENTIFIER_5_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long IDENTIFIER_5_DATABASE_ID = DATABASE_2_ID;
    public final static String IDENTIFIER_5_DOI = "10.12345/13/50BBFCFE08A12";
    public final static Instant IDENTIFIER_5_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant IDENTIFIER_5_MODIFIED = Instant.ofEpochSecond(1541588352L);
    public final static Instant IDENTIFIER_5_EXECUTION = Instant.ofEpochSecond(1541588352L);
    public final static Integer IDENTIFIER_5_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_5_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_5_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_5_QUERY_HASH = QUERY_2_QUERY_HASH;
    public final static String IDENTIFIER_5_RESULT_HASH = QUERY_2_RESULT_HASH;
    public final static String IDENTIFIER_5_QUERY = QUERY_2_STATEMENT;
    public final static String IDENTIFIER_5_NORMALIZED = QUERY_2_STATEMENT;
    public final static Long IDENTIFIER_5_RESULT_NUMBER = QUERY_2_RESULT_NUMBER;
    public final static String IDENTIFIER_5_PUBLISHER = "Australian Government";
    public final static IdentifierType IDENTIFIER_5_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_5_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static IdentifierStatusType IDENTIFIER_5_STATUS_TYPE = IdentifierStatusType.DRAFT;
    public final static IdentifierStatusTypeDto IDENTIFIER_5_STATUS_TYPE_DTO = IdentifierStatusTypeDto.DRAFT;
    public final static UUID IDENTIFIER_5_CREATED_BY = USER_2_ID;

    public final static Long IDENTIFIER_5_TITLE_1_ID = 3L;
    public final static Long IDENTIFIER_5_TITLE_1_IDENTIFIER_ID = IDENTIFIER_5_ID;
    public final static String IDENTIFIER_5_TITLE_1_TITLE = "Australische Wetterdaten";
    public final static LanguageType IDENTIFIER_5_TITLE_1_LANG = LanguageType.DE;
    public final static LanguageTypeDto IDENTIFIER_5_TITLE_1_LANG_DTO = LanguageTypeDto.DE;
    public final static TitleType IDENTIFIER_5_TITLE_1_TYPE = TitleType.SUBTITLE;
    public final static TitleTypeDto IDENTIFIER_5_TITLE_1_TYPE_DTO = TitleTypeDto.SUBTITLE;

    public final static IdentifierTitle IDENTIFIER_5_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_5_TITLE_1_ID)
            .title(IDENTIFIER_5_TITLE_1_TITLE)
            .language(IDENTIFIER_5_TITLE_1_LANG)
            .titleType(IDENTIFIER_5_TITLE_1_TYPE)
            .build();

    public final static IdentifierTitleDto IDENTIFIER_5_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_5_TITLE_1_ID)
            .title(IDENTIFIER_5_TITLE_1_TITLE)
            .language(IDENTIFIER_5_TITLE_1_LANG_DTO)
            .titleType(IDENTIFIER_5_TITLE_1_TYPE_DTO)
            .build();

    public final static IdentifierSaveTitleDto IDENTIFIER_5_TITLE_1_CREATE_DTO = IdentifierSaveTitleDto.builder()
            .title(IDENTIFIER_5_TITLE_1_TITLE)
            .language(IDENTIFIER_5_TITLE_1_LANG_DTO)
            .titleType(IDENTIFIER_5_TITLE_1_TYPE_DTO)
            .build();

    public final static Long IDENTIFIER_5_DESCRIPTION_1_ID = 2L;
    public final static Long IDENTIFIER_5_DESCRIPTION_1_IDENTIFIER_ID = IDENTIFIER_5_ID;
    public final static String IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION = "Alle Wetterdaten in Australien";
    public final static LanguageType IDENTIFIER_5_DESCRIPTION_1_LANG = LanguageType.DE;
    public final static LanguageTypeDto IDENTIFIER_5_DESCRIPTION_1_LANG_DTO = LanguageTypeDto.DE;
    public final static DescriptionType IDENTIFIER_5_DESCRIPTION_1_TYPE = DescriptionType.ABSTRACT;
    public final static DescriptionTypeDto IDENTIFIER_5_DESCRIPTION_1_TYPE_DTO = DescriptionTypeDto.ABSTRACT;

    public final static IdentifierDescription IDENTIFIER_5_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_5_DESCRIPTION_1_ID)
            .description(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_5_DESCRIPTION_1_LANG)
            .descriptionType(IDENTIFIER_5_DESCRIPTION_1_TYPE)
            .build();

    public final static IdentifierDescriptionDto IDENTIFIER_5_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_5_DESCRIPTION_1_ID)
            .description(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_5_DESCRIPTION_1_LANG_DTO)
            .descriptionType(IDENTIFIER_5_DESCRIPTION_1_TYPE_DTO)
            .build();

    public final static IdentifierSaveDescriptionDto IDENTIFIER_5_DESCRIPTION_1_CREATE_DTO = IdentifierSaveDescriptionDto.builder()
            .id(null)
            .description(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_5_DESCRIPTION_1_LANG_DTO)
            .descriptionType(IDENTIFIER_5_DESCRIPTION_1_TYPE_DTO)
            .build();

    public final static Long IDENTIFIER_5_CREATOR_1_ID = 2L;

    public final static Creator IDENTIFIER_5_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_5_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeType.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final static CreatorDto IDENTIFIER_5_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_5_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final static CreatorSaveDto IDENTIFIER_5_CREATOR_1_CREATE_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static CreatorSaveDto IDENTIFIER_5_CREATOR_1_MODIFY_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(null) /* <<<< */
            .nameIdentifierScheme(null) /* <<<< */
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static Long IDENTIFIER_5_CREATOR_2_ID = 3L;

    public final static Creator IDENTIFIER_5_CREATOR_2 = Creator.builder()
            .id(IDENTIFIER_5_CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .creatorName(CREATOR_2_NAME)
            .nameIdentifier(CREATOR_2_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeType.ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static CreatorDto IDENTIFIER_5_CREATOR_2_DTO = CreatorDto.builder()
            .id(IDENTIFIER_5_CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .creatorName(CREATOR_2_NAME)
            .nameIdentifier(CREATOR_2_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static CreatorSaveDto IDENTIFIER_5_CREATOR_2_CREATE_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .creatorName(CREATOR_2_NAME)
            .nameIdentifier(CREATOR_2_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static CreatorSaveDto IDENTIFIER_5_CREATOR_2_MODIFY_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .creatorName(CREATOR_2_NAME)
            .nameIdentifier(null) /* <<<< */
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static Identifier IDENTIFIER_5 = Identifier.builder()
            .id(IDENTIFIER_5_ID)
            .queryId(IDENTIFIER_5_QUERY_ID)
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
            .owner(USER_2)
            .ownedBy(USER_2_ID)
            .creators(new LinkedList<>(List.of(IDENTIFIER_5_CREATOR_1, IDENTIFIER_5_CREATOR_2)))
            .status(IDENTIFIER_5_STATUS_TYPE)
            .build();

    public final static IdentifierDto IDENTIFIER_5_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_5_ID)
            .databaseId(DATABASE_2_ID)
            .queryId(IDENTIFIER_5_QUERY_ID)
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
            .creators(new LinkedList<>(List.of(IDENTIFIER_5_CREATOR_1_DTO, IDENTIFIER_5_CREATOR_2_DTO)))
            .build();

    public final static IdentifierBriefDto IDENTIFIER_5_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_5_ID)
            .databaseId(DATABASE_2_ID)
            .queryId(IDENTIFIER_5_QUERY_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_5_TITLE_1_DTO)))
            .doi(IDENTIFIER_5_DOI)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_5_PUBLISHER)
            .type(IDENTIFIER_5_TYPE_DTO)
            .build();

    public final static Long RELATED_IDENTIFIER_5_ID = 1L;
    public final static Long RELATED_IDENTIFIER_5_IDENTIFIER_ID = 2L;
    public final static String RELATED_IDENTIFIER_5_VALUE = "10.5281/zenodo.6637333";
    public final static RelatedType RELATED_IDENTIFIER_5_TYPE = RelatedType.DOI;
    public final static RelatedTypeDto RELATED_IDENTIFIER_5_TYPE_DTO = RelatedTypeDto.DOI;
    public final static RelationType RELATED_IDENTIFIER_5_RELATION_TYPE = RelationType.CITES;
    public final static RelationTypeDto RELATED_IDENTIFIER_5_RELATION_TYPE_DTO = RelationTypeDto.CITES;

    public final static RelatedIdentifier IDENTIFIER_1_RELATED_IDENTIFIER_1 = RelatedIdentifier.builder()
            .id(RELATED_IDENTIFIER_5_ID)
            .identifier(IDENTIFIER_5)
            .type(RELATED_IDENTIFIER_5_TYPE)
            .relation(RELATED_IDENTIFIER_5_RELATION_TYPE)
            .value(RELATED_IDENTIFIER_5_VALUE)
            .build();

    public final static RelatedIdentifierSaveDto IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO = RelatedIdentifierSaveDto.builder()
            .value(RELATED_IDENTIFIER_5_VALUE)
            .type(RELATED_IDENTIFIER_5_TYPE_DTO)
            .relation(RELATED_IDENTIFIER_5_RELATION_TYPE_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_5_CREATE_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_5_DATABASE_ID)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_5_PUBLISHER)
            .build();

    public final static IdentifierSaveDto IDENTIFIER_5_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_5_ID)
            .queryId(IDENTIFIER_5_QUERY_ID)
            .databaseId(IDENTIFIER_5_DATABASE_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_5_DESCRIPTION_1_CREATE_DTO)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_5_TITLE_1_CREATE_DTO)))
            .relatedIdentifiers(new LinkedList<>(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO)))
            .publicationDay(IDENTIFIER_5_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_5_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_5_CREATOR_1_CREATE_DTO, IDENTIFIER_5_CREATOR_2_CREATE_DTO)))
            .publisher(IDENTIFIER_5_PUBLISHER)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .type(IDENTIFIER_5_TYPE_DTO)
            .build();

    public final static Long IDENTIFIER_6_ID = 6L;
    public final static Long IDENTIFIER_6_QUERY_ID = QUERY_3_ID;
    public final static Long IDENTIFIER_6_CONTAINER_ID = CONTAINER_3_ID;
    public final static Long IDENTIFIER_6_DATABASE_ID = DATABASE_3_ID;
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
    public final static String IDENTIFIER_6_NORMALIZED = QUERY_3_STATEMENT;
    public final static Long IDENTIFIER_6_RESULT_NUMBER = QUERY_3_RESULT_NUMBER;
    public final static String IDENTIFIER_6_PUBLISHER = "Norwegian Government";
    public final static IdentifierType IDENTIFIER_6_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_6_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static IdentifierStatusType IDENTIFIER_6_STATUS_TYPE = IdentifierStatusType.PUBLISHED;
    public final static IdentifierStatusTypeDto IDENTIFIER_6_STATUS_TYPE_DTO = IdentifierStatusTypeDto.PUBLISHED;

    public final static Long IDENTIFIER_6_TITLE_1_ID = 4L;
    public final static Long IDENTIFIER_6_TITLE_1_IDENTIFIER_ID = IDENTIFIER_6_ID;
    public final static String IDENTIFIER_6_TITLE_1_TITLE = "Norwegian weather data";
    public final static String IDENTIFIER_6_TITLE_1_TITLE_MODIFY = "Norwegian weather some data";
    public final static LanguageType IDENTIFIER_6_TITLE_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_6_TITLE_1_LANG_DTO = LanguageTypeDto.EN;

    public final static IdentifierTitle IDENTIFIER_6_TITLE_1 = IdentifierTitle.builder()
            .id(IDENTIFIER_6_TITLE_1_ID)
            .title(IDENTIFIER_6_TITLE_1_TITLE)
            .language(IDENTIFIER_6_TITLE_1_LANG)
            .build();

    public final static IdentifierTitleDto IDENTIFIER_6_TITLE_1_DTO = IdentifierTitleDto.builder()
            .id(IDENTIFIER_6_TITLE_1_ID)
            .title(IDENTIFIER_6_TITLE_1_TITLE)
            .language(IDENTIFIER_6_TITLE_1_LANG_DTO)
            .build();

    public final static IdentifierTitleDto IDENTIFIER_6_TITLE_1_DTO_MODIFY = IdentifierTitleDto.builder()
            .id(IDENTIFIER_6_TITLE_1_ID)
            .title(IDENTIFIER_6_TITLE_1_TITLE_MODIFY)
            .language(IDENTIFIER_6_TITLE_1_LANG_DTO)
            .build();

    public final static IdentifierSaveTitleDto IDENTIFIER_6_TITLE_1_CREATE_DTO = IdentifierSaveTitleDto.builder()
            .title(IDENTIFIER_6_TITLE_1_TITLE_MODIFY)
            .language(IDENTIFIER_6_TITLE_1_LANG_DTO)
            .build();

    public final static Long IDENTIFIER_6_DESCRIPTION_1_ID = 3L;
    public final static Long IDENTIFIER_6_DESCRIPTION_1_IDENTIFIER_ID = IDENTIFIER_6_ID;
    public final static String IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION = "Selecting all from the weather Norwegian table";
    public final static String IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION_MODIFY = "Selecting some from the weather Norwegian table";
    public final static LanguageType IDENTIFIER_6_DESCRIPTION_1_LANG = LanguageType.EN;
    public final static LanguageTypeDto IDENTIFIER_6_DESCRIPTION_1_LANG_DTO = LanguageTypeDto.EN;

    public final static IdentifierDescription IDENTIFIER_6_DESCRIPTION_1 = IdentifierDescription.builder()
            .id(IDENTIFIER_6_DESCRIPTION_1_ID)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG)
            .build();

    public final static IdentifierDescriptionDto IDENTIFIER_6_DESCRIPTION_1_DTO = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_6_DESCRIPTION_1_ID)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static IdentifierDescriptionDto IDENTIFIER_6_DESCRIPTION_1_DTO_MODIFY = IdentifierDescriptionDto.builder()
            .id(IDENTIFIER_6_DESCRIPTION_1_ID)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION_MODIFY)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static IdentifierSaveDescriptionDto IDENTIFIER_6_DESCRIPTION_1_CREATE_DTO = IdentifierSaveDescriptionDto.builder()
            .id(null)
            .description(IDENTIFIER_6_DESCRIPTION_1_DESCRIPTION_MODIFY)
            .language(IDENTIFIER_6_DESCRIPTION_1_LANG_DTO)
            .build();

    private final static Long IDENTIFIER_6_CREATOR_1_ID = 4L;

    public final static Creator IDENTIFIER_6_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_6_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeType.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final static CreatorDto IDENTIFIER_6_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final static CreatorSaveDto IDENTIFIER_6_CREATOR_1_CREATE_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
            .build();

    public final static CreatorSaveDto IDENTIFIER_6_CREATOR_1_MODIFY_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(null) /* <<<< */
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ISNI) /* <<<< */
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
            .build();

    private final static Long IDENTIFIER_6_CREATOR_2_ID = 5L;

    public final static Creator IDENTIFIER_6_CREATOR_2 = Creator.builder()
            .id(IDENTIFIER_6_CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .creatorName(CREATOR_2_NAME)
            .nameIdentifier(CREATOR_2_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeType.ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static CreatorDto IDENTIFIER_6_CREATOR_2_DTO = CreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .creatorName(CREATOR_2_NAME)
            .nameIdentifier(CREATOR_2_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    private final static Long IDENTIFIER_6_CREATOR_3_ID = 6L;

    public final static Creator IDENTIFIER_6_CREATOR_3 = Creator.builder()
            .id(IDENTIFIER_6_CREATOR_3_ID)
            .firstname(CREATOR_3_FIRSTNAME)
            .lastname(CREATOR_3_LASTNAME)
            .creatorName(CREATOR_3_NAME)
            .nameIdentifier(CREATOR_3_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeType.ORCID)
            .affiliation(CREATOR_3_AFFIL)
            .affiliationIdentifier(CREATOR_3_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_3_AFFIL_SCHEME_TYPE)
            .affiliationIdentifierSchemeUri(CREATOR_3_AFFIL_URI)
            .build();

    public final static CreatorDto IDENTIFIER_6_CREATOR_3_DTO = CreatorDto.builder()
            .id(IDENTIFIER_6_CREATOR_3_ID)
            .firstname(CREATOR_3_FIRSTNAME)
            .lastname(CREATOR_3_LASTNAME)
            .creatorName(CREATOR_3_NAME)
            .nameIdentifier(CREATOR_3_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_3_AFFIL)
            .affiliationIdentifier(CREATOR_3_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_3_AFFIL_SCHEME_TYPE_DTO)
            .affiliationIdentifierSchemeUri(CREATOR_3_AFFIL_URI)
            .build();

    public final static Identifier IDENTIFIER_6 = Identifier.builder()
            .id(IDENTIFIER_6_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
            .descriptions(new LinkedList<>(List.of(IDENTIFIER_6_DESCRIPTION_1)))
            .titles(new LinkedList<>(List.of(IDENTIFIER_6_TITLE_1)))
            .doi(IDENTIFIER_6_DOI)
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
            .owner(USER_3)
            .ownedBy(USER_3_ID)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_6_CREATOR_1, IDENTIFIER_6_CREATOR_2, IDENTIFIER_6_CREATOR_3)))
            .status(IDENTIFIER_6_STATUS_TYPE)
            .build();

    public final static IdentifierDto IDENTIFIER_6_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(DATABASE_3_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
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
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>(List.of(IDENTIFIER_6_CREATOR_1_DTO, IDENTIFIER_6_CREATOR_2_DTO, IDENTIFIER_6_CREATOR_3_DTO)))
            .status(IDENTIFIER_6_STATUS_TYPE_DTO)
            .build();


    public final static IdentifierBriefDto IDENTIFIER_6_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(DATABASE_3_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
            .titles(new LinkedList<>(List.of(IDENTIFIER_6_TITLE_1_DTO)))
            .doi(IDENTIFIER_6_DOI)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_6_PUBLISHER)
            .type(IDENTIFIER_6_TYPE_DTO)
            .status(IDENTIFIER_6_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_6_CREATE_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_6_DATABASE_ID)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_6_PUBLISHER)
            .build();

    public final static IdentifierSaveDto IDENTIFIER_6_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(IDENTIFIER_6_DATABASE_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
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

    public final static Long IDENTIFIER_7_ID = 7L;
    public final static Long IDENTIFIER_7_DATABASE_ID = DATABASE_4_ID;
    public final static String IDENTIFIER_7_DOI = null;
    public final static Instant IDENTIFIER_7_CREATED = Instant.ofEpochSecond(1641588352L);
    public final static Instant IDENTIFIER_7_MODIFIED = Instant.ofEpochSecond(1541588352L);
    public final static Instant IDENTIFIER_7_EXECUTION = Instant.ofEpochSecond(1541588352L);
    public final static Integer IDENTIFIER_7_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_7_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_7_PUBLICATION_YEAR = 2022;
    public final static Long IDENTIFIER_7_RESULT_NUMBER = 2L;
    public final static String IDENTIFIER_7_PUBLISHER = "Swedish Government";
    public final static IdentifierType IDENTIFIER_7_TYPE = IdentifierType.DATABASE;
    public final static IdentifierTypeDto IDENTIFIER_7_TYPE_DTO = IdentifierTypeDto.DATABASE;
    public final static IdentifierStatusType IDENTIFIER_7_STATUS_TYPE = IdentifierStatusType.DRAFT;
    public final static IdentifierStatusTypeDto IDENTIFIER_7_STATUS_TYPE_DTO = IdentifierStatusTypeDto.DRAFT;

    public final static DataCiteBody<DataCiteDoi> IDENTIFIER_7_DATA_CITE = DataCiteBody.<DataCiteDoi>builder()
            .data(DataCiteData.<DataCiteDoi>builder()
                    .type("dois")
                    .attributes(DataCiteDoi.builder()
                            .doi(IDENTIFIER_7_DOI)
                            .build())
                    .build())
            .build();

    private final static Long IDENTIFIER_7_CREATOR_1_ID = 6L;

    public final static Creator IDENTIFIER_7_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_7_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeType.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final static CreatorDto IDENTIFIER_7_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_7_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
            .affiliationIdentifierSchemeUri(CREATOR_1_AFFIL_URI)
            .build();

    public final static IdentifierDto IDENTIFIER_7_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_7_ID)
            .databaseId(DATABASE_4_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_7_DOI)
            .execution(IDENTIFIER_7_EXECUTION)
            .publicationDay(IDENTIFIER_7_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .resultNumber(IDENTIFIER_7_RESULT_NUMBER)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE_DTO)
            .owner(USER_4_BRIEF_DTO)
            .relatedIdentifiers(new LinkedList<>())
            .licenses(new LinkedList<>())
            .funders(new LinkedList<>())
            .creators(new LinkedList<>(List.of(IDENTIFIER_7_CREATOR_1_DTO)))
            .status(IDENTIFIER_7_STATUS_TYPE_DTO)
            .build();

    public final static CreatorSaveDto IDENTIFIER_7_CREATOR_1_CREATE_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_7_CREATE_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_7_DATABASE_ID)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .build();

    public final static IdentifierSaveDto IDENTIFIER_7_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_7_ID)
            .databaseId(IDENTIFIER_7_DATABASE_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .creators(new LinkedList<>(List.of(IDENTIFIER_7_CREATOR_1_CREATE_DTO)))
            .funders(new LinkedList<>())
            .licenses(new LinkedList<>())
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE_DTO)
            .build();

    public final static Long IDENTIFIER_2_ID = 2L;
    public final static Long IDENTIFIER_2_DATABASE_ID = DATABASE_1_ID;
    public final static String IDENTIFIER_2_DOI = null;
    public final static Instant IDENTIFIER_2_CREATED = Instant.ofEpochSecond(1651588352L);
    public final static Instant IDENTIFIER_2_MODIFIED = Instant.ofEpochSecond(1551588352L);
    public final static Instant IDENTIFIER_2_EXECUTION = Instant.ofEpochSecond(1551588352L);
    public final static Integer IDENTIFIER_2_PUBLICATION_DAY = 10;
    public final static Integer IDENTIFIER_2_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_2_PUBLICATION_YEAR = 2023;
    public final static String IDENTIFIER_2_QUERY_HASH = QUERY_1_QUERY_HASH;
    public final static String IDENTIFIER_2_RESULT_HASH = QUERY_1_RESULT_HASH;
    public final static String IDENTIFIER_2_QUERY = QUERY_1_STATEMENT;
    public final static Long IDENTIFIER_2_QUERY_ID = QUERY_1_ID;
    public final static String IDENTIFIER_2_NORMALIZED = QUERY_1_STATEMENT;
    public final static Long IDENTIFIER_2_RESULT_NUMBER = QUERY_1_RESULT_NUMBER;
    public final static String IDENTIFIER_2_PUBLISHER = "Swedish Government";
    public final static IdentifierType IDENTIFIER_2_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_2_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static IdentifierStatusType IDENTIFIER_2_STATUS_TYPE = IdentifierStatusType.PUBLISHED;
    public final static IdentifierStatusTypeDto IDENTIFIER_2_STATUS_TYPE_DTO = IdentifierStatusTypeDto.PUBLISHED;
    public final static UUID IDENTIFIER_2_CREATED_BY = USER_1_ID;

    public final static IdentifierCreateDto IDENTIFIER_2_CREATE_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .type(IDENTIFIER_2_TYPE_DTO)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_2_PUBLISHER)
            .build();

    public final static Identifier IDENTIFIER_2 = Identifier.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
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
            .owner(USER_1)
            .ownedBy(USER_1_ID)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>())
            .status(IDENTIFIER_2_STATUS_TYPE)
            .build();

    public final static IdentifierDto IDENTIFIER_2_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
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
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>())
            .status(IDENTIFIER_2_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierBriefDto IDENTIFIER_2_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_2_DOI)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .status(IDENTIFIER_2_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierSaveDto IDENTIFIER_2_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_2_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .creators(new LinkedList<>())
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .queryId(QUERY_1_ID)
            .build();

    public final static Long IDENTIFIER_3_ID = 3L;
    public final static Long IDENTIFIER_3_DATABASE_ID = DATABASE_1_ID;
    public final static Long IDENTIFIER_3_VIEW_ID = VIEW_1_ID;
    public final static String IDENTIFIER_3_DOI = null;
    public final static Instant IDENTIFIER_3_CREATED = Instant.ofEpochSecond(1651588352L);
    public final static Instant IDENTIFIER_3_MODIFIED = Instant.ofEpochSecond(1551588352L);
    public final static Instant IDENTIFIER_3_EXECUTION = Instant.ofEpochSecond(1551588352L);
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
    public final static IdentifierStatusType IDENTIFIER_3_STATUS_TYPE = IdentifierStatusType.PUBLISHED;
    public final static IdentifierStatusTypeDto IDENTIFIER_3_STATUS_TYPE_DTO = IdentifierStatusTypeDto.PUBLISHED;
    public final static UUID IDENTIFIER_3_CREATED_BY = USER_1_ID;

    public final static Identifier IDENTIFIER_3 = Identifier.builder()
            .id(IDENTIFIER_3_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_3_DOI)
            .database(null /* DATABASE_1 */)
            .created(IDENTIFIER_3_CREATED)
            .lastModified(IDENTIFIER_3_MODIFIED)
            .execution(IDENTIFIER_3_EXECUTION)
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
            .owner(USER_1)
            .ownedBy(USER_1_ID)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>())
            .status(IDENTIFIER_3_STATUS_TYPE)
            .build();

    public final static IdentifierDto IDENTIFIER_3_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_3_DOI)
            .execution(IDENTIFIER_3_EXECUTION)
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
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .creators(new LinkedList<>())
            .status(IDENTIFIER_3_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierBriefDto IDENTIFIER_3_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_3_DOI)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .status(IDENTIFIER_3_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_3_CREATE_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .type(IDENTIFIER_3_TYPE_DTO)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_3_PUBLISHER)
            .build();

    public final static IdentifierSaveDto IDENTIFIER_3_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_3_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .creators(new LinkedList<>())
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .build();

    public final static Long IDENTIFIER_4_ID = 4L;
    public final static Long IDENTIFIER_4_DATABASE_ID = DATABASE_1_ID;
    public final static Long IDENTIFIER_4_TABLE_ID = TABLE_1_ID;
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
    public final static UUID IDENTIFIER_4_CREATED_BY = USER_1_ID;

    public final static Identifier IDENTIFIER_4 = Identifier.builder()
            .id(IDENTIFIER_4_ID)
            .tableId(IDENTIFIER_4_TABLE_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_4_DOI)
            .database(null /* DATABASE_1 */)
            .created(IDENTIFIER_4_CREATED)
            .lastModified(IDENTIFIER_4_MODIFIED)
            .execution(IDENTIFIER_4_EXECUTION)
            .publicationDay(IDENTIFIER_4_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .resultHash(IDENTIFIER_4_RESULT_HASH)
            .resultNumber(IDENTIFIER_4_RESULT_NUMBER)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE)
            .owner(USER_1)
            .ownedBy(USER_1_ID)
            .licenses(new LinkedList<>(List.of(LICENSE_1)))
            .creators(new LinkedList<>())
            .status(IDENTIFIER_4_STATUS_TYPE)
            .build();

    public final static IdentifierDto IDENTIFIER_4_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .tableId(IDENTIFIER_4_TABLE_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
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
            .creators(new LinkedList<>())
            .status(IDENTIFIER_4_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierBriefDto IDENTIFIER_4_BRIEF_DTO = IdentifierBriefDto.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .tableId(IDENTIFIER_4_TABLE_ID)
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_4_DOI)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE_DTO)
            .status(IDENTIFIER_4_STATUS_TYPE_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_4_CREATE_DTO = IdentifierCreateDto.builder()
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .build();

    public final static IdentifierSaveDto IDENTIFIER_4_SAVE_DTO = IdentifierSaveDto.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .tableId(IDENTIFIER_4_TABLE_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .relatedIdentifiers(new LinkedList<>())
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .creators(new LinkedList<>())
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE_DTO)
            .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
            .build();

    public final static String VIRTUAL_HOST_NAME = "fda";
    public final static String VIRTUAL_HOST_DESCRIPTION = "FAIR Data Austria";
    public final static String VIRTUAL_HOST_TAGS = "";

    public final static CreateVirtualHostDto VIRTUAL_HOST_CREATE_DTO = CreateVirtualHostDto.builder()
            .name(VIRTUAL_HOST_NAME)
            .description(VIRTUAL_HOST_DESCRIPTION)
            .tags(VIRTUAL_HOST_TAGS)
            .build();

    public final static ExchangeUpdatePermissionsDto VIRTUAL_HOST_EXCHANGE_UPDATE_DTO = ExchangeUpdatePermissionsDto.builder()
            .exchange(DATABASE_1_EXCHANGE)
            .read(".*")
            .write(".*")
            .build();

    public final static GrantVirtualHostPermissionsDto VIRTUAL_HOST_GRANT_DTO = GrantVirtualHostPermissionsDto.builder()
            .read(".*")
            .write(".*")
            .configure(".*")
            .build();

    public final static Long BANNER_MESSAGE_1_ID = 1L;
    public final static String BANNER_MESSAGE_1_MESSAGE = "Next maintenance in 7 days!";
    public final static BannerMessageType BANNER_MESSAGE_1_TYPE = BannerMessageType.INFO;
    public final static BannerMessageTypeDto BANNER_MESSAGE_1_TYPE_DTO = BannerMessageTypeDto.INFO;
    public final static Instant BANNER_MESSAGE_1_START = Instant.ofEpochSecond(1684577786L) /* 2022-12-23 22:00:00 (UTC) */;
    public final static Instant BANNER_MESSAGE_1_END = null;

    public final static BannerMessage BANNER_MESSAGE_1 = BannerMessage.builder()
            .id(BANNER_MESSAGE_1_ID)
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BANNER_MESSAGE_1_TYPE)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final static BannerMessageDto BANNER_MESSAGE_1_DTO = BannerMessageDto.builder()
            .id(BANNER_MESSAGE_1_ID)
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BANNER_MESSAGE_1_TYPE_DTO)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final static BannerMessageCreateDto BANNER_MESSAGE_1_CREATE_DTO = BannerMessageCreateDto.builder()
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BANNER_MESSAGE_1_TYPE_DTO)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final static BannerMessageUpdateDto BANNER_MESSAGE_1_UPDATE_DTO = BannerMessageUpdateDto.builder()
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BannerMessageTypeDto.WARNING)
            .displayStart(BANNER_MESSAGE_1_START)
            .displayEnd(BANNER_MESSAGE_1_END)
            .build();

    public final static Long BANNER_MESSAGE_2_ID = 2L;
    public final static String BANNER_MESSAGE_2_MESSAGE = "No operation on Christmas 2022!";
    public final static BannerMessageType BANNER_MESSAGE_2_TYPE = BannerMessageType.ERROR;
    public final static BannerMessageTypeDto BANNER_MESSAGE_2_TYPE_DTO = BannerMessageTypeDto.ERROR;
    public final static Instant BANNER_MESSAGE_2_START = Instant.ofEpochSecond(1671836400L) /* 2022-12-23 22:00:00 (UTC) */;
    public final static Instant BANNER_MESSAGE_2_END = Instant.ofEpochSecond(1672009200L) /* 2022-12-25 22:00:00 (UTC) */;

    public final static BannerMessage BANNER_MESSAGE_2 = BannerMessage.builder()
            .id(BANNER_MESSAGE_2_ID)
            .message(BANNER_MESSAGE_2_MESSAGE)
            .type(BANNER_MESSAGE_2_TYPE)
            .displayStart(BANNER_MESSAGE_2_START)
            .displayEnd(BANNER_MESSAGE_2_END)
            .build();

    public final static BannerMessageCreateDto BANNER_MESSAGE_2_CREATE_DTO = BannerMessageCreateDto.builder()
            .message(BANNER_MESSAGE_2_MESSAGE)
            .type(BANNER_MESSAGE_2_TYPE_DTO)
            .displayStart(BANNER_MESSAGE_2_START)
            .displayEnd(BANNER_MESSAGE_2_END)
            .build();

    public final static Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_1_PUBLIC)
            .isSchemaPublic(DATABASE_1_SCHEMA_PUBLIC)
            .name(DATABASE_1_NAME)
            .description(DATABASE_1_DESCRIPTION)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4)))
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .created(DATABASE_1_CREATED)
            .lastModified(DATABASE_1_LAST_MODIFIED)
            .ownedBy(DATABASE_1_CREATED_BY)
            .owner(USER_1)
            .ownedBy(DATABASE_1_OWNER)
            .owner(USER_1)
            .image(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
            .contactPerson(USER_1_ID)
            .contact(USER_1)
            .subsets(new LinkedList<>())
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .accesses(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final static DatabaseDto DATABASE_1_DTO = DatabaseDto.builder()
            .id(DATABASE_1_ID)
            .isPublic(DATABASE_1_PUBLIC)
            .name(DATABASE_1_NAME)
            .container(CONTAINER_1_BRIEF_DTO)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_1_BRIEF_DTO, IDENTIFIER_2_BRIEF_DTO, IDENTIFIER_3_BRIEF_DTO, IDENTIFIER_4_BRIEF_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_1_BRIEF_DTO, TABLE_2_BRIEF_DTO, TABLE_3_BRIEF_DTO, TABLE_4_BRIEF_DTO)))
            .views(new LinkedList<>(List.of(VIEW_1_BRIEF_DTO, VIEW_2_BRIEF_DTO, VIEW_3_BRIEF_DTO)))
            .build();

    public final static PrivilegedDatabaseDto DATABASE_1_PRIVILEGED_DTO = PrivilegedDatabaseDto.builder()
            .id(DATABASE_1_ID)
            .isPublic(DATABASE_1_PUBLIC)
            .isSchemaPublic(DATABASE_1_SCHEMA_PUBLIC)
            .name(DATABASE_1_NAME)
            .container(CONTAINER_1_PRIVILEGED_DTO)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_1_DTO, IDENTIFIER_2_DTO, IDENTIFIER_3_DTO, IDENTIFIER_4_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO)))
            .views(new LinkedList<>(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO)))
            .owner(USER_1_BRIEF_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static DatabaseAccess DATABASE_1_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccessDto DATABASE_1_USER_1_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_1_ID)
            .user(USER_1_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccessDto DATABASE_1_USER_2_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccessDto DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccessDto DATABASE_1_USER_2_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccessDto DATABASE_1_USER_3_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_3_ID)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_2_CREATED)
            .lastModified(Instant.now())
            .isPublic(DATABASE_2_PUBLIC)
            .isSchemaPublic(DATABASE_2_SCHEMA_PUBLIC)
            .name(DATABASE_2_NAME)
            .description(DATABASE_2_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .created(DATABASE_2_CREATED)
            .lastModified(DATABASE_2_LAST_MODIFIED)
            .ownedBy(DATABASE_2_OWNER)
            .owner(USER_2)
            .contactPerson(USER_2_ID)
            .contact(USER_2)
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .accesses(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final static PrivilegedDatabaseDto DATABASE_2_PRIVILEGED_DTO = PrivilegedDatabaseDto.builder()
            .id(DATABASE_2_ID)
            .isPublic(DATABASE_2_PUBLIC)
            .isSchemaPublic(DATABASE_2_SCHEMA_PUBLIC)
            .name(DATABASE_2_NAME)
            .container(CONTAINER_1_PRIVILEGED_DTO)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_5_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_5_DTO, TABLE_6_DTO, TABLE_7_DTO)))
            .views(new LinkedList<>(List.of(VIEW_4_DTO)))
            .owner(USER_2_BRIEF_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static DatabaseDto DATABASE_2_DTO = DatabaseDto.builder()
            .id(DATABASE_2_ID)
            .isPublic(DATABASE_2_PUBLIC)
            .name(DATABASE_2_NAME)
            .container(CONTAINER_1_BRIEF_DTO)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_5_BRIEF_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_5_BRIEF_DTO, TABLE_6_BRIEF_DTO, TABLE_7_BRIEF_DTO)))
            .views(new LinkedList<>(List.of(VIEW_4_BRIEF_DTO)))
            .identifiers(new LinkedList<>())
            .owner(USER_2_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccessDto DATABASE_2_USER_2_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccessDto DATABASE_2_USER_2_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .user(USER_2_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccessDto DATABASE_2_USER_3_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_3_ID)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static Database DATABASE_3 = Database.builder()
            .id(DATABASE_3_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_3_PUBLIC)
            .isSchemaPublic(DATABASE_3_SCHEMA_PUBLIC)
            .name(DATABASE_3_NAME)
            .description(DATABASE_3_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .created(DATABASE_3_CREATED)
            .lastModified(DATABASE_3_LAST_MODIFIED)
            .ownedBy(DATABASE_3_OWNER)
            .owner(USER_3)
            .contactPerson(USER_3_ID)
            .contact(USER_3)
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .accesses(new LinkedList<>()) /* DATABASE_3_USER_1_WRITE_ALL_ACCESS */
            .identifiers(new LinkedList<>()) /* IDENTIFIER_6 */
            .build();

    public final static DatabaseAccess DATABASE_3_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccessDto DATABASE_3_USER_1_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .user(USER_1_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccessDto DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .user(USER_1_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccessDto DATABASE_3_USER_3_READ_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.READ)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_3_ID)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccessDto DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_3_ID)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccessDto DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO = DatabaseAccessDto.builder()
            .type(AccessTypeDto.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_3_ID)
            .user(USER_3_BRIEF_DTO)
            .build();

    public final static PrivilegedDatabaseDto DATABASE_3_PRIVILEGED_DTO = PrivilegedDatabaseDto.builder()
            .id(DATABASE_3_ID)
            .isPublic(DATABASE_3_PUBLIC)
            .isSchemaPublic(DATABASE_3_SCHEMA_PUBLIC)
            .name(DATABASE_3_NAME)
            .container(CONTAINER_1_PRIVILEGED_DTO)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_6_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_8_DTO)))
            .views(new LinkedList<>(List.of(VIEW_5_DTO)))
            .owner(USER_3_BRIEF_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static Identifier IDENTIFIER_7 = Identifier.builder()
            .id(IDENTIFIER_7_ID)
            .descriptions(new LinkedList<>())
            .titles(new LinkedList<>())
            .doi(IDENTIFIER_7_DOI)
            .created(IDENTIFIER_7_CREATED)
            .lastModified(IDENTIFIER_7_MODIFIED)
            .execution(IDENTIFIER_7_EXECUTION)
            .publicationDay(IDENTIFIER_7_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .resultNumber(IDENTIFIER_7_RESULT_NUMBER)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE)
            .owner(USER_4)
            .ownedBy(USER_4_ID)
            .licenses(new LinkedList<>())
            .creators(new LinkedList<>(List.of(IDENTIFIER_7_CREATOR_1)))
            .relatedIdentifiers(new LinkedList<>())
            .funders(new LinkedList<>())
            .status(IDENTIFIER_7_STATUS_TYPE)
            .build();

    public final static Database DATABASE_4 = Database.builder()
            .id(DATABASE_4_ID)
            .created(Instant.now().minus(4, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_4_PUBLIC)
            .isSchemaPublic(DATABASE_4_SCHEMA_PUBLIC)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .cid(CONTAINER_4_ID)
            .container(CONTAINER_4)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .created(DATABASE_4_CREATED)
            .lastModified(DATABASE_4_LAST_MODIFIED)
            .ownedBy(DATABASE_4_OWNER)
            .owner(USER_4)
            .contactPerson(USER_4_ID)
            .contact(USER_4)
            .tables(new LinkedList<>())
            .views(new LinkedList<>())
            .identifiers(new LinkedList<>())
            .build();

    public final static PrivilegedDatabaseDto DATABASE_4_PRIVILEGED_DTO = PrivilegedDatabaseDto.builder()
            .id(DATABASE_4_ID)
            .isPublic(DATABASE_4_PUBLIC)
            .isSchemaPublic(DATABASE_4_SCHEMA_PUBLIC)
            .name(DATABASE_4_NAME)
            .container(CONTAINER_1_PRIVILEGED_DTO)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .identifiers(new LinkedList<>(List.of(IDENTIFIER_7_DTO)))
            .tables(new LinkedList<>(List.of(TABLE_9_DTO)))
            .views(new LinkedList<>(List.of()))
            .owner(USER_3_BRIEF_DTO)
            .lastRetrieved(Instant.now())
            .build();

    public final static DatabaseAccess DATABASE_4_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_3_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_3_ID)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .database(DATABASE_4)
            .huserid(USER_3_ID)
            .build();

    public final static List<IdentifierDto> VIEW_1_DTO_IDENTIFIERS = List.of(IDENTIFIER_3_DTO);

    public final static Constraints TABLE_1_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_1)
                    .column(TABLE_1_COLUMNS.get(0))
                    .id(1L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_1_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_1_BRIEF_DTO)
                    .column(TABLE_1_COLUMNS_BRIEF_0_DTO)
                    .id(1L)
                    .build())))
            .build();

    public final static Constraints TABLE_2_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>(List.of("`mintemp` > 0")))
            .foreignKeys(new LinkedList<>(List.of(ForeignKey.builder()
                    .id(1L)
                    .name("fk_location")
                    .onDelete(ReferenceType.NO_ACTION)
                    .references(new LinkedList<>(List.of(ForeignKeyReference.builder()
                            .id(1L)
                            .column(TABLE_2_COLUMNS.get(2))
                            .referencedColumn(TABLE_1_COLUMNS.get(0))
                            .foreignKey(null) // set later
                            .build())))
                    .table(TABLE_2)
                    .referencedTable(TABLE_1)
                    .onUpdate(ReferenceType.NO_ACTION)
                    .build())))
            .uniques(new LinkedList<>(List.of(Unique.builder()
                    .id(1L)
                    .table(TABLE_2)
                    .name("uk_1")
                    .columns(new LinkedList<>(List.of(TABLE_2_COLUMNS.get(1))))
                    .build())))
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_2)
                    .column(TABLE_2_COLUMNS.get(0))
                    .id(2L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_2_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>(List.of("`mintemp` > 0")))
            .foreignKeys(new LinkedList<>(List.of(ForeignKeyDto.builder()
                    .id(1L)
                    .name("fk_location")
                    .onDelete(ReferenceTypeDto.NO_ACTION)
                    .references(new LinkedList<>(List.of(ForeignKeyReferenceDto.builder()
                            .id(1L)
                            .column(TABLE_2_COLUMNS_BRIEF_2_DTO)
                            .referencedColumn(TABLE_1_COLUMNS_BRIEF_0_DTO)
                            .foreignKey(null) // set later
                            .build())))
                    .table(TABLE_1_BRIEF_DTO)
                    .referencedTable(TABLE_2_BRIEF_DTO)
                    .onUpdate(ReferenceTypeDto.NO_ACTION)
                    .build())))
            .uniques(new LinkedList<>(List.of(UniqueDto.builder()
                    .id(1L)
                    .table(TABLE_2_BRIEF_DTO)
                    .name("uk_1")
                    .columns(new LinkedList<>(List.of(TABLE_2_COLUMNS_DTO.get(1))))
                    .build())))
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_2_BRIEF_DTO)
                    .column(TABLE_2_COLUMNS_BRIEF_0_DTO)
                    .id(2L)
                    .build())))
            .build();

    public final static Constraints TABLE_3_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_3)
                    .column(TABLE_3_COLUMNS.get(0))
                    .id(3L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_3_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_3_BRIEF_DTO)
                    .column(TABLE_3_COLUMNS_BRIEF_0_DTO)
                    .id(3L)
                    .build())))
            .build();

    public final static Constraints TABLE_4_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_4)
                    .column(TABLE_4_COLUMNS.get(0))
                    .id(4L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_4_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_4_BRIEF_DTO)
                    .column(TABLE_4_COLUMNS_BRIEF_0_DTO)
                    .id(4L)
                    .build())))
            .build();

    public final static Constraints TABLE_5_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_5)
                    .column(TABLE_5_COLUMNS.get(0))
                    .id(5L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_5_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_5_BRIEF_DTO)
                    .column(TABLE_5_COLUMNS_BRIEF_0_DTO)
                    .id(5L)
                    .build())))
            .build();

    public final static Constraints TABLE_6_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>(List.of()))
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_6)
                    .column(TABLE_6_COLUMNS.get(0))
                    .id(6L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_6_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_6_BRIEF_DTO)
                    .column(TABLE_6_COLUMNS_BRIEF_0_DTO)
                    .id(6L)
                    .build())))
            .build();

    public final static Constraints TABLE_7_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>(List.of(ForeignKey.builder()
                            .id(8L)
                            .name("fk_name_id")
                            .onDelete(ReferenceType.NO_ACTION)
                            .references(new LinkedList<>(List.of(ForeignKeyReference.builder()
                                    .id(2L)
                                    .column(TABLE_6_COLUMNS.get(0))
                                    .referencedColumn(TABLE_7_COLUMNS.get(0))
                                    .foreignKey(null) // set later
                                    .build())))
                            .table(TABLE_7)
                            .referencedTable(TABLE_6)
                            .onUpdate(ReferenceType.NO_ACTION)
                            .build(),
                    ForeignKey.builder()
                            .id(9L)
                            .name("fk_zoo_id")
                            .onDelete(ReferenceType.NO_ACTION)
                            .references(new LinkedList<>(List.of(ForeignKeyReference.builder()
                                    .id(3L)
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
                    .id(7L)
                    .build())))
            .build();

    public final static ForeignKeyDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_0_DTO = ForeignKeyDto.builder()
            .id(2L)
            .name("fk_name_id")
            .onDelete(ReferenceTypeDto.NO_ACTION)
            .references(new LinkedList<>(List.of(ForeignKeyReferenceDto.builder()
                    .id(2L)
                    .column(TABLE_6_COLUMNS_BRIEF_0_DTO)
                    .referencedColumn(TABLE_7_COLUMNS_BRIEF_0_DTO)
                    .foreignKey(null) // set later
                    .build())))
            .table(TABLE_7_BRIEF_DTO)
            .referencedTable(TABLE_6_BRIEF_DTO)
            .onUpdate(ReferenceTypeDto.NO_ACTION)
            .build();

    public final static ForeignKeyBriefDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_0_DTO = ForeignKeyBriefDto.builder()
            .id(2L)
            .build();

    public final static ForeignKeyDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_1_DTO = ForeignKeyDto.builder()
            .id(3L)
            .name("fk_zoo_id")
            .onDelete(ReferenceTypeDto.NO_ACTION)
            .references(new LinkedList<>(List.of(ForeignKeyReferenceDto.builder()
                    .id(3L)
                    .column(TABLE_5_COLUMNS_BRIEF_0_DTO)
                    .referencedColumn(TABLE_7_COLUMNS_BRIEF_1_DTO)
                    .foreignKey(null) // set later
                    .build())))
            .table(TABLE_7_BRIEF_DTO)
            .referencedTable(TABLE_5_BRIEF_DTO)
            .onUpdate(ReferenceTypeDto.NO_ACTION)
            .build();

    public final static ForeignKeyBriefDto TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_1_DTO = ForeignKeyBriefDto.builder()
            .id(3L)
            .build();

    public final static ConstraintsDto TABLE_7_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>(List.of(TABLE_7_CONSTRAINTS_FOREIGN_KEY_0_DTO,
                    TABLE_7_CONSTRAINTS_FOREIGN_KEY_1_DTO)))
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_7_BRIEF_DTO)
                    .column(TABLE_7_COLUMNS_BRIEF_0_DTO)
                    .id(7L)
                    .build())))
            .build();

    public final static Constraints TABLE_8_CONSTRAINTS = Constraints.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedList<>(List.of(PrimaryKey.builder()
                    .table(TABLE_8)
                    .column(TABLE_8_COLUMNS.get(0))
                    .id(8L)
                    .build())))
            .build();

    public final static ConstraintsDto TABLE_8_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .checks(new LinkedHashSet<>())
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .primaryKey(new LinkedHashSet<>(Set.of(PrimaryKeyDto.builder()
                    .table(TABLE_8_BRIEF_DTO)
                    .column(TABLE_8_COLUMNS_BRIEF_0_DTO)
                    .id(8L)
                    .build())))
            .build();

    public final static ExportResourceDto EXPORT_RESOURCE_DTO = ExportResourceDto.builder()
            .filename("68b329da9893e34099c7d8ad5cb9c940")
            .resource(new InputStreamResource(InputStream.nullInputStream()))
            .build();

    public static void saveObservedMetrics(Map<String, String> observedMetrics) throws IOException {
        final int keySize = observedMetrics.keySet().stream().max(Comparator.comparingInt(String::length)).get().length();
        final int valueSize = observedMetrics.values().stream().max(Comparator.comparingInt(String::length)).get().length();
        final StringBuilder content = new StringBuilder("| ")
                .append(StringUtils.rightPad("**Metric**", Integer.max(keySize + 2, 16)))
                .append(" | ")
                .append(StringUtils.rightPad("**Description**", Integer.max(valueSize, 19)))
                .append(" |\n")
                .append("|-")
                .append(StringUtils.leftPad("", Integer.max(keySize + 2, 16), "-"))
                .append("-|-")
                .append(StringUtils.leftPad("", Integer.max(valueSize, 19), "-"))
                .append("-|\n");
        observedMetrics.forEach((key, value) -> content.append("| ")
                .append(StringUtils.rightPad("`" + key + "`", Integer.max(keySize + 2, 16)))
                .append(" | ")
                .append(StringUtils.rightPad(value, Integer.max(valueSize, 19)))
                .append(" |\n"));
        FileUtils.writeStringToFile(new File("../metrics.md"), content.toString(), Charset.defaultCharset());
    }

}
