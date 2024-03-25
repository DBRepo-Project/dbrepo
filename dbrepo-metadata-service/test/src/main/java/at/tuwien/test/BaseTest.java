package at.tuwien.test;

import at.tuwien.api.amqp.*;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.*;
import at.tuwien.api.database.*;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.columns.concepts.*;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyCreateDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.identifier.*;
import at.tuwien.api.keycloak.CredentialDto;
import at.tuwien.api.keycloak.CredentialTypeDto;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
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
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.api.user.*;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.entities.database.*;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKey;
import at.tuwien.entities.database.table.constraints.foreignKey.ForeignKeyReference;
import at.tuwien.entities.database.table.constraints.unique.Unique;
import at.tuwien.entities.identifier.*;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.entities.maintenance.BannerMessageType;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.entities.user.User;
import at.tuwien.querystore.Query;
import at.tuwien.test.utils.ArrayUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigInteger;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Database 1 (Private, User 1) -> Container 1
 * <ul>
 * <li>Table 1</li>
 * <li>Table 2</li>
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
 * Database 2 (Private, User 2) -> Container 1
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
 * Database 3 (Public, User 3) -> Container 1
 * <ul>
 * <li>Table 8</li>
 * <li>Query 3</li>
 * <li>Query 4</li>
 * <li>Query 5</li>
 * <li>View 5</li>
 * <li>Identifier 6 (Title=en, Description=en, Query=3)</li>
 * </ul>
 * <p>
 * Database 4 (Public, User 4)
 * <li>Identifier 7 (Database=4)</li>
 * <ul>
 * </ul>
 * <br />
 * User 1 (authorities=default researcher)
 * <br />
 * User 2 (authorities=default developer)
 * <br />
 * User 3 (authorities=default data-steward)
 */
public abstract class BaseTest {

    public final static String[] DEFAULT_SEMANTICS_HANDLING = new String[]{"default-semantics-handling",
            "create-semantic-unit", "execute-semantic-query", "table-semantic-analyse", "create-semantic-concept"};

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
            "create-identifier", "find-identifier", "list-identifiers"};

    public final static String[] ESCALATED_IDENTIFIER_HANDLING = new String[]{"escalated-identifier-handling",
            "modify-identifier-metadata", "delete-identifier", "update-foreign-identifier", "create-foreign-identifier"};

    public final static String[] DEFAULT_QUERY_HANDLING = new String[]{"default-query-handling", "view-table-data",
            "execute-query", "view-table-history", "list-database-views", "list-queries", "view-database-view-data",
            "export-query-data", "find-query", "create-database-view", "delete-database-view", "delete-table-data",
            "export-table-data", "persist-query", "re-execute-query", "insert-table-data", "find-database-view"};

    public final static String[] ESCALATED_QUERY_HANDLING = new String[]{"escalated-query-handling"};

    public final static String[] DEFAULT_TABLE_HANDLING = new String[]{"default-table-handling",
            "list-tables", "create-table", "modify-table-column-semantics", "find-table", "delete-table"};

    public final static String[] ESCALATED_TABLE_HANDLING = new String[]{"escalated-table-handling",
            "delete-foreign-table"};

    public final static String[] DEFAULT_USER_HANDLING = new String[]{"default-user-handling", "modify-user-theme",
            "modify-user-information"};

    public final static String[] ESCALATED_USER_HANDLING = new String[]{"escalated-user-handling", "find-user"};

    public final static String[] DEFAULT_RESEARCHER_ROLES = ArrayUtil.merge(List.of(new String[]{"default-researcher-roles"},
            DEFAULT_CONTAINER_HANDLING, DEFAULT_DATABASE_HANDLING, DEFAULT_IDENTIFIER_HANDLING, DEFAULT_QUERY_HANDLING,
            DEFAULT_TABLE_HANDLING, DEFAULT_USER_HANDLING, DEFAULT_SEMANTICS_HANDLING));

    public final static String[] DEFAULT_DEVELOPER_ROLES = ArrayUtil.merge(List.of(new String[]{"default-developer-roles"},
            DEFAULT_CONTAINER_HANDLING, DEFAULT_DATABASE_HANDLING, DEFAULT_IDENTIFIER_HANDLING, DEFAULT_QUERY_HANDLING,
            DEFAULT_TABLE_HANDLING, DEFAULT_USER_HANDLING, ESCALATED_USER_HANDLING, ESCALATED_CONTAINER_HANDLING,
            ESCALATED_DATABASE_HANDLING, ESCALATED_IDENTIFIER_HANDLING, ESCALATED_QUERY_HANDLING,
            ESCALATED_TABLE_HANDLING));

    public final static String[] DEFAULT_DATA_STEWARD_ROLES = ArrayUtil.merge(List.of(new String[]{"default-data-steward-roles"},
            ESCALATED_IDENTIFIER_HANDLING, DEFAULT_SEMANTICS_HANDLING, ESCALATED_SEMANTICS_HANDLING));

    public final static List<GrantedAuthorityDto> AUTHORITY_DEFAULT_RESEARCHER_ROLES = Arrays.stream(DEFAULT_RESEARCHER_ROLES)
            .map(GrantedAuthorityDto::new)
            .collect(Collectors.toList());

    public final static List<GrantedAuthorityDto> AUTHORITY_DEFAULT_DEVELOPER_ROLES = Arrays.stream(DEFAULT_DEVELOPER_ROLES)
            .map(GrantedAuthorityDto::new)
            .collect(Collectors.toList());

    public final static List<GrantedAuthorityDto> AUTHORITY_DEFAULT_DATA_STEWARD_ROLES = Arrays.stream(DEFAULT_DATA_STEWARD_ROLES)
            .map(GrantedAuthorityDto::new)
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

    public final static UserThemeSetDto USER_THEME_DARK_DTO = UserThemeSetDto.builder()
            .theme("dark")
            .build();

    public final static UserThemeSetDto USER_THEME_LIGHT_DTO = UserThemeSetDto.builder()
            .theme("light")
            .build();

    public final static UUID REALM_DBREPO_ID = UUID.fromString("6264bf7b-d1d3-4562-9c07-ce4364a8f9d3");
    public final static String REALM_DBREPO_NAME = "dbrepo";
    public final static Boolean REALM_DBREPO_ENABLED = true;

    public final static UUID ROLE_DEFAULT_REALM_DBREPO_ROLES_ID = UUID.fromString("c74cbbe7-3ab1-4472-9211-cc904567268");
    public final static String ROLE_DEFAULT_REALM_DBREPO_ROLES_NAME = "default-dbrepo-roles";
    public final static UUID ROLE_DEFAULT_REALM_DBREPO_ROLES_REALM_ID = REALM_DBREPO_ID;

    public final static UUID ROLE_DEFAULT_RESEARCHER_ROLES_ID = UUID.fromString("c74cbbe7-3ab1-4472-9211-cc9045672682");
    public final static String ROLE_DEFAULT_RESEARCHER_ROLES_NAME = "default-researcher-roles";
    public final static UUID ROLE_DEFAULT_RESEARCHER_ROLES_REALM_ID = REALM_DBREPO_ID;

    public final static TokenDto TOKEN_DTO = TokenDto.builder()
            .accessToken("ey.yee.skrr")
            .scope("openid")
            .build();

    public final static String USER_BROKER_USERNAME = "guest";
    public final static String USER_BROKER_PASSWORD = "guest";

    public final static UUID USER_1_ID = UUID.fromString("cd5bab0d-7799-4069-85fb-c5d738572a0b");
    public final static String USER_1_EMAIL = "john.doe@example.com";
    public final static String USER_1_USERNAME = "junit1";
    public final static String USER_1_PASSWORD = "junit1";
    public final static String USER_1_PASSWORD_ENCODED = "$2a$10$0dtdedA/RLTrFbUsvpbUw.I73AXOKeQP3t5UXj96OvnDEaDb3d3M6";
    public final static String USER_1_DATABASE_PASSWORD = "*440BA4FD1A87A0999647DB67C0EE258198B247BA" /* junit1 */;
    public final static String USER_1_FIRSTNAME = "John";
    public final static String USER_1_LASTNAME = "Doe";
    public final static String USER_1_NAME = "John Doe";
    public final static String USER_1_AFFILIATION = "TU Graz";
    public final static String USER_1_ORCID = "000000034216302X";
    public final static String USER_1_ORCID_UNCOMPRESSED = "0000-0003-4216-302X";
    public final static String USER_1_ORCID_URL = "https://orcid.org/" + USER_1_ORCID_UNCOMPRESSED;
    public final static String USER_1_TITLES_BEFORE = "Dr.";
    public final static String USER_1_TITLES_AFTER = "MSc BSc";
    public final static Boolean USER_1_VERIFIED = false;
    public final static Boolean USER_1_TOTP = false;
    public final static Long USER_1_NOT_BEFORE = 0L;
    public final static Boolean USER_1_ENABLED = true;
    public final static String USER_1_THEME = "light";
    public final static Instant USER_1_CREATED = Instant.ofEpochSecond(1677399441L) /* 2023-02-26 08:17:21 (UTC) */;
    public final static Instant USER_1_LAST_MODIFIED = USER_1_CREATED;
    public final static UUID USER_1_REALM_ID = REALM_DBREPO_ID;

    public final static CreateUserDto USER_1_RABBITMQ_CREATE_DTO = CreateUserDto.builder()
            .password("")
            .tags("")
            .build();

    public final static GrantVirtualHostPermissionsDto USER_1_RABBITMQ_GRANT_DTO = GrantVirtualHostPermissionsDto.builder()
            .configure("")
            .read("")
            .write("")
            .build();

    public final static UserAttributesDto USER_1_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_1_THEME)
            .orcid(USER_1_ORCID_UNCOMPRESSED)
            .affiliation(USER_1_AFFILIATION)
            .mariadbPassword(USER_1_DATABASE_PASSWORD)
            .build();

    public final static CredentialDto USER_1_KEYCLOAK_CREDENTIAL_1 = CredentialDto.builder()
            .type(CredentialTypeDto.PASSWORD)
            .temporary(false)
            .value(USER_1_PASSWORD)
            .build();

    public final static UserCreateDto USER_1_KEYCLOAK_SIGNUP_REQUEST = UserCreateDto.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .enabled(USER_1_ENABLED)
            .credentials(List.of(USER_1_KEYCLOAK_CREDENTIAL_1))
            .build();

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .affiliation(USER_1_AFFILIATION)
            .orcid(USER_1_ORCID)
            .theme(USER_1_THEME)
            .mariadbPassword(USER_1_DATABASE_PASSWORD)
            .build();

    public final static UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .attributes(USER_1_ATTRIBUTES_DTO)
            .name(USER_1_NAME)
            .build();

    public final static UserUpdateDto USER_1_UPDATE_DTO = UserUpdateDto.builder()
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .affiliation(USER_1_AFFILIATION)
            .orcid(USER_1_ORCID)
            .build();

    public final static UserThemeSetDto USER_1_THEME_SET_DTO = UserThemeSetDto.builder()
            .theme(USER_1_THEME)
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
            .build();

    public final static UserBriefDto USER_1_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .name(USER_1_NAME)
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

    public final static at.tuwien.api.amqp.UserDetailsDto USER_1_DETAILS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_1_USERNAME)
            .tags(new String[]{})
            .build();

    public final static at.tuwien.api.amqp.UserDetailsDto USER_1_DETAILS_WITH_TAGS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_1_USERNAME)
            .tags(new String[]{"administrator"})
            .build();

    public final static UUID USER_2_ID = UUID.fromString("eeb9a51b-4cd8-4039-90bf-e24f17372f7c");
    public final static String USER_2_EMAIL = "jane.doe@example.com";
    public final static String USER_2_USERNAME = "junit2";
    public final static String USER_2_FIRSTNAME = "Jane";
    public final static String USER_2_LASTNAME = "Doe";
    public final static String USER_2_NAME = "Jane Doe";
    public final static String USER_2_AFFILIATION = "TU Wien";
    public final static String USER_2_ORCID_URL = "https://orcid.org/0000-0002-9272-6225";
    public final static String USER_2_PASSWORD = "junit2";
    public final static String USER_2_DATABASE_PASSWORD = "*9AA70A8B0EEFAFCB5BED5BDEF6EE264D5DA915AE" /* junit2 */;
    public final static Boolean USER_2_VERIFIED = true;
    public final static Boolean USER_2_TOTP = false;
    public final static Long USER_2_NOT_BEFORE = 0L;
    public final static Boolean USER_2_ENABLED = true;
    public final static String USER_2_THEME = "light";
    public final static Instant USER_2_CREATED = Instant.ofEpochSecond(1677399528L) /* 2023-02-26 08:18:48 (UTC) */;
    public final static Instant USER_2_LAST_MODIFIED = USER_1_CREATED;
    public final static UUID USER_2_REALM_ID = REALM_DBREPO_ID;

    public final static UserAttributesDto USER_2_ATTRIBUTES_DTO = UserAttributesDto.builder()
            .theme(USER_2_THEME)
            .orcid(USER_2_ORCID_URL)
            .affiliation(USER_2_AFFILIATION)
            .mariadbPassword(USER_2_DATABASE_PASSWORD)
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
            .build();

    public final static UserDto USER_2_DTO = UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .name(USER_2_NAME)
            .build();

    public final static UserBriefDto USER_2_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .name(USER_2_NAME)
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
            .authorities(AUTHORITY_DEFAULT_DEVELOPER_AUTHORITIES)
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

    public final static Principal USER_2_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_2_DETAILS,
            USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());

    public final static UUID USER_3_ID = UUID.fromString("7b080e33-d8db-4276-9d53-47208e657006");
    public final static String USER_3_USERNAME = "junit3";
    public final static String USER_3_FIRSTNAME = "System";
    public final static String USER_3_LASTNAME = "System";
    public final static String USER_3_NAME = "System System";
    public final static String USER_3_AFFILIATION = "TU Wien";
    public final static String USER_3_ORCID_URL = null;
    public final static String USER_3_ORCID_UNCOMPRESSED = null;
    public final static String USER_3_EMAIL = "system@example.com";
    public final static String USER_3_PASSWORD = "password";
    public final static String USER_3_DATABASE_PASSWORD = "*D65FCA043964B63E849DD6334699ECB065905DA4" /* junit3 */;
    public final static Boolean USER_3_VERIFIED = true;
    public final static Boolean USER_3_TOTP = false;
    public final static Long USER_3_NOT_BEFORE = 0L;
    public final static Boolean USER_3_ENABLED = true;
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
            .build();

    public final static UserDto USER_3_DTO = UserDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .name(USER_3_NAME)
            .build();

    public final static UserBriefDto USER_3_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .name(USER_3_NAME)
            .build();

    public final static UserDetails USER_3_DETAILS = UserDetailsDto.builder()
            .id(USER_3_ID.toString())
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_DATA_STEWARD_AUTHORITIES)
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

    public final static UUID USER_4_ID = UUID.fromString("791d58c5-bfab-4520-b4fc-b44d4ab9feb0");
    public final static String USER_4_USERNAME = "junit4";
    public final static String USER_4_FIRSTNAME = "JUnit";
    public final static String USER_4_LASTNAME = "4";
    public final static String USER_4_NAME = "JUnit 4";
    public final static String USER_4_AFFILIATION = "TU Wien";
    public final static String USER_4_ORCID_URL = null;
    public final static String USER_4_PASSWORD = "junit4";
    public final static String USER_4_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit4 */;
    public final static String USER_4_EMAIL = "junit4@ossdip.at";
    public final static Boolean USER_4_VERIFIED = true;
    public final static Boolean USER_4_ENABLED = true;
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
            .build();

    public final static UserDto USER_4_DTO = UserDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .attributes(USER_4_ATTRIBUTES_DTO)
            .build();

    public final static UserBriefDto USER_4_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .build();

    public final static UserDetails USER_4_DETAILS = UserDetailsDto.builder()
            .id(USER_4_ID.toString())
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .password(USER_4_PASSWORD)
            .authorities(List.of())
            .build();

    public final static Principal USER_4_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_4_DETAILS,
            USER_4_PASSWORD, USER_4_DETAILS.getAuthorities());

    public final static UUID USER_5_ID = UUID.fromString("28ff851d-d7bc-4422-959c-edd7a5b15630");
    public final static String USER_5_USERNAME = "system";
    public final static String USER_5_FIRSTNAME = "System";
    public final static String USER_5_LASTNAME = "System";
    public final static String USER_5_NAME = "System System";
    public final static String USER_5_AFFILIATION = "TU Wien";
    public final static String USER_5_ORCID = null;
    public final static String USER_5_PASSWORD = "junit5";
    public final static String USER_5_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit5 */;
    public final static String USER_5_EMAIL = "system@ossdip.at";
    public final static Boolean USER_5_VERIFIED = true;
    public final static Boolean USER_5_ENABLED = true;
    public final static String USER_5_THEME = "dark";
    public final static Instant USER_5_CREATED = Instant.ofEpochSecond(1677399592L) /* 2023-02-26 08:19:52 (UTC) */;
    public final static UUID USER_5_REALM_ID = REALM_DBREPO_ID;

    public final static UserDto USER_5_DTO = UserDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .build();

    public final static UserDetails USER_5_DETAILS = UserDetailsDto.builder()
            .id(USER_5_ID.toString())
            .username(USER_5_USERNAME)
            .email(USER_5_EMAIL)
            .password(USER_5_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
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
            .build();

    public final static UUID USER_6_ID = UUID.fromString("28ff851d-d7bc-4422-959c-edd7a5b15630");
    public final static String USER_6_USERNAME = "system";
    public final static String USER_6_FIRSTNAME = "System";
    public final static String USER_6_LASTNAME = "System";
    public final static String USER_6_NAME = "System System";
    public final static String USER_6_AFFILIATION = "TU Wien";
    public final static String USER_6_ORCID = null;
    public final static String USER_6_PASSWORD = "junit5";
    public final static String USER_6_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit5 */;
    public final static String USER_6_EMAIL = "system@ossdip.at";
    public final static Boolean USER_6_VERIFIED = true;
    public final static Boolean USER_6_ENABLED = true;
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
    public final static String IMAGE_1_REGISTRY = "docker.io/library";
    public final static String IMAGE_1_NAME = "mariadb";
    public final static String IMAGE_1_VERSION = "11.1.3";
    public final static String IMAGE_1_DIALECT = "org.hibernate.dialect.MariaDBDialect";
    public final static String IMAGE_1_DRIVER = "org.mariadb.jdbc.Driver";
    public final static String IMAGE_1_JDBC = "mariadb";
    public final static Integer IMAGE_1_PORT = 3306;

    public final static Long IMAGE_DATE_1_ID = 1L;
    public final static Long IMAGE_DATE_1_IMAGE_ID = IMAGE_1_ID;
    public final static String IMAGE_DATE_1_UNIX_FORMAT = "yyyy-MM-dd";
    public final static String IMAGE_DATE_1_DATABASE_FORMAT = "%Y-%c-%d";
    public final static String IMAGE_DATE_1_EXAMPLE = "2022-01-30";
    public final static Boolean IMAGE_DATE_1_HAS_TIME = false;

    public final static ContainerImageDate IMAGE_DATE_1 = ContainerImageDate.builder()
            .id(IMAGE_DATE_1_ID)
            .iid(IMAGE_DATE_1_IMAGE_ID)
            .unixFormat(IMAGE_DATE_1_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_1_DATABASE_FORMAT)
            .example(IMAGE_DATE_1_EXAMPLE)
            .hasTime(IMAGE_DATE_1_HAS_TIME)
            .build();

    public final static ImageDateDto IMAGE_DATE_1_DTO = ImageDateDto.builder()
            .id(IMAGE_DATE_1_ID)
            .unixFormat(IMAGE_DATE_1_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_1_DATABASE_FORMAT)
            .example(IMAGE_DATE_1_EXAMPLE)
            .hasTime(IMAGE_DATE_1_HAS_TIME)
            .build();

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

    public final static Long IMAGE_DATE_2_ID = 2L;
    public final static Long IMAGE_DATE_2_IMAGE_ID = IMAGE_1_ID;
    public final static String IMAGE_DATE_2_UNIX_FORMAT = "dd.MM.yy";
    public final static String IMAGE_DATE_2_DATABASE_FORMAT = "%d.%c.%y";
    public final static String IMAGE_DATE_2_EXAMPLE = "30.01.2022";
    public final static Boolean IMAGE_DATE_2_HAS_TIME = false;

    public final static ContainerImageDate IMAGE_DATE_2 = ContainerImageDate.builder()
            .id(IMAGE_DATE_2_ID)
            .iid(IMAGE_DATE_2_IMAGE_ID)
            .unixFormat(IMAGE_DATE_2_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_2_DATABASE_FORMAT)
            .example(IMAGE_DATE_2_EXAMPLE)
            .hasTime(IMAGE_DATE_2_HAS_TIME)
            .build();

    public final static ImageDateDto IMAGE_DATE_2_DTO = ImageDateDto.builder()
            .id(IMAGE_DATE_2_ID)
            .unixFormat(IMAGE_DATE_2_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_2_DATABASE_FORMAT)
            .example(IMAGE_DATE_2_EXAMPLE)
            .hasTime(IMAGE_DATE_2_HAS_TIME)
            .build();

    public final static Long IMAGE_DATE_3_ID = 3L;
    public final static Long IMAGE_DATE_3_IMAGE_ID = IMAGE_1_ID;
    public final static String IMAGE_DATE_3_UNIX_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
    public final static String IMAGE_DATE_3_DATABASE_FORMAT = "%Y-%c-%dT%H:%i:%S.%f";
    public final static String IMAGE_DATE_3_EXAMPLE = "2022-01-30T13:44:25.499";
    public final static Boolean IMAGE_DATE_3_HAS_TIME = true;

    public final static ContainerImageDate IMAGE_DATE_3 = ContainerImageDate.builder()
            .id(IMAGE_DATE_3_ID)
            .iid(IMAGE_DATE_3_IMAGE_ID)
            .unixFormat(IMAGE_DATE_3_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_3_DATABASE_FORMAT)
            .example(IMAGE_DATE_3_EXAMPLE)
            .hasTime(IMAGE_DATE_3_HAS_TIME)
            .build();

    public final static ImageDateDto IMAGE_DATE_3_DTO = ImageDateDto.builder()
            .id(IMAGE_DATE_3_ID)
            .unixFormat(IMAGE_DATE_3_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_3_DATABASE_FORMAT)
            .example(IMAGE_DATE_3_EXAMPLE)
            .hasTime(IMAGE_DATE_3_HAS_TIME)
            .build();

    public final static Long IMAGE_DATE_4_ID = 4L;
    public final static Long IMAGE_DATE_4_IMAGE_ID = IMAGE_1_ID;
    public final static String IMAGE_DATE_4_UNIX_FORMAT = "HH:mm:ss";
    public final static String IMAGE_DATE_4_DATABASE_FORMAT = "%H:%i:%S";
    public final static String IMAGE_DATE_4_EXAMPLE = "14:44:25";
    public final static Boolean IMAGE_DATE_4_HAS_TIME = true;

    public final static ContainerImageDate IMAGE_DATE_4 = ContainerImageDate.builder()
            .id(IMAGE_DATE_4_ID)
            .iid(IMAGE_DATE_4_IMAGE_ID)
            .unixFormat(IMAGE_DATE_4_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_4_DATABASE_FORMAT)
            .example(IMAGE_DATE_4_EXAMPLE)
            .hasTime(IMAGE_DATE_4_HAS_TIME)
            .build();

    public final static ImageDateDto IMAGE_DATE_4_DTO = ImageDateDto.builder()
            .id(IMAGE_DATE_4_ID)
            .unixFormat(IMAGE_DATE_4_UNIX_FORMAT)
            .databaseFormat(IMAGE_DATE_4_DATABASE_FORMAT)
            .example(IMAGE_DATE_4_EXAMPLE)
            .hasTime(IMAGE_DATE_4_HAS_TIME)
            .build();

    public final static ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_PORT)
            .dateFormats(List.of(IMAGE_DATE_1, IMAGE_DATE_2, IMAGE_DATE_3, IMAGE_DATE_4))
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
            .dateFormats(List.of(IMAGE_DATE_1_DTO, IMAGE_DATE_2_DTO, IMAGE_DATE_3_DTO))
            .build();

    public final static ImageBriefDto IMAGE_1_BRIEF_DTO = ImageBriefDto.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .build();

    public final static Long CONTAINER_1_ID = 1L;
    public final static ContainerImage CONTAINER_1_IMAGE = IMAGE_1;
    public final static ImageDto CONTAINER_1_IMAGE_DTO = IMAGE_1_DTO;
    public final static String CONTAINER_1_NAME = "u01";
    public final static String CONTAINER_1_INTERNALNAME = "dbrepo-userdb-u01";
    public final static String CONTAINER_1_IP = "127.0.0.1";
    public final static String CONTAINER_1_UI_HOST = "localhost";
    public final static Integer CONTAINER_1_UI_PORT = 3306;
    public final static String CONTAINER_1_UI_ADDITIONAL_FLAGS = "?sslMode=disable";
    public final static Boolean CONTAINER_1_RUNNING = true;
    public final static String CONTAINER_1_HOST = "localhost";
    public final static Integer CONTAINER_1_PORT = 3308;
    public final static String CONTAINER_1_SIDECAR_HOST = "localhost";
    public final static Integer CONTAINER_1_SIDECAR_PORT = 33081;
    public final static String CONTAINER_1_PRIVILEGED_USERNAME = "root";
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
            .uiAdditionalFlags(CONTAINER_1_UI_ADDITIONAL_FLAGS)
            .sidecarHost(CONTAINER_1_SIDECAR_HOST)
            .sidecarPort(CONTAINER_1_SIDECAR_PORT)
            .privilegedUsername(CONTAINER_1_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_1_PRIVILEGED_PASSWORD)
            .build();

    public final static ContainerDto CONTAINER_1_DTO = ContainerDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .image(CONTAINER_1_IMAGE_DTO)
            .created(CONTAINER_1_CREATED)
            .host(CONTAINER_1_HOST)
            .port(CONTAINER_1_PORT)
            .sidecarHost(CONTAINER_1_SIDECAR_HOST)
            .sidecarPort(CONTAINER_1_SIDECAR_PORT)
            .build();

    public final static ContainerBriefDto CONTAINER_1_DTO_BRIEF = ContainerBriefDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .created(CONTAINER_1_CREATED)
            .running(CONTAINER_1_RUNNING)
            .build();

    public final static Long CONTAINER_2_ID = 2L;
    public final static ContainerImage CONTAINER_2_IMAGE = IMAGE_1;
    public final static ImageDto CONTAINER_2_IMAGE_DTO = IMAGE_1_DTO;
    public final static String CONTAINER_2_NAME = "u02";
    public final static String CONTAINER_2_INTERNALNAME = "dbrepo-userdb-u02";
    public final static String CONTAINER_2_IP = "172.30.0.6";
    public final static String CONTAINER_2_HOST = "localhost";
    public final static Integer CONTAINER_2_PORT = 3309;
    public final static String CONTAINER_2_SIDECAR_HOST = "localhost";
    public final static Integer CONTAINER_2_SIDECAR_PORT = 33091;
    public final static Boolean CONTAINER_2_RUNNING = true;
    public final static String CONTAINER_2_PRIVILEGED_USERNAME = "root";
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
            .sidecarHost(CONTAINER_2_SIDECAR_HOST)
            .sidecarPort(CONTAINER_2_SIDECAR_PORT)
            .privilegedUsername(CONTAINER_2_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_2_PRIVILEGED_PASSWORD)
            .build();

    public final static ContainerDto CONTAINER_2_DTO = ContainerDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .image(CONTAINER_2_IMAGE_DTO)
            .created(CONTAINER_2_CREATED)
            .host(CONTAINER_2_HOST)
            .port(CONTAINER_2_PORT)
            .sidecarHost(CONTAINER_1_SIDECAR_HOST)
            .sidecarPort(CONTAINER_1_SIDECAR_PORT)
            .build();

    public final static ContainerBriefDto CONTAINER_2_DTO_BRIEF = ContainerBriefDto.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .created(CONTAINER_2_CREATED)
            .running(CONTAINER_2_RUNNING)
            .build();

    public final static Long CONTAINER_3_ID = 3L;
    public final static ContainerImage CONTAINER_3_IMAGE = IMAGE_1;
    public final static String CONTAINER_3_NAME = "u03";
    public final static String CONTAINER_3_INTERNALNAME = "dbrepo-userdb-u03";
    public final static String CONTAINER_3_IP = "172.30.0.7";
    public final static String CONTAINER_3_HOST = "localhost";
    public final static Integer CONTAINER_3_PORT = 3310;
    public final static String CONTAINER_3_SIDECAR_HOST = "localhost";
    public final static Integer CONTAINER_3_SIDECAR_PORT = 33101;
    public final static String CONTAINER_3_PRIVILEGED_USERNAME = "root";
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
            .sidecarHost(CONTAINER_3_SIDECAR_HOST)
            .sidecarPort(CONTAINER_3_SIDECAR_PORT)
            .privilegedUsername(CONTAINER_3_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_3_PRIVILEGED_PASSWORD)
            .build();

    public final static Long CONTAINER_4_ID = 4L;
    public final static ContainerImage CONTAINER_4_IMAGE = IMAGE_1;
    public final static String CONTAINER_4_NAME = "u04";
    public final static String CONTAINER_4_INTERNALNAME = "dbrepo-userdb-u04";
    public final static String CONTAINER_4_IP = "172.30.0.8";
    public final static String CONTAINER_4_HOST = "localhost";
    public final static Integer CONTAINER_4_PORT = 3311;
    public final static String CONTAINER_4_SIDECAR_HOST = "localhost";
    public final static Integer CONTAINER_4_SIDECAR_PORT = 33111;
    public final static String CONTAINER_4_PRIVILEGED_USERNAME = "root";
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
            .sidecarHost(CONTAINER_4_SIDECAR_HOST)
            .sidecarPort(CONTAINER_4_SIDECAR_PORT)
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
    public final static String DATABASE_1_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_1_CREATED = Instant.ofEpochSecond(1677399741L) /* 2023-02-26 08:22:21 (UTC) */;
    public final static Instant DATABASE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399741L) /* 2023-02-26 08:22:21 (UTC) */;
    public final static UUID DATABASE_1_OWNER = USER_1_ID;
    public final static UUID DATABASE_1_CREATOR = USER_1_ID;

    public final static GrantExchangePermissionsDto USER_1_RABBITMQ_GRANT_TOPIC_DTO = GrantExchangePermissionsDto.builder()
            .exchange("dbrepo")
            .read("^(dbrepo\\." + DATABASE_1_INTERNALNAME + "\\..)$")
            .write("^(dbrepo\\." + DATABASE_1_INTERNALNAME + "\\..)$")
            .build();

    public final static DatabaseCreateDto DATABASE_1_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_1_NAME)
            .isPublic(DATABASE_1_PUBLIC)
            .cid(CONTAINER_1_ID)
            .build();

    public final static Long DATABASE_2_ID = 2L;
    public final static String DATABASE_2_NAME = "Zoo";
    public final static String DATABASE_2_DESCRIPTION = "Zoo data";
    public final static String DATABASE_2_INTERNALNAME = "zoo";
    public final static Boolean DATABASE_2_PUBLIC = false;
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
    public final static String DATABASE_3_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_3_CREATED = Instant.ofEpochSecond(1677399792L) /* 2023-02-26 08:23:12 (UTC) */;
    public final static Instant DATABASE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677399792L) /* 2023-02-26 08:23:12 (UTC) */;
    public final static UUID DATABASE_3_OWNER = USER_3_ID;
    public final static UUID DATABASE_3_CREATOR = USER_3_ID;

    public final static DatabaseDto DATABASE_3_DTO = DatabaseDto.builder()
            .id(DATABASE_3_ID)
            .created(DATABASE_3_CREATED)
            .isPublic(DATABASE_3_PUBLIC)
            .name(DATABASE_3_NAME)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .tables(List.of()) /* TABLE_3, TABLE_3, TABLE_3 */
            .views(List.of())
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
    public final static String DATABASE_4_INTERNALNAME = "weather_at";
    public final static String DATABASE_4_EXCHANGE = "dbrepo";
    public final static Instant DATABASE_4_CREATED = Instant.ofEpochSecond(1677399813L) /* 2023-02-26 08:23:33 (UTC) */;
    public final static Instant DATABASE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677399813L) /* 2023-02-26 08:23:33 (UTC) */;
    public final static UUID DATABASE_4_OWNER = USER_4_ID;
    public final static UUID DATABASE_4_CREATOR = USER_4_ID;

    public final static DatabaseDto DATABASE_4_DTO = DatabaseDto.builder()
            .id(DATABASE_4_ID)
            .created(Instant.now().minus(4, HOURS))
            .isPublic(DATABASE_4_PUBLIC)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .created(DATABASE_4_CREATED)
            .creator(USER_4_DTO)
            .owner(USER_4_DTO)
            .tables(List.of())
            .views(List.of())
            .build();

    public final static TableCreateDto TABLE_0_CREATE_DTO = TableCreateDto.builder()
            .name("full")
            .description("full example")
            .constraints(ConstraintsCreateDto.builder()
                    .uniques(List.of())
                    .foreignKeys(List.of())
                    .build())
            .columns(List.of(ColumnCreateDto.builder()
                            .name("col1a")
                            .type(ColumnTypeDto.CHAR)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col1b")
                            .type(ColumnTypeDto.CHAR)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .size(50L)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col2a")
                            .type(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col2b")
                            .type(ColumnTypeDto.VARCHAR)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .size(1024L)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col3")
                            .type(ColumnTypeDto.BINARY)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col4")
                            .type(ColumnTypeDto.VARBINARY)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .size(200L)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col5")
                            .type(ColumnTypeDto.TINYBLOB)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col6")
                            .type(ColumnTypeDto.TINYTEXT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col7")
                            .type(ColumnTypeDto.TEXT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col8")
                            .type(ColumnTypeDto.BLOB)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col9")
                            .type(ColumnTypeDto.MEDIUMTEXT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col10")
                            .type(ColumnTypeDto.MEDIUMBLOB)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col11")
                            .type(ColumnTypeDto.LONGTEXT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col12")
                            .type(ColumnTypeDto.LONGBLOB)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col13")
                            .type(ColumnTypeDto.ENUM)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .enums(List.of("val1", "val2"))
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col14")
                            .type(ColumnTypeDto.SET)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .sets(List.of("val1", "val2"))
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col15")
                            .type(ColumnTypeDto.BIT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col16")
                            .type(ColumnTypeDto.TINYINT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col17")
                            .type(ColumnTypeDto.BOOL)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col18")
                            .type(ColumnTypeDto.SMALLINT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col19")
                            .type(ColumnTypeDto.MEDIUMINT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col20")
                            .type(ColumnTypeDto.INT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col21")
                            .type(ColumnTypeDto.BIGINT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col22")
                            .type(ColumnTypeDto.FLOAT)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col23")
                            .type(ColumnTypeDto.DOUBLE)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col24")
                            .type(ColumnTypeDto.DECIMAL)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col25")
                            .type(ColumnTypeDto.DATE)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .dfid(IMAGE_DATE_1_ID)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col26")
                            .type(ColumnTypeDto.DATETIME)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .dfid(IMAGE_DATE_3_ID)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col27")
                            .type(ColumnTypeDto.TIMESTAMP)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .dfid(IMAGE_DATE_3_ID)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col28")
                            .type(ColumnTypeDto.TIME)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .dfid(IMAGE_DATE_4_ID)
                            .build(),
                    ColumnCreateDto.builder()
                            .name("col29")
                            .type(ColumnTypeDto.YEAR)
                            .nullAllowed(true)
                            .primaryKey(false)
                            .build()))
            .build();

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "Weather AUS";
    public final static String TABLE_1_INTERNALNAME = "weather_aus";
    public final static Boolean TABLE_1_VERSIONED = true;
    public final static Boolean TABLE_1_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_1_DESCRIPTION = "Weather in the world";
    public final static String TABLE_1_QUEUE_NAME = TABLE_1_INTERNALNAME;
    public final static String TABLE_1_ROUTING_KEY = "dbrepo\\." + DATABASE_1_EXCHANGE + "\\." + TABLE_1_QUEUE_NAME;
    public final static Long TABLE_1_DATABASE_ID = DATABASE_1_ID;
    public final static Instant TABLE_1_CREATED = Instant.ofEpochSecond(1677399975L) /* 2023-02-26 08:26:15 (UTC) */;
    public final static Instant TABLE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399975L) /* 2023-02-26 08:26:15 (UTC) */;

    public final static Constraints TABLE_1_CONSTRAINTS = Constraints.builder()
            .foreignKeys(new LinkedList<>())
            .uniques(new LinkedList<>())
            .checks(new LinkedHashSet<>())
            .build();

    public final static Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .tdbid(DATABASE_1_ID)
            .database(null /* DATABASE_1 */)
            .created(TABLE_1_CREATED)
            .internalName(TABLE_1_INTERNALNAME)
            .isVersioned(TABLE_1_VERSIONED)
            .processedConstraints(TABLE_1_PROCESSED_CONSTRAINTS)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .identifiers(List.of())
            .columns(List.of() /* TABLE_1_COLUMNS */)
            .constraints(TABLE_1_CONSTRAINTS)
            .createdBy(USER_1_ID)
            .creator(USER_1)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .build();

    public final static TableDto TABLE_1_DTO = TableDto.builder()
            .id(TABLE_1_ID)
            .tdbid(DATABASE_1_ID)
            .created(TABLE_1_CREATED)
            .internalName(TABLE_1_INTERNALNAME)
            .isVersioned(TABLE_1_VERSIONED)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .identifiers(List.of())
            .columns(List.of() /* TABLE_1_COLUMNS */)
            .constraints(null /* TABLE_1_CONSTRAINTS */)
            .createdBy(USER_1_ID)
            .owner(USER_1_DTO)
            .build();

    public final static TableBriefDto TABLE_1_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_1_ID)
            .internalName(TABLE_1_INTERNALNAME)
            .isVersioned(TABLE_1_VERSIONED)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .columns(List.of() /* TABLE_1_COLUMNS */)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final static Long TABLE_2_ID = 2L;
    public final static String TABLE_2_NAME = "Weather Location";
    public final static String TABLE_2_INTERNALNAME = "weather_location";
    public final static Boolean TABLE_2_VERSIONED = true;
    public final static Boolean TABLE_2_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_2_DESCRIPTION = "Weather location";
    public final static String TABLE_2_QUEUE_NAME = TABLE_2_INTERNALNAME;
    public final static String TABLE_2_ROUTING_KEY = "dbrepo\\." + DATABASE_1_EXCHANGE + "\\." + TABLE_2_QUEUE_NAME;
    public final static Instant TABLE_2_CREATED = Instant.ofEpochSecond(1677400007L) /* 2023-02-26 08:26:47 (UTC) */;
    public final static Instant TABLE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677400007L) /* 2023-02-26 08:26:47 (UTC) */;

    public final static Constraints TABLE_2_CONSTRAINTS = Constraints.builder()
            .uniques(new LinkedList<>())
            .foreignKeys(new LinkedList<>())
            .checks(new LinkedHashSet<>())
            .build();

    public final static Table TABLE_2 = Table.builder()
            .id(TABLE_2_ID)
            .tdbid(DATABASE_1_ID)
            .database(null /* DATABASE_1 */)
            .created(TABLE_2_CREATED)
            .internalName(TABLE_2_INTERNALNAME)
            .isVersioned(TABLE_2_VERSIONED)
            .processedConstraints(TABLE_2_PROCESSED_CONSTRAINTS)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .columns(List.of() /* TABLE_2_COLUMNS */)
            .constraints(TABLE_2_CONSTRAINTS)
            .createdBy(USER_2_ID)
            .ownedBy(USER_2_ID)
            .owner(USER_2)
            .build();

    public final static TableDto TABLE_2_DTO = TableDto.builder()
            .id(TABLE_2_ID)
            .tdbid(DATABASE_1_ID)
            .created(TABLE_2_CREATED)
            .internalName(TABLE_2_INTERNALNAME)
            .isVersioned(TABLE_2_VERSIONED)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .columns(List.of() /* TABLE_2_COLUMNS */)
            .constraints(null /* TABLE_2_CONSTRAINTS */)
            .createdBy(USER_2_ID)
            .owner(USER_2_DTO)
            .build();

    public final static TableBriefDto TABLE_2_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_2_ID)
            .internalName(TABLE_2_INTERNALNAME)
            .isVersioned(TABLE_2_VERSIONED)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .columns(List.of() /* TABLE_2_COLUMNS */)
            .owner(USER_2_BRIEF_DTO)
            .build();

    public final static Long TABLE_3_ID = 3L;
    public final static String TABLE_3_NAME = "Sensor";
    public final static String TABLE_3_INTERNALNAME = "sensor";
    public final static Boolean TABLE_3_VERSIONED = true;
    public final static Boolean TABLE_3_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_3_DESCRIPTION = "Some sensor data";
    public final static String TABLE_3_QUEUE_NAME = TABLE_3_INTERNALNAME;
    public final static String TABLE_3_ROUTING_KEY = "dbrepo\\." + DATABASE_1_EXCHANGE + "\\." + TABLE_3_QUEUE_NAME;
    public final static Instant TABLE_3_CREATED = Instant.ofEpochSecond(1677400031L) /* 2023-02-26 08:27:11 (UTC) */;
    public final static Instant TABLE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677400031L) /* 2023-02-26 08:27:11 (UTC) */;

    public final static Constraints TABLE_3_CONSTRAINTS = Constraints.builder()
            .uniques(new LinkedList<>())
            .foreignKeys(new LinkedList<>())
            .checks(new LinkedHashSet<>())
            .build();

    public final static Table TABLE_3 = Table.builder()
            .id(TABLE_3_ID)
            .tdbid(DATABASE_1_ID)
            .database(null /* DATABASE_1 */)
            .created(TABLE_3_CREATED)
            .internalName(TABLE_3_INTERNALNAME)
            .isVersioned(TABLE_3_VERSIONED)
            .processedConstraints(TABLE_3_PROCESSED_CONSTRAINTS)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .queueName(TABLE_3_QUEUE_NAME)
            .routingKey(TABLE_3_ROUTING_KEY)
            .columns(List.of() /* TABLE_3_COLUMNS */)
            .constraints(TABLE_3_CONSTRAINTS)
            .createdBy(USER_3_ID)
            .ownedBy(USER_3_ID)
            .owner(USER_3)
            .build();

    public final static TableDto TABLE_3_DTO = TableDto.builder()
            .id(TABLE_3_ID)
            .tdbid(DATABASE_1_ID)
            .created(TABLE_3_CREATED)
            .internalName(TABLE_3_INTERNALNAME)
            .isVersioned(TABLE_3_VERSIONED)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .queueName(TABLE_3_QUEUE_NAME)
            .routingKey(TABLE_3_ROUTING_KEY)
            .columns(List.of() /* TABLE_3_COLUMNS */)
            .constraints(null /* TABLE_3_CONSTRAINTS */)
            .createdBy(USER_3_ID)
            .owner(USER_3_DTO)
            .build();

    public final static TableBriefDto TABLE_3_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_3_ID)
            .internalName(TABLE_3_INTERNALNAME)
            .isVersioned(TABLE_3_VERSIONED)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .columns(List.of() /* TABLE_3_COLUMNS */)
            .owner(USER_3_BRIEF_DTO)
            .build();

    public final static ConstraintsCreateDto TABLE_3_CONSTRAINTS_CREATE_DTO = ConstraintsCreateDto.builder()
            .uniques(List.of(List.of("id")))
            .build();

    public final static ConstraintsCreateDto TABLE_3_CONSTRAINTS_INVALID_CREATE_DTO = ConstraintsCreateDto.builder()
            .uniques(List.of(List.of("id")))
            .foreignKeys(List.of(ForeignKeyCreateDto.builder()
                    .referencedTable("weather_location")
                    .columns(List.of("fahrzeug"))
                    .referencedColumns(List.of("doesnotexist")).build()))
            .build();

    public final static TableCreateDto TABLE_3_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(List.of())
            .constraints(TABLE_3_CONSTRAINTS_CREATE_DTO)
            .build();

    public final static TableCreateDto TABLE_3_INVALID_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(List.of())
            .constraints(TABLE_3_CONSTRAINTS_INVALID_CREATE_DTO)
            .build();

    public final static Long TABLE_5_ID = 5L;
    public final static String TABLE_5_NAME = "zoo";
    public final static String TABLE_5_INTERNALNAME = "zoo";
    public final static Boolean TABLE_5_VERSIONED = true;
    public final static Boolean TABLE_5_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_5_DESCRIPTION = "Some Kaggle dataset";
    public final static String TABLE_5_QUEUE_NAME = TABLE_5_INTERNALNAME;
    public final static String TABLE_5_ROUTING_KEY = "dbrepo\\." + DATABASE_2_EXCHANGE + "\\." + TABLE_5_QUEUE_NAME;
    public final static Instant TABLE_5_CREATED = Instant.ofEpochSecond(1677400067L) /* 2023-02-26 08:27:47 (UTC) */;
    public final static Instant TABLE_5_LAST_MODIFIED = Instant.ofEpochSecond(1677400067L) /* 2023-02-26 08:27:47 (UTC) */;

    public final static Table TABLE_5 = Table.builder()
            .id(TABLE_5_ID)
            .tdbid(DATABASE_2_ID)
            .created(Instant.now())
            .internalName(TABLE_5_INTERNALNAME)
            .isVersioned(TABLE_5_VERSIONED)
            .processedConstraints(TABLE_5_PROCESSED_CONSTRAINTS)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .queueName(TABLE_5_QUEUE_NAME)
            .routingKey(TABLE_5_ROUTING_KEY)
            .columns(List.of() /* needs to be set in the junit tests */)
            .constraints(null) /* TABLE_5_CONSTRAINTS */
            .createdBy(USER_1_ID)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .build();

    public final static TableDto TABLE_5_DTO = TableDto.builder()
            .id(TABLE_5_ID)
            .tdbid(DATABASE_2_ID)
            .created(Instant.now())
            .internalName(TABLE_5_INTERNALNAME)
            .isVersioned(TABLE_5_VERSIONED)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .queueName(TABLE_5_QUEUE_NAME)
            .routingKey(TABLE_5_ROUTING_KEY)
            .columns(List.of() /* needs to be set in the junit tests */)
            .constraints(null) /* TABLE_5_CONSTRAINTS */
            .createdBy(USER_1_ID)
            .owner(USER_1_DTO)
            .build();

    public final static TableCsvDto TABLE_5_CSV_DTO = TableCsvDto.builder()
            .data(new HashMap<>() {{
                put("id", "102");
            }})
            .build();

    public final static Long TABLE_6_ID = 6L;
    public final static String TABLE_6_NAME = "names";
    public final static String TABLE_6_INTERNALNAME = "names";
    public final static Boolean TABLE_6_VERSIONED = true;
    public final static Boolean TABLE_6_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_6_DESCRIPTION = "Some names dataset";
    public final static String TABLE_6_QUEUE_NAME = TABLE_6_INTERNALNAME;
    public final static String TABLE_6_ROUTING_KEY = "dbrepo\\." + DATABASE_2_EXCHANGE + "\\." + TABLE_6_QUEUE_NAME;
    public final static Instant TABLE_6_CREATED = Instant.ofEpochSecond(1677400117L) /* 2023-02-26 08:28:37 (UTC) */;
    public final static Instant TABLE_6_LAST_MODIFIED = Instant.ofEpochSecond(1677400117L) /* 2023-02-26 08:28:37 (UTC) */;

    public final static Table TABLE_6 = Table.builder()
            .id(TABLE_6_ID)
            .tdbid(DATABASE_2_ID)
            .created(TABLE_6_CREATED)
            .internalName(TABLE_6_INTERNALNAME)
            .isVersioned(TABLE_6_VERSIONED)
            .processedConstraints(TABLE_6_PROCESSED_CONSTRAINTS)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .queueName(TABLE_6_QUEUE_NAME)
            .routingKey(TABLE_6_ROUTING_KEY)
            .columns(List.of() /* needs to be set in the junit tests */)
            .constraints(null) /* TABLE_6_CONSTRAINTS */
            .createdBy(USER_1_ID)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_6_CREATED)
            .build();

    public final static TableDto TABLE_6_DTO = TableDto.builder()
            .id(TABLE_6_ID)
            .tdbid(DATABASE_2_ID)
            .created(TABLE_6_CREATED)
            .internalName(TABLE_6_INTERNALNAME)
            .isVersioned(TABLE_6_VERSIONED)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .queueName(TABLE_6_QUEUE_NAME)
            .routingKey(TABLE_6_ROUTING_KEY)
            .columns(List.of() /* needs to be set in the junit tests */)
            .constraints(null) /* TABLE_6_CONSTRAINTS */
            .createdBy(USER_1_ID)
            .owner(USER_1_DTO)
            .created(TABLE_6_CREATED)
            .build();

    public final static Long TABLE_7_ID = 7L;
    public final static String TABLE_7_NAME = "likes";
    public final static String TABLE_7_INTERNAL_NAME = "likes";
    public final static Boolean TABLE_7_VERSIONED = true;
    public final static Boolean TABLE_7_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_7_DESCRIPTION = "Some likes dataset";
    public final static String TABLE_7_QUEUE_NAME = TABLE_7_INTERNAL_NAME;
    public final static String TABLE_7_ROUTING_KEY = "dbrepo\\." + DATABASE_2_EXCHANGE + "\\." + TABLE_7_QUEUE_NAME;
    public final static Instant TABLE_7_CREATED = Instant.ofEpochSecond(1677400147L) /* 2023-02-26 08:29:07 (UTC) */;
    public final static Instant TABLE_7_LAST_MODIFIED = Instant.ofEpochSecond(1677400147L) /* 2023-02-26 08:29:07 (UTC) */;

    public final static Table TABLE_7 = Table.builder()
            .id(TABLE_7_ID)
            .tdbid(DATABASE_2_ID)
            .created(TABLE_7_CREATED)
            .internalName(TABLE_7_INTERNAL_NAME)
            .isVersioned(TABLE_7_VERSIONED)
            .processedConstraints(TABLE_7_PROCESSED_CONSTRAINTS)
            .description(TABLE_7_DESCRIPTION)
            .name(TABLE_7_NAME)
            .lastModified(TABLE_7_LAST_MODIFIED)
            .queueName(TABLE_7_QUEUE_NAME)
            .routingKey(TABLE_7_ROUTING_KEY)
            .columns(List.of() /* TABLE_7_COLUMNS */)
            .constraints(null) /* TABLE_7_CONSTRAINTS */
            .createdBy(USER_1_ID)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_7_CREATED)
            .build();

    public final static TableDto TABLE_7_DTO = TableDto.builder()
            .id(TABLE_7_ID)
            .tdbid(DATABASE_2_ID)
            .created(TABLE_7_CREATED)
            .internalName(TABLE_7_INTERNAL_NAME)
            .isVersioned(TABLE_7_VERSIONED)
            .description(TABLE_7_DESCRIPTION)
            .name(TABLE_7_NAME)
            .queueName(TABLE_7_QUEUE_NAME)
            .routingKey(TABLE_7_ROUTING_KEY)
            .columns(List.of() /* TABLE_7_COLUMNS */)
            .constraints(null) /* TABLE_7_CONSTRAINTS */
            .createdBy(USER_1_ID)
            .owner(USER_1_DTO)
            .created(TABLE_7_CREATED)
            .build();

    public final static Long TABLE_4_ID = 4L;
    public final static String TABLE_4_NAME = "Sensor";
    public final static String TABLE_4_INTERNAL_NAME = "sensor";
    public final static Boolean TABLE_4_VERSIONED = true;
    public final static Boolean TABLE_4_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_4_DESCRIPTION = "Hello sensor";
    public final static String TABLE_4_QUEUE_NAME = TABLE_4_INTERNAL_NAME;
    public final static String TABLE_4_ROUTING_KEY = "dbrepo\\." + DATABASE_1_EXCHANGE + "\\." + TABLE_4_QUEUE_NAME;
    public final static Instant TABLE_4_CREATED = Instant.ofEpochSecond(1677400175L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677400175L) /* 2023-02-26 08:29:35 (UTC) */;

    public final static Constraints TABLE_4_CONSTRAINTS = Constraints.builder()
            .uniques(List.of())
            .foreignKeys(List.of())
            .checks(Set.of())
            .build();

    public final static Table TABLE_4 = Table.builder()
            .id(TABLE_4_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNAL_NAME)
            .description(TABLE_4_DESCRIPTION)
            .database(null /* DATABASE_1 */)
            .name(TABLE_4_NAME)
            .queueName(TABLE_4_QUEUE_NAME)
            .routingKey(TABLE_4_ROUTING_KEY)
            .columns(List.of() /* TABLE_4_COLUMNS */)
            .isVersioned(TABLE_4_VERSIONED)
            .processedConstraints(TABLE_4_PROCESSED_CONSTRAINTS)
            .createdBy(USER_1_ID)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_4_CREATED)
            .constraints(TABLE_4_CONSTRAINTS)
            .lastModified(TABLE_4_LAST_MODIFIED)
            .build();

    public final static TableDto TABLE_4_DTO = TableDto.builder()
            .id(TABLE_4_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_4_INTERNAL_NAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .queueName(TABLE_4_QUEUE_NAME)
            .routingKey(TABLE_4_ROUTING_KEY)
            .columns(List.of() /* TABLE_4_COLUMNS */)
            .isVersioned(TABLE_4_VERSIONED)
            .createdBy(USER_1_ID)
            .owner(USER_1_DTO)
            .created(TABLE_4_CREATED)
            .build();

    public final static TableBriefDto TABLE_4_BRIEF_DTO = TableBriefDto.builder()
            .id(TABLE_4_ID)
            .internalName(TABLE_4_INTERNAL_NAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .columns(List.of() /* TABLE_4_COLUMNS */)
            .isVersioned(TABLE_4_VERSIONED)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final static List<TableColumn> TABLE_4_COLUMNS = List.of(TableColumn.builder()
                    .id(44L)
                    .ordinalPosition(0)
                    .table(TABLE_4)
                    .name("Timestamp")
                    .internalName("timestamp")
                    .columnType(TableColumnType.TIMESTAMP)
                    .isNullAllowed(false)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .build(),
            TableColumn.builder()
                    .id(45L)
                    .ordinalPosition(1)
                    .table(TABLE_4)
                    .name("Value")
                    .internalName("value")
                    .columnType(TableColumnType.DECIMAL)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .build());

    public final static List<ColumnDto> TABLE_4_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(44L)
                    .databaseId(DATABASE_1_ID)
                    .tableId(TABLE_4_ID)
                    .name("Timestamp")
                    .internalName("timestamp")
                    .columnType(ColumnTypeDto.TIMESTAMP)
                    .dateFormat(IMAGE_DATE_3_DTO)
                    .isNullAllowed(false)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .build(),
            ColumnDto.builder()
                    .id(45L)
                    .databaseId(DATABASE_1_ID)
                    .tableId(TABLE_4_ID)
                    .name("Value")
                    .internalName("value")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .dateFormat(null)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .build());

    public final static Long TABLE_8_ID = 8L;
    public final static String TABLE_8_NAME = "mfcc";
    public final static String TABLE_8_INTERNAL_NAME = "mfcc";
    public final static Boolean TABLE_8_VERSIONED = true;
    public final static Boolean TABLE_8_PROCESSED_CONSTRAINTS = true;
    public final static String TABLE_8_DESCRIPTION = "Hello mfcc";
    public final static String TABLE_8_QUEUE_NAME = TABLE_8_INTERNAL_NAME;
    public final static String TABLE_8_ROUTING_KEY = "dbrepo\\." + DATABASE_3_EXCHANGE + "\\." + TABLE_8_QUEUE_NAME;
    public final static Instant TABLE_8_CREATED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_8_LAST_MODIFIED = Instant.ofEpochSecond(1688400185L) /* 2023-02-26 08:29:35 (UTC) */;

    public final static Table TABLE_8 = Table.builder()
            .id(TABLE_8_ID)
            .tdbid(DATABASE_1_ID)
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .isVersioned(TABLE_8_VERSIONED)
            .processedConstraints(TABLE_8_PROCESSED_CONSTRAINTS)
            .database(null /* DATABASE_1 */)
            .name(TABLE_8_NAME)
            .queueName(TABLE_8_QUEUE_NAME)
            .routingKey(TABLE_8_ROUTING_KEY)
            .columns(List.of() /* TABLE_8_COLUMNS */)
            .createdBy(USER_1_ID)
            .ownedBy(USER_1_ID)
            .owner(USER_1)
            .created(TABLE_8_CREATED)
            .lastModified(TABLE_8_LAST_MODIFIED)
            .build();

    public final static TableCsvDto TABLE_8_CSV_DTO = TableCsvDto.builder()
            .data(new HashMap<>() {{
                put("value", "2.1");
            }})
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
    public final static String ONTOLOGY_1_SPARQL_ENDPOINT = null;
    public final static String ONTOLOGY_1_RDF_PATH = "rdf/om-2.0.rdf";
    public final static UUID ONTOLOGY_1_CREATED_BY = USER_1_ID;

    public final static Ontology ONTOLOGY_1 = Ontology.builder()
            .id(ONTOLOGY_1_ID)
            .prefix(ONTOLOGY_1_PREFIX)
            .uri(ONTOLOGY_1_URI)
            .sparqlEndpoint(ONTOLOGY_1_SPARQL_ENDPOINT)
            .rdfPath(ONTOLOGY_1_RDF_PATH)
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

    public final static Long COLUMN_CONCEPT_PRECIPITATION_ID = 1L;
    public final static String COLUMN_CONCEPT_PRECIPITATION_NAME = "precipitation";
    public final static String COLUMN_CONCEPT_PRECIPITATION_URI = "http://www.wikidata.org/entity/Q25257";
    public final static String COLUMN_CONCEPT_PRECIPITATION_DESCRIPTION = null;
    public final static Instant COLUMN_CONCEPT_PRECIPITATION_CREATED = Instant.ofEpochSecond(1701976048L) /* 2023-12-07 19:07:27 */;

    public final static ConceptSaveDto COLUMN_CONCEPT_PRECIPITATION_SAVE_DTO = ConceptSaveDto.builder()
            .uri(COLUMN_CONCEPT_PRECIPITATION_URI)
            .name(COLUMN_CONCEPT_PRECIPITATION_NAME)
            .description(COLUMN_CONCEPT_PRECIPITATION_DESCRIPTION)
            .build();

    public final static ConceptDto COLUMN_CONCEPT_PRECIPITATION_DTO = ConceptDto.builder()
            .id(COLUMN_CONCEPT_PRECIPITATION_ID)
            .uri(COLUMN_CONCEPT_PRECIPITATION_URI)
            .name(COLUMN_CONCEPT_PRECIPITATION_NAME)
            .description(COLUMN_CONCEPT_PRECIPITATION_DESCRIPTION)
            .build();

    public final static TableColumnConcept COLUMN_CONCEPT_PRECIPITATION = TableColumnConcept.builder()
            .id(COLUMN_CONCEPT_PRECIPITATION_ID)
            .uri(COLUMN_CONCEPT_PRECIPITATION_URI)
            .name(COLUMN_CONCEPT_PRECIPITATION_NAME)
            .description(COLUMN_CONCEPT_PRECIPITATION_DESCRIPTION)
            .created(COLUMN_CONCEPT_PRECIPITATION_CREATED)
            .build();

    public final static Long COLUMN_CONCEPT_FAIR_DATA_ID = 2L;
    public final static String COLUMN_CONCEPT_FAIR_DATA_NAME = "FAIR data";
    public final static String COLUMN_CONCEPT_FAIR_DATA_URI = "http://www.wikidata.org/entity/Q29032648";
    public final static String COLUMN_CONCEPT_FAIR_DATA_DESCRIPTION = "data compliant with the terms of the FAIR Data Principles";
    public final static Instant COLUMN_CONCEPT_FAIR_DATA_CREATED = Instant.now();

    public final static ConceptSaveDto COLUMN_CONCEPT_FAIR_DATA_SAVE_DTO = ConceptSaveDto.builder()
            .uri(COLUMN_CONCEPT_FAIR_DATA_URI)
            .name(COLUMN_CONCEPT_FAIR_DATA_NAME)
            .description(COLUMN_CONCEPT_FAIR_DATA_DESCRIPTION)
            .build();

    public final static ConceptDto COLUMN_CONCEPT_FAIR_DATA_DTO = ConceptDto.builder()
            .id(COLUMN_CONCEPT_FAIR_DATA_ID)
            .uri(COLUMN_CONCEPT_FAIR_DATA_URI)
            .name(COLUMN_CONCEPT_FAIR_DATA_NAME)
            .description(COLUMN_CONCEPT_FAIR_DATA_DESCRIPTION)
            .build();

    public final static TableColumnConcept COLUMN_CONCEPT_FAIR_DATA = TableColumnConcept.builder()
            .id(COLUMN_CONCEPT_FAIR_DATA_ID)
            .uri(COLUMN_CONCEPT_FAIR_DATA_URI)
            .name(COLUMN_CONCEPT_FAIR_DATA_NAME)
            .description(COLUMN_CONCEPT_FAIR_DATA_DESCRIPTION)
            .created(COLUMN_CONCEPT_FAIR_DATA_CREATED)
            .build();

    public final static Long UNIT_MILLIMETRE_ID = 1L;
    public final static String UNIT_MILLIMETRE_NAME = "millimetre";
    public final static String UNIT_MILLIMETRE_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/millimetre";
    public final static String UNIT_MILLIMETRE_DESCRIPTION = "The millimetre is a unit of length defined as 1.0e-3 metre.";
    public final static Instant UNIT_MILLIMETRE_CREATED = Instant.ofEpochSecond(1701976282L) /* 2023-12-07 19:11:22 */;

    public final static UnitSaveDto UNIT_MILLIMETRE_SAVE_DTO = UnitSaveDto.builder()
            .uri(UNIT_MILLIMETRE_URI)
            .name(UNIT_MILLIMETRE_NAME)
            .description(UNIT_MILLIMETRE_DESCRIPTION)
            .build();

    public final static UnitDto UNIT_MILLIMETRE_DTO = UnitDto.builder()
            .id(UNIT_MILLIMETRE_ID)
            .uri(UNIT_MILLIMETRE_URI)
            .name(UNIT_MILLIMETRE_NAME)
            .description(UNIT_MILLIMETRE_DESCRIPTION)
            .build();

    public final static TableColumnUnit UNIT_MILLIMETRE = TableColumnUnit.builder()
            .id(UNIT_MILLIMETRE_ID)
            .uri(UNIT_MILLIMETRE_URI)
            .name(UNIT_MILLIMETRE_NAME)
            .description(UNIT_MILLIMETRE_DESCRIPTION)
            .created(UNIT_MILLIMETRE_CREATED)
            .build();

    public final static Long UNIT_TONNE_ID = 2L;
    public final static String UNIT_TONNE_NAME = "tonne";
    public final static String UNIT_TONNE_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/tonne";
    public final static String UNIT_TONNE_DESCRIPTION = "The tonne is a unit of mass defined as 1000 kilogram.";
    public final static Instant UNIT_TONNE_CREATED = Instant.ofEpochSecond(1701976462L) /* 2023-12-07 19:14:22 */;

    public final static UnitSaveDto UNIT_TONNE_SAVE_DTO = UnitSaveDto.builder()
            .uri(UNIT_TONNE_URI)
            .name(UNIT_TONNE_NAME)
            .description(UNIT_TONNE_DESCRIPTION)
            .build();

    public final static UnitDto UNIT_TONNE_DTO = UnitDto.builder()
            .id(UNIT_TONNE_ID)
            .uri(UNIT_TONNE_URI)
            .name(UNIT_TONNE_NAME)
            .description(UNIT_TONNE_DESCRIPTION)
            .build();

    public final static TableColumnUnit UNIT_TONNE = TableColumnUnit.builder()
            .id(UNIT_TONNE_ID)
            .uri(UNIT_TONNE_URI)
            .name(UNIT_TONNE_NAME)
            .description(UNIT_TONNE_DESCRIPTION)
            .created(UNIT_TONNE_CREATED)
            .build();

    public final static Long COLUMN_4_1_ID = 45L;
    public final static Integer COLUMN_4_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_4_1_PRIMARY = true;
    public final static String COLUMN_4_1_NAME = "id";
    public final static String COLUMN_4_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_4_1_TYPE = TableColumnType.BIGINT;
    public final static ColumnTypeDto COLUMN_4_1_TYPE_DTO = ColumnTypeDto.BIGINT;
    public final static Long COLUMN_4_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_1_NULL = false;
    public final static Boolean COLUMN_4_1_AUTO_GENERATED = true;
    public final static String COLUMN_4_1_FOREIGN_KEY = null;
    public final static String COLUMN_4_1_CHECK = null;
    public final static List<String> COLUMN_4_1_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_1_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_1_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_1_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_1_SET_VALUES = null;
    public final static List<String> COLUMN_4_1_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_2_ID = 46L;
    public final static Integer COLUMN_4_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_4_2_PRIMARY = false;
    public final static String COLUMN_4_2_NAME = "Animal Name";
    public final static String COLUMN_4_2_INTERNAL_NAME = "animal_name";
    public final static TableColumnType COLUMN_4_2_TYPE = TableColumnType.VARCHAR;
    public final static ColumnTypeDto COLUMN_4_2_TYPE_DTO = ColumnTypeDto.VARCHAR;
    public final static Long COLUMN_4_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_2_NULL = true;
    public final static Boolean COLUMN_4_2_AUTO_GENERATED = false;
    public final static String COLUMN_4_2_FOREIGN_KEY = null;
    public final static String COLUMN_4_2_CHECK = null;
    public final static List<String> COLUMN_4_2_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_2_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_2_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_2_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_2_SET_VALUES = null;
    public final static List<String> COLUMN_4_2_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_3_ID = 47L;
    public final static Integer COLUMN_4_3_ORDINALPOS = 2;
    public final static Boolean COLUMN_4_3_PRIMARY = false;
    public final static String COLUMN_4_3_NAME = "Hair";
    public final static String COLUMN_4_3_INTERNAL_NAME = "hair";
    public final static TableColumnType COLUMN_4_3_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_3_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_3_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_3_NULL = true;
    public final static Boolean COLUMN_4_3_AUTO_GENERATED = false;
    public final static String COLUMN_4_3_FOREIGN_KEY = null;
    public final static String COLUMN_4_3_CHECK = null;
    public final static List<String> COLUMN_4_3_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_3_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_3_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_3_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_3_SET_VALUES = null;
    public final static List<String> COLUMN_4_3_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_4_ID = 48L;
    public final static Integer COLUMN_4_4_ORDINALPOS = 3;
    public final static Boolean COLUMN_4_4_PRIMARY = false;
    public final static String COLUMN_4_4_NAME = "Feathers";
    public final static String COLUMN_4_4_INTERNAL_NAME = "feathers";
    public final static TableColumnType COLUMN_4_4_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_4_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_4_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_4_NULL = true;
    public final static Boolean COLUMN_4_4_AUTO_GENERATED = false;
    public final static String COLUMN_4_4_FOREIGN_KEY = null;
    public final static String COLUMN_4_4_CHECK = null;
    public final static List<String> COLUMN_4_4_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_4_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_4_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_4_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_4_SET_VALUES = null;
    public final static List<String> COLUMN_4_4_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_5_ID = 49L;
    public final static Integer COLUMN_4_5_ORDINALPOS = 4;
    public final static Boolean COLUMN_4_5_PRIMARY = false;
    public final static String COLUMN_4_5_NAME = "Bread";
    public final static String COLUMN_4_5_INTERNAL_NAME = "bread";
    public final static TableColumnType COLUMN_4_5_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_5_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_5_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_5_NULL = true;
    public final static Boolean COLUMN_4_5_AUTO_GENERATED = false;
    public final static String COLUMN_4_5_FOREIGN_KEY = null;
    public final static String COLUMN_4_5_CHECK = null;
    public final static List<String> COLUMN_4_5_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_5_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_5_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_5_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_5_SET_VALUES = null;
    public final static List<String> COLUMN_4_5_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_6_ID = 50L;
    public final static Integer COLUMN_4_6_ORDINALPOS = 5;
    public final static Boolean COLUMN_4_6_PRIMARY = false;
    public final static String COLUMN_4_6_NAME = "Eggs";
    public final static String COLUMN_4_6_INTERNAL_NAME = "eggs";
    public final static TableColumnType COLUMN_4_6_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_6_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_6_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_6_NULL = true;
    public final static Boolean COLUMN_4_6_AUTO_GENERATED = false;
    public final static String COLUMN_4_6_FOREIGN_KEY = null;
    public final static String COLUMN_4_6_CHECK = null;
    public final static List<String> COLUMN_4_6_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_6_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_6_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_6_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_6_SET_VALUES = null;
    public final static List<String> COLUMN_4_6_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_7_ID = 51L;
    public final static Integer COLUMN_4_7_ORDINALPOS = 6;
    public final static Boolean COLUMN_4_7_PRIMARY = false;
    public final static String COLUMN_4_7_NAME = "Milk";
    public final static String COLUMN_4_7_INTERNAL_NAME = "milk";
    public final static TableColumnType COLUMN_4_7_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_7_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_7_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_7_NULL = true;
    public final static Boolean COLUMN_4_7_AUTO_GENERATED = false;
    public final static String COLUMN_4_7_FOREIGN_KEY = null;
    public final static String COLUMN_4_7_CHECK = null;
    public final static List<String> COLUMN_4_7_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_7_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_7_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_7_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_7_SET_VALUES = null;
    public final static List<String> COLUMN_4_7_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_8_ID = 52L;
    public final static Integer COLUMN_4_8_ORDINALPOS = 7;
    public final static Boolean COLUMN_4_8_PRIMARY = false;
    public final static String COLUMN_4_8_NAME = "Water";
    public final static String COLUMN_4_8_INTERNAL_NAME = "water";
    public final static TableColumnType COLUMN_4_8_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_8_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_8_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_8_NULL = true;
    public final static Boolean COLUMN_4_8_AUTO_GENERATED = false;
    public final static String COLUMN_4_8_FOREIGN_KEY = null;
    public final static String COLUMN_4_8_CHECK = null;
    public final static List<String> COLUMN_4_8_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_8_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_8_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_8_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_8_SET_VALUES = null;
    public final static List<String> COLUMN_4_8_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_9_ID = 53L;
    public final static Integer COLUMN_4_9_ORDINALPOS = 8;
    public final static Boolean COLUMN_4_9_PRIMARY = false;
    public final static String COLUMN_4_9_NAME = "Airborne";
    public final static String COLUMN_4_9_INTERNAL_NAME = "airborne";
    public final static TableColumnType COLUMN_4_9_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_9_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_9_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_9_NULL = true;
    public final static Boolean COLUMN_4_9_AUTO_GENERATED = false;
    public final static String COLUMN_4_9_FOREIGN_KEY = null;
    public final static String COLUMN_4_9_CHECK = null;
    public final static List<String> COLUMN_4_9_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_9_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_9_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_9_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_9_SET_VALUES = null;
    public final static List<String> COLUMN_4_9_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_10_ID = 54L;
    public final static Integer COLUMN_4_10_ORDINALPOS = 9;
    public final static Boolean COLUMN_4_10_PRIMARY = false;
    public final static String COLUMN_4_10_NAME = "Waterborne";
    public final static String COLUMN_4_10_INTERNAL_NAME = "waterborne";
    public final static TableColumnType COLUMN_4_10_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_10_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_10_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_10_NULL = true;
    public final static Boolean COLUMN_4_10_AUTO_GENERATED = false;
    public final static String COLUMN_4_10_FOREIGN_KEY = null;
    public final static String COLUMN_4_10_CHECK = null;
    public final static List<String> COLUMN_4_10_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_10_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_10_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_10_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_10_SET_VALUES = null;
    public final static List<String> COLUMN_4_10_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_11_ID = 55L;
    public final static Integer COLUMN_4_11_ORDINALPOS = 10;
    public final static Boolean COLUMN_4_11_PRIMARY = false;
    public final static String COLUMN_4_11_NAME = "Aquantic";
    public final static String COLUMN_4_11_INTERNAL_NAME = "aquatic";
    public final static TableColumnType COLUMN_4_11_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_11_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_11_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_11_NULL = true;
    public final static Boolean COLUMN_4_11_AUTO_GENERATED = false;
    public final static String COLUMN_4_11_FOREIGN_KEY = null;
    public final static String COLUMN_4_11_CHECK = null;
    public final static List<String> COLUMN_4_11_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_11_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_11_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_11_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_11_SET_VALUES = null;
    public final static List<String> COLUMN_4_11_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_12_ID = 56L;
    public final static Integer COLUMN_4_12_ORDINALPOS = 11;
    public final static Boolean COLUMN_4_12_PRIMARY = false;
    public final static String COLUMN_4_12_NAME = "Predator";
    public final static String COLUMN_4_12_INTERNAL_NAME = "predator";
    public final static TableColumnType COLUMN_4_12_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_12_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_12_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_12_NULL = true;
    public final static Boolean COLUMN_4_12_AUTO_GENERATED = false;
    public final static String COLUMN_4_12_FOREIGN_KEY = null;
    public final static String COLUMN_4_12_CHECK = null;
    public final static List<String> COLUMN_4_12_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_12_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_12_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_12_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_12_SET_VALUES = null;
    public final static List<String> COLUMN_4_12_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_13_ID = 57L;
    public final static Integer COLUMN_4_13_ORDINALPOS = 12;
    public final static Boolean COLUMN_4_13_PRIMARY = false;
    public final static String COLUMN_4_13_NAME = "Backbone";
    public final static String COLUMN_4_13_INTERNAL_NAME = "backbone";
    public final static TableColumnType COLUMN_4_13_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_13_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_13_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_13_NULL = true;
    public final static Boolean COLUMN_4_13_AUTO_GENERATED = false;
    public final static String COLUMN_4_13_FOREIGN_KEY = null;
    public final static String COLUMN_4_13_CHECK = null;
    public final static List<String> COLUMN_4_13_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_13_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_13_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_13_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_13_SET_VALUES = null;
    public final static List<String> COLUMN_4_13_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_14_ID = 58L;
    public final static Integer COLUMN_4_14_ORDINALPOS = 13;
    public final static Boolean COLUMN_4_14_PRIMARY = false;
    public final static String COLUMN_4_14_NAME = "Breathes";
    public final static String COLUMN_4_14_INTERNAL_NAME = "breathes";
    public final static TableColumnType COLUMN_4_14_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_14_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_14_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_14_NULL = true;
    public final static Boolean COLUMN_4_14_AUTO_GENERATED = false;
    public final static String COLUMN_4_14_FOREIGN_KEY = null;
    public final static String COLUMN_4_14_CHECK = null;
    public final static List<String> COLUMN_4_14_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_14_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_14_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_14_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_14_SET_VALUES = null;
    public final static List<String> COLUMN_4_14_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_15_ID = 59L;
    public final static Integer COLUMN_4_15_ORDINALPOS = 14;
    public final static Boolean COLUMN_4_15_PRIMARY = false;
    public final static String COLUMN_4_15_NAME = "Venomous";
    public final static String COLUMN_4_15_INTERNAL_NAME = "venomous";
    public final static TableColumnType COLUMN_4_15_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_15_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_15_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_15_NULL = true;
    public final static Boolean COLUMN_4_15_AUTO_GENERATED = false;
    public final static String COLUMN_4_15_FOREIGN_KEY = null;
    public final static String COLUMN_4_15_CHECK = null;
    public final static List<String> COLUMN_4_15_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_15_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_15_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_15_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_15_SET_VALUES = null;
    public final static List<String> COLUMN_4_15_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_16_ID = 60L;
    public final static Integer COLUMN_4_16_ORDINALPOS = 15;
    public final static Boolean COLUMN_4_16_PRIMARY = false;
    public final static String COLUMN_4_16_NAME = "Fin";
    public final static String COLUMN_4_16_INTERNAL_NAME = "fins";
    public final static TableColumnType COLUMN_4_16_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_16_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_16_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_16_NULL = true;
    public final static Boolean COLUMN_4_16_AUTO_GENERATED = false;
    public final static String COLUMN_4_16_FOREIGN_KEY = null;
    public final static String COLUMN_4_16_CHECK = null;
    public final static List<String> COLUMN_4_16_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_16_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_16_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_16_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_16_SET_VALUES = null;
    public final static List<String> COLUMN_4_16_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_17_ID = 61L;
    public final static Integer COLUMN_4_17_ORDINALPOS = 16;
    public final static Boolean COLUMN_4_17_PRIMARY = false;
    public final static String COLUMN_4_17_NAME = "Legs";
    public final static String COLUMN_4_17_INTERNAL_NAME = "legs";
    public final static TableColumnType COLUMN_4_17_TYPE = TableColumnType.INT;
    public final static ColumnTypeDto COLUMN_4_17_TYPE_DTO = ColumnTypeDto.INT;
    public final static Long COLUMN_4_17_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_17_NULL = true;
    public final static Boolean COLUMN_4_17_AUTO_GENERATED = false;
    public final static String COLUMN_4_17_FOREIGN_KEY = null;
    public final static String COLUMN_4_17_CHECK = null;
    public final static List<String> COLUMN_4_17_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_17_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_17_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_17_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_17_SET_VALUES = null;
    public final static List<String> COLUMN_4_17_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_18_ID = 62L;
    public final static Integer COLUMN_4_18_ORDINALPOS = 17;
    public final static Boolean COLUMN_4_18_PRIMARY = false;
    public final static String COLUMN_4_18_NAME = "Tail";
    public final static String COLUMN_4_18_INTERNAL_NAME = "tail";
    public final static TableColumnType COLUMN_4_18_TYPE = TableColumnType.DECIMAL;
    public final static ColumnTypeDto COLUMN_4_18_TYPE_DTO = ColumnTypeDto.DECIMAL;
    public final static Long COLUMN_4_18_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_18_NULL = true;
    public final static Boolean COLUMN_4_18_AUTO_GENERATED = false;
    public final static String COLUMN_4_18_FOREIGN_KEY = null;
    public final static String COLUMN_4_18_CHECK = null;
    public final static List<String> COLUMN_4_18_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_18_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_18_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_18_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_18_SET_VALUES = null;
    public final static List<String> COLUMN_4_18_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_19_ID = 63L;
    public final static Integer COLUMN_4_19_ORDINALPOS = 18;
    public final static Boolean COLUMN_4_19_PRIMARY = false;
    public final static String COLUMN_4_19_NAME = "Domestic";
    public final static String COLUMN_4_19_INTERNAL_NAME = "domestic";
    public final static TableColumnType COLUMN_4_19_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_19_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_19_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_19_NULL = true;
    public final static Boolean COLUMN_4_19_AUTO_GENERATED = false;
    public final static String COLUMN_4_19_FOREIGN_KEY = null;
    public final static String COLUMN_4_19_CHECK = null;
    public final static List<String> COLUMN_4_19_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_19_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_19_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_19_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_19_SET_VALUES = null;
    public final static List<String> COLUMN_4_19_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_20_ID = 64L;
    public final static Integer COLUMN_4_20_ORDINALPOS = 19;
    public final static Boolean COLUMN_4_20_PRIMARY = false;
    public final static String COLUMN_4_20_NAME = "Cat Size";
    public final static String COLUMN_4_20_INTERNAL_NAME = "catsize";
    public final static TableColumnType COLUMN_4_20_TYPE = TableColumnType.BOOL;
    public final static ColumnTypeDto COLUMN_4_20_TYPE_DTO = ColumnTypeDto.BOOL;
    public final static Long COLUMN_4_20_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_20_NULL = true;
    public final static Boolean COLUMN_4_20_AUTO_GENERATED = false;
    public final static String COLUMN_4_20_FOREIGN_KEY = null;
    public final static String COLUMN_4_20_CHECK = null;
    public final static List<String> COLUMN_4_20_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_20_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_20_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_20_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_20_SET_VALUES = null;
    public final static List<String> COLUMN_4_20_SET_VALUES_DTO = null;

    public final static Long COLUMN_4_21_ID = 65L;
    public final static Integer COLUMN_4_21_ORDINALPOS = 20;
    public final static Boolean COLUMN_4_21_PRIMARY = false;
    public final static String COLUMN_4_21_NAME = "Class Type";
    public final static String COLUMN_4_21_INTERNAL_NAME = "class_type";
    public final static TableColumnType COLUMN_4_21_TYPE = TableColumnType.DECIMAL;
    public final static ColumnTypeDto COLUMN_4_21_TYPE_DTO = ColumnTypeDto.DECIMAL;
    public final static Long COLUMN_4_21_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_21_NULL = true;
    public final static Boolean COLUMN_4_21_AUTO_GENERATED = false;
    public final static String COLUMN_4_21_FOREIGN_KEY = null;
    public final static String COLUMN_4_21_CHECK = null;
    public final static List<String> COLUMN_4_21_ENUM_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_21_SET_VALUES_ARR = List.of();
    public final static List<String> COLUMN_4_21_ENUM_VALUES = null;
    public final static List<String> COLUMN_4_21_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_4_21_SET_VALUES = null;
    public final static List<String> COLUMN_4_21_SET_VALUES_DTO = null;

    public final static Long COLUMN_5_1_ID = 66L;
    public final static Integer COLUMN_5_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_5_1_PRIMARY = true;
    public final static String COLUMN_5_1_NAME = "id";
    public final static String COLUMN_5_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_5_1_TYPE = TableColumnType.BIGINT;
    public final static ColumnTypeDto COLUMN_5_1_TYPE_DTO = ColumnTypeDto.BIGINT;
    public final static Long COLUMN_5_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_1_NULL = false;
    public final static Boolean COLUMN_5_1_AUTO_GENERATED = true;
    public final static String COLUMN_5_1_FOREIGN_KEY = null;
    public final static String COLUMN_5_1_CHECK = null;
    public final static List<String> COLUMN_5_1_ENUM_VALUES = null;
    public final static List<String> COLUMN_5_1_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_5_1_SET_VALUES = null;
    public final static List<String> COLUMN_5_1_SET_VALUES_DTO = null;

    public final static Long COLUMN_5_2_ID = 67L;
    public final static Integer COLUMN_5_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_5_2_PRIMARY = false;
    public final static String COLUMN_5_2_NAME = "firstname";
    public final static String COLUMN_5_2_INTERNAL_NAME = "firstname";
    public final static TableColumnType COLUMN_5_2_TYPE = TableColumnType.VARCHAR;
    public final static ColumnTypeDto COLUMN_5_2_TYPE_DTO = ColumnTypeDto.VARCHAR;
    public final static Long COLUMN_5_2_SIZE = 20L;
    public final static Long COLUMN_5_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_2_NULL = false;
    public final static Boolean COLUMN_5_2_AUTO_GENERATED = false;
    public final static String COLUMN_5_2_FOREIGN_KEY = null;
    public final static String COLUMN_5_2_CHECK = null;
    public final static List<String> COLUMN_5_2_ENUM_VALUES = null;
    public final static List<String> COLUMN_5_2_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_5_2_SET_VALUES = null;
    public final static List<String> COLUMN_5_2_SET_VALUES_DTO = null;

    public final static Long COLUMN_5_3_ID = 68L;
    public final static Integer COLUMN_5_3_ORDINALPOS = 2;
    public final static Boolean COLUMN_5_3_PRIMARY = false;
    public final static String COLUMN_5_3_NAME = "lastname";
    public final static String COLUMN_5_3_INTERNAL_NAME = "lastname";
    public final static TableColumnType COLUMN_5_3_TYPE = TableColumnType.VARCHAR;
    public final static ColumnTypeDto COLUMN_5_3_TYPE_DTO = ColumnTypeDto.VARCHAR;
    public final static Long COLUMN_5_3_SIZE = 40L;
    public final static Long COLUMN_5_3_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_3_NULL = false;
    public final static Boolean COLUMN_5_3_AUTO_GENERATED = false;
    public final static String COLUMN_5_3_FOREIGN_KEY = null;
    public final static String COLUMN_5_3_CHECK = null;
    public final static List<String> COLUMN_5_3_ENUM_VALUES = null;
    public final static List<String> COLUMN_5_3_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_5_3_SET_VALUES = null;
    public final static List<String> COLUMN_5_3_SET_VALUES_DTO = null;

    public final static Long COLUMN_5_4_ID = 69L;
    public final static Integer COLUMN_5_4_ORDINALPOS = 3;
    public final static Boolean COLUMN_5_4_PRIMARY = false;
    public final static String COLUMN_5_4_NAME = "birth";
    public final static String COLUMN_5_4_INTERNAL_NAME = "birth";
    public final static TableColumnType COLUMN_5_4_TYPE = TableColumnType.YEAR;
    public final static ColumnTypeDto COLUMN_5_4_TYPE_DTO = ColumnTypeDto.YEAR;
    public final static Long COLUMN_5_4_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_4_NULL = true;
    public final static Boolean COLUMN_5_4_AUTO_GENERATED = false;
    public final static String COLUMN_5_4_FOREIGN_KEY = null;
    public final static String COLUMN_5_4_CHECK = null;
    public final static List<String> COLUMN_5_4_ENUM_VALUES = null;
    public final static List<String> COLUMN_5_4_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_5_4_SET_VALUES = null;
    public final static List<String> COLUMN_5_4_SET_VALUES_DTO = null;

    public final static Long COLUMN_5_5_ID = 70L;
    public final static Integer COLUMN_5_5_ORDINALPOS = 4;
    public final static Boolean COLUMN_5_5_PRIMARY = false;
    public final static String COLUMN_5_5_NAME = "reminder";
    public final static String COLUMN_5_5_INTERNAL_NAME = "reminder";
    public final static TableColumnType COLUMN_5_5_TYPE = TableColumnType.TIME;
    public final static ColumnTypeDto COLUMN_5_5_TYPE_DTO = ColumnTypeDto.TIME;
    public final static Long COLUMN_5_5_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_5_NULL = true;
    public final static Boolean COLUMN_5_5_AUTO_GENERATED = false;
    public final static String COLUMN_5_5_FOREIGN_KEY = null;
    public final static String COLUMN_5_5_CHECK = null;
    public final static List<String> COLUMN_5_5_ENUM_VALUES = null;
    public final static List<String> COLUMN_5_5_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_5_5_SET_VALUES = null;
    public final static List<String> COLUMN_5_5_SET_VALUES_DTO = null;

    public final static Long COLUMN_5_6_ID = 71L;
    public final static Integer COLUMN_5_6_ORDINALPOS = 5;
    public final static Boolean COLUMN_5_6_PRIMARY = false;
    public final static String COLUMN_5_6_NAME = "ref_id";
    public final static String COLUMN_5_6_INTERNAL_NAME = "ref_id";
    public final static TableColumnType COLUMN_5_6_TYPE = TableColumnType.BIGINT;
    public final static ColumnTypeDto COLUMN_5_6_TYPE_DTO = ColumnTypeDto.BIGINT;
    public final static Long COLUMN_5_6_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_6_NULL = true;
    public final static Boolean COLUMN_5_6_AUTO_GENERATED = false;
    public final static String COLUMN_5_6_FOREIGN_KEY = null;
    public final static String COLUMN_5_6_CHECK = null;
    public final static List<String> COLUMN_5_6_ENUM_VALUES = null;
    public final static List<String> COLUMN_5_6_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_5_6_SET_VALUES = null;
    public final static List<String> COLUMN_5_6_SET_VALUES_DTO = null;

    public final static Long COLUMN_8_1_ID = 72L;
    public final static Integer COLUMN_8_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_8_1_PRIMARY = true;
    public final static String COLUMN_8_1_NAME = "ID";
    public final static String COLUMN_8_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_8_1_TYPE = TableColumnType.BIGINT;
    public final static ColumnTypeDto COLUMN_8_1_TYPE_DTO = ColumnTypeDto.BIGINT;
    public final static Long COLUMN_8_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_8_1_NULL = false;
    public final static Boolean COLUMN_8_1_AUTO_GENERATED = true;
    public final static String COLUMN_8_1_FOREIGN_KEY = null;
    public final static String COLUMN_8_1_CHECK = null;
    public final static List<String> COLUMN_8_1_ENUM_VALUES = null;
    public final static List<String> COLUMN_8_1_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_8_1_SET_VALUES = null;
    public final static List<String> COLUMN_8_1_SET_VALUES_DTO = null;

    public final static Long COLUMN_8_2_ID = 73L;
    public final static Integer COLUMN_8_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_8_2_PRIMARY = true;
    public final static String COLUMN_8_2_NAME = "Value";
    public final static String COLUMN_8_2_INTERNAL_NAME = "value";
    public final static TableColumnType COLUMN_8_2_TYPE = TableColumnType.INT;
    public final static ColumnTypeDto COLUMN_8_2_TYPE_DTO = ColumnTypeDto.INT;
    public final static Long COLUMN_8_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_8_2_NULL = true;
    public final static Boolean COLUMN_8_2_AUTO_GENERATED = false;
    public final static String COLUMN_8_2_FOREIGN_KEY = null;
    public final static String COLUMN_8_2_CHECK = null;
    public final static List<String> COLUMN_8_2_ENUM_VALUES = null;
    public final static List<String> COLUMN_8_2_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_8_2_SET_VALUES = null;
    public final static List<String> COLUMN_8_2_SET_VALUES_DTO = null;

    public final static List<TableColumn> TABLE_8_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_8_1_ID)
                    .ordinalPosition(COLUMN_8_1_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_1_NAME)
                    .internalName(COLUMN_8_1_INTERNAL_NAME)
                    .columnType(COLUMN_8_1_TYPE)
                    .isNullAllowed(COLUMN_8_1_NULL)
                    .autoGenerated(COLUMN_8_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_8_1_PRIMARY)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_8_2_ID)
                    .ordinalPosition(COLUMN_8_2_ORDINALPOS)
                    .table(TABLE_8)
                    .name(COLUMN_8_2_NAME)
                    .internalName(COLUMN_8_2_INTERNAL_NAME)
                    .columnType(COLUMN_8_2_TYPE)
                    .isNullAllowed(COLUMN_8_2_NULL)
                    .autoGenerated(COLUMN_8_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_8_2_PRIMARY)
                    .build());

    public final static Long QUERY_1_ID = 1L;
    public final static String QUERY_1_STATEMENT = "SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM " +
            "`weather_aus`";
    public final static String QUERY_1_DOI = null;
    public final static Long QUERY_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long QUERY_1_DATABASE_ID = DATABASE_1_ID;
    public final static Long QUERY_1_RESULT_NUMBER = 2L;
    public final static String QUERY_1_QUERY_HASH = "a3b8ac39e38167d14cf3a9c20a69e4b6954d049525390b973a2c23064953a992";
    public final static String QUERY_1_RESULT_HASH = "8358c8ade4849d2094ab5bb29127afdae57e6bb5acb1db7af603813d406c467a";
    public final static Instant QUERY_1_CREATED = Instant.ofEpochSecond(1677648377L);
    public final static Instant QUERY_1_EXECUTION = Instant.now();
    public final static Boolean QUERY_1_PERSISTED = true;

    public final static Query QUERY_1 = Query.builder()
            .id(QUERY_1_ID)
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .resultNumber(QUERY_1_RESULT_NUMBER)
            .created(QUERY_1_CREATED)
            .executed(QUERY_1_EXECUTION)
            .createdBy(USER_1_ID)
            .isPersisted(QUERY_1_PERSISTED)
            .build();

    public final static QueryDto QUERY_1_DTO = QueryDto.builder()
            .id(QUERY_1_ID)
            .databaseId(QUERY_1_DATABASE_ID)
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .created(QUERY_1_CREATED)
            .creator(USER_1_DTO)
            .execution(QUERY_1_EXECUTION)
            .createdBy(USER_1_ID)
            .build();

    public final static QueryBriefDto QUERY_1_BRIEF_DTO = QueryBriefDto.builder()
            .id(QUERY_1_ID)
            .databaseId(QUERY_1_DATABASE_ID)
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .created(QUERY_1_CREATED)
            .execution(QUERY_1_EXECUTION)
            .createdBy(USER_1_ID)
            .creator(USER_1_DTO)
            .build();

    public final static Long QUERY_2_ID = 2L;
    public final static String QUERY_2_STATEMENT = "SELECT `location` FROM `weather_aus`";
    public final static String QUERY_2_QUERY_HASH = "a2d2dd94ebc7653bb5a3b55dd8ed5e91d3d13c225c6855a1eb4eb7ca14c36ced";
    public final static Long QUERY_2_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long QUERY_2_DATABASE_ID = DATABASE_2_ID;
    public final static Long QUERY_2_RESULT_NUMBER = 2L;
    public final static String QUERY_2_RESULT_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static Instant QUERY_2_CREATED = Instant.now().minus(2, MINUTES);
    public final static Instant QUERY_2_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_2_LAST_MODIFIED = Instant.ofEpochSecond(1541588352L);
    public final static Boolean QUERY_2_PERSISTED = false;

    public final static Query QUERY_2 = Query.builder()
            .id(QUERY_2_ID)
            .query(QUERY_2_STATEMENT)
            .queryHash(QUERY_2_QUERY_HASH)
            .resultHash(QUERY_2_RESULT_HASH)
            .resultNumber(QUERY_2_RESULT_NUMBER)
            .created(QUERY_2_CREATED)
            .executed(QUERY_2_EXECUTION)
            .createdBy(USER_1_ID)
            .isPersisted(QUERY_2_PERSISTED)
            .build();

    public final static QueryDto QUERY_2_DTO = QueryDto.builder()
            .id(QUERY_2_ID)
            .databaseId(QUERY_2_DATABASE_ID)
            .query(QUERY_2_STATEMENT)
            .queryNormalized(QUERY_2_STATEMENT)
            .resultNumber(QUERY_2_RESULT_NUMBER)
            .resultHash(QUERY_2_RESULT_HASH)
            .lastModified(QUERY_2_LAST_MODIFIED)
            .created(QUERY_2_CREATED)
            .createdBy(USER_1_ID)
            .queryHash(QUERY_2_QUERY_HASH)
            .execution(QUERY_2_EXECUTION)
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

    public final static Query QUERY_3 = Query.builder()
            .id(QUERY_3_ID)
            .query(QUERY_3_STATEMENT)
            .queryHash(QUERY_3_QUERY_HASH)
            .resultHash(QUERY_3_RESULT_HASH)
            .created(QUERY_3_CREATED)
            .executed(QUERY_3_EXECUTION)
            .createdBy(USER_1_ID)
            .resultNumber(QUERY_3_RESULT_NUMBER)
            .isPersisted(QUERY_3_PERSISTED)
            .build();

    public final static QueryDto QUERY_3_DTO = QueryDto.builder()
            .id(QUERY_3_ID)
            .databaseId(QUERY_3_DATABASE_ID)
            .query(QUERY_3_STATEMENT)
            .queryNormalized(QUERY_3_STATEMENT)
            .resultNumber(QUERY_3_RESULT_NUMBER)
            .resultHash(QUERY_3_RESULT_HASH)
            .lastModified(QUERY_3_LAST_MODIFIED)
            .created(QUERY_3_CREATED)
            .createdBy(USER_1_ID)
            .queryHash(QUERY_3_QUERY_HASH)
            .execution(QUERY_3_EXECUTION)
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

    public final static Query QUERY_4 = Query.builder()
            .id(QUERY_4_ID)
            .query(QUERY_4_STATEMENT)
            .queryHash(QUERY_4_QUERY_HASH)
            .resultHash(QUERY_4_RESULT_HASH)
            .created(QUERY_4_CREATED)
            .executed(QUERY_4_EXECUTION)
            .isPersisted(QUERY_4_PERSISTED)
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .createdBy(USER_1_ID)
            .build();
    public final static List<Map<String, Object>> QUERY_4_RESULT_RESULT = List.of(
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
            }});

    public final static QueryResultDto QUERY_4_RESULT_DTO = QueryResultDto.builder()
            .id(QUERY_4_RESULT_ID)
            .result(QUERY_4_RESULT_RESULT)
            .build();

    public final static QueryDto QUERY_4_DTO = QueryDto.builder()
            .id(QUERY_4_ID)
            .databaseId(QUERY_4_DATABASE_ID)
            .query(QUERY_4_STATEMENT)
            .queryNormalized(QUERY_4_STATEMENT)
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .resultHash(QUERY_4_RESULT_HASH)
            .lastModified(QUERY_4_LAST_MODIFIED)
            .created(QUERY_4_CREATED)
            .createdBy(USER_1_ID)
            .queryHash(QUERY_4_QUERY_HASH)
            .execution(QUERY_4_EXECUTION)
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

    public final static Query QUERY_5 = Query.builder()
            .id(QUERY_5_ID)
            .query(QUERY_5_STATEMENT)
            .queryHash(QUERY_5_QUERY_HASH)
            .resultHash(QUERY_5_RESULT_HASH)
            .created(QUERY_5_CREATED)
            .executed(QUERY_5_EXECUTION)
            .createdBy(USER_1_ID)
            .isPersisted(QUERY_5_PERSISTED)
            .build();

    public final static QueryDto QUERY_5_DTO = QueryDto.builder()
            .id(QUERY_5_ID)
            .databaseId(QUERY_5_DATABASE_ID)
            .query(QUERY_5_STATEMENT)
            .queryNormalized(QUERY_5_STATEMENT)
            .resultNumber(QUERY_5_RESULT_NUMBER)
            .resultHash(QUERY_5_RESULT_HASH)
            .lastModified(QUERY_5_LAST_MODIFIED)
            .created(QUERY_5_CREATED)
            .queryHash(QUERY_5_QUERY_HASH)
            .execution(QUERY_5_EXECUTION)
            .build();

    public final static Long QUERY_6_ID = 6L;
    public final static String QUERY_6_STATEMENT = "SELECT `location` FROM `weather_aus` WHERE `id` = 1";
    public final static String QUERY_6_QUERY_HASH = "6d6dc48b12cdfd959d39a62887334a6bbd529b93eed4f211f3f671bd9e7d6225";
    public final static Long QUERY_6_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long QUERY_6_DATABASE_ID = DATABASE_2_ID;
    public final static String QUERY_6_RESULT_HASH = "ff5f7cbe1b96d596957f6e59e55b8b1b577fa5d505d5795af99595cfd50cb80d";
    public final static Instant QUERY_6_CREATED = Instant.now().minus(5, MINUTES);
    public final static Instant QUERY_6_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_6_LAST_MODIFIED = Instant.ofEpochSecond(1551588555L);
    public final static Long QUERY_6_RESULT_NUMBER = 1L;
    public final static Boolean QUERY_6_PERSISTED = true;

    public final static Query QUERY_6 = Query.builder()
            .id(QUERY_6_ID)
            .query(QUERY_6_STATEMENT)
            .queryHash(QUERY_6_QUERY_HASH)
            .resultHash(QUERY_6_RESULT_HASH)
            .created(QUERY_6_CREATED)
            .executed(QUERY_6_EXECUTION)
            .createdBy(USER_1_ID)
            .isPersisted(QUERY_6_PERSISTED)
            .build();

    public final static QueryDto QUERY_6_DTO = QueryDto.builder()
            .id(QUERY_6_ID)
            .databaseId(QUERY_6_DATABASE_ID)
            .query(QUERY_6_STATEMENT)
            .queryNormalized(QUERY_6_STATEMENT)
            .resultNumber(QUERY_6_RESULT_NUMBER)
            .resultHash(QUERY_6_RESULT_HASH)
            .lastModified(QUERY_6_LAST_MODIFIED)
            .created(QUERY_6_CREATED)
            .createdBy(USER_1_ID)
            .queryHash(QUERY_6_QUERY_HASH)
            .execution(QUERY_6_EXECUTION)
            .build();

    public final static List<TableColumn> TABLE_1_COLUMNS = List.of(TableColumn.builder()
                    .id(1L)
                    .ordinalPosition(0)
                    .table(TABLE_1)
                    .name("id")
                    .internalName("id")
                    .columnType(TableColumnType.BIGINT)
                    .isNullAllowed(false)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .enums(null)
                    .sets(null)
                    .build(),
            TableColumn.builder()
                    .id(2L)
                    .ordinalPosition(1)
                    .table(TABLE_1)
                    .name("Date")
                    .internalName("date")
                    .columnType(TableColumnType.DATE)
                    .dateFormat(IMAGE_DATE_1)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            TableColumn.builder()
                    .id(3L)
                    .ordinalPosition(2)
                    .table(TABLE_1)
                    .name("Location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            TableColumn.builder()
                    .id(4L)
                    .ordinalPosition(3)
                    .table(TABLE_1)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            TableColumn.builder()
                    .id(5L)
                    .ordinalPosition(4)
                    .table(TABLE_1)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .concept(COLUMN_CONCEPT_PRECIPITATION)
                    .unit(UNIT_MILLIMETRE)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build());

    public final static List<ColumnDto> TABLE_1_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(1L)
                    .name("id")
                    .internalName("id")
                    .columnType(ColumnTypeDto.BIGINT)
                    .isNullAllowed(false)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(2L)
                    .name("Date")
                    .internalName("date")
                    .columnType(ColumnTypeDto.DATE)
                    .dateFormat(IMAGE_DATE_1_DTO)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(3L)
                    .name("Location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(4L)
                    .name("MinTemp")
                    .internalName("mintemp")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(5L)
                    .name("Rainfall")
                    .internalName("rainfall")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .concept(COLUMN_CONCEPT_PRECIPITATION_DTO)
                    .unit(UNIT_MILLIMETRE_DTO)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build());

    public final static List<TableColumn> TABLE_2_COLUMNS = List.of(TableColumn.builder()
                    .id(6L)
                    .ordinalPosition(0)
                    .table(TABLE_2)
                    .name("location")
                    .internalName("location")
                    .columnType(TableColumnType.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .enums(null)
                    .sets(null)
                    .build(),
            TableColumn.builder()
                    .id(7L)
                    .ordinalPosition(1)
                    .table(TABLE_2)
                    .name("lat")
                    .internalName("lat")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            TableColumn.builder()
                    .id(8L)
                    .ordinalPosition(2)
                    .table(TABLE_2)
                    .name("lng")
                    .internalName("lng")
                    .columnType(TableColumnType.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build());

    public final static List<ColumnDto> TABLE_2_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(6L)
                    .name("location")
                    .internalName("location")
                    .columnType(ColumnTypeDto.VARCHAR)
                    .size(255L)
                    .isNullAllowed(false)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(7L)
                    .name("lat")
                    .internalName("lat")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build(),
            ColumnDto.builder()
                    .id(8L)
                    .name("lng")
                    .internalName("lng")
                    .columnType(ColumnTypeDto.DECIMAL)
                    .size(10L)
                    .d(0L)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enums(null)
                    .sets(null)
                    .build());

    public final static Long TABLE_1_FOREIGN_KEY_1_ID = 1L;
    public final static String TABLE_1_FOREIGN_KEY_1_NAME = "FK_JUNIT_1";

    public final static ForeignKey TABLE_1_FOREIGN_KEY_1 = ForeignKey.builder()
            .fkid(TABLE_1_FOREIGN_KEY_1_ID)
            .name(TABLE_1_FOREIGN_KEY_1_NAME)
            .referencedTable(TABLE_2)
            .table(TABLE_1)
            .references(new LinkedList<>()) /* TABLE_1_FOREIGN_KEY_REFERENCE */
            .build();

    public final static Long TABLE_1_FOREIGN_KEY_REFERENCE_ID = 1L;

    public final static ForeignKeyReference TABLE_1_FOREIGN_KEY_REFERENCE = ForeignKeyReference.builder()
            .id(TABLE_1_FOREIGN_KEY_REFERENCE_ID)
            .foreignKey(TABLE_1_FOREIGN_KEY_1)
            .column(TABLE_1_COLUMNS.get(2))
            .referencedColumn(TABLE_1_COLUMNS.get(0))
            .build();

    public final static Unique TABLE_1_UNIQUE_CONSTRAINT_1 = Unique.builder()
            .name("UK_1")
            .columns(new LinkedList<>())
            .table(TABLE_1)
            .build();

    public final static String TABLE_1_CHECK_1 = "`mintemp` > 0";

    public final static Unique TABLE_2_UNIQUE_CONSTRAINT_1 = Unique.builder()
            .name("UK_1")
            .columns(List.of(TABLE_2_COLUMNS.get(0)))
            .table(TABLE_2)
            .build();

    public final static List<TableColumn> TABLE_3_COLUMNS = List.of(TableColumn.builder()
                    .id(9L)
                    .table(TABLE_3)
                    .ordinalPosition(0)
                    .autoGenerated(true)
                    .columnType(TableColumnType.BIGINT)
                    .name("id")
                    .internalName("id")
                    .isNullAllowed(false)
                    .isPrimaryKey(true)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(10L)
                    .table(TABLE_3)
                    .ordinalPosition(1)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("linie")
                    .internalName("linie")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(11L)
                    .table(TABLE_3)
                    .ordinalPosition(2)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("richtung")
                    .internalName("richtung")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(12L)
                    .table(TABLE_3)
                    .ordinalPosition(3)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("betriebsdatum")
                    .internalName("betriebsdatum")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(13L)
                    .table(TABLE_3)
                    .ordinalPosition(4)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("fahrzeug")
                    .internalName("fahrzeug")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(14L)
                    .table(TABLE_3)
                    .ordinalPosition(5)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("kurs")
                    .internalName("kurs")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(15L)
                    .table(TABLE_3)
                    .ordinalPosition(6)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("seq_von")
                    .internalName("seq_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(16L)
                    .table(TABLE_3)
                    .ordinalPosition(7)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_diva_von")
                    .internalName("halt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(17L)
                    .table(TABLE_3)
                    .ordinalPosition(8)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_diva_von")
                    .internalName("halt_punkt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(18L)
                    .table(TABLE_3)
                    .ordinalPosition(9)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_kurz_von1")
                    .internalName("halt_kurz_von1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(19L)
                    .table(TABLE_3)
                    .ordinalPosition(10)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("datum_von")
                    .internalName("datum_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(20L)
                    .table(TABLE_3)
                    .ordinalPosition(11)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("soll_an_von")
                    .internalName("soll_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(21L)
                    .table(TABLE_3)
                    .ordinalPosition(12)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("ist_an_von")
                    .internalName("ist_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(22L)
                    .table(TABLE_3)
                    .ordinalPosition(13)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("soll_ab_von")
                    .internalName("soll_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(23L)
                    .table(TABLE_3)
                    .ordinalPosition(14)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("ist_ab_von")
                    .internalName("ist_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(24L)
                    .table(TABLE_3)
                    .ordinalPosition(15)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("seq_nach")
                    .internalName("seq_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(25L)
                    .table(TABLE_3)
                    .ordinalPosition(16)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_diva_nach")
                    .internalName("halt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(26L)
                    .table(TABLE_3)
                    .ordinalPosition(17)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_diva_nach")
                    .internalName("halt_punkt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(27L)
                    .table(TABLE_3)
                    .ordinalPosition(18)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_kurz_nach1")
                    .internalName("halt_kurz_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(28L)
                    .table(TABLE_3)
                    .ordinalPosition(19)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("datum_nach")
                    .internalName("datum_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(29L)
                    .table(TABLE_3)
                    .ordinalPosition(20)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("soll_an_nach")
                    .internalName("soll_an_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(30L)
                    .table(TABLE_3)
                    .ordinalPosition(21)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("ist_an_nach1")
                    .internalName("ist_an_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(31L)
                    .table(TABLE_3)
                    .ordinalPosition(22)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("soll_ab_nach")
                    .internalName("soll_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(32L)
                    .table(TABLE_3)
                    .ordinalPosition(23)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("ist_ab_nach")
                    .internalName("ist_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(33L)
                    .table(TABLE_3)
                    .ordinalPosition(24)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("fahrt_id")
                    .internalName("fahrt_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(34L)
                    .table(TABLE_3)
                    .ordinalPosition(25)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("fahrweg_id")
                    .internalName("fahrweg_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(35L)
                    .table(TABLE_3)
                    .ordinalPosition(26)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("fw_no")
                    .internalName("fw_no")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(36L)
                    .table(TABLE_3)
                    .ordinalPosition(27)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("fw_typ")
                    .internalName("fw_typ")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(37L)
                    .table(TABLE_3)
                    .ordinalPosition(28)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("fw_kurz")
                    .internalName("fw_kurz")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(38L)
                    .table(TABLE_3)
                    .ordinalPosition(29)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("fw_lang")
                    .internalName("fw_lang")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(39L)
                    .table(TABLE_3)
                    .ordinalPosition(30)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("umlauf_von")
                    .internalName("umlauf_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(40L)
                    .table(TABLE_3)
                    .ordinalPosition(31)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_id_von")
                    .internalName("halt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(41L)
                    .table(TABLE_3)
                    .ordinalPosition(32)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_id_nach")
                    .internalName("halt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(42L)
                    .table(TABLE_3)
                    .ordinalPosition(33)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_id_von")
                    .internalName("halt_punkt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            TableColumn.builder()
                    .id(43L)
                    .table(TABLE_3)
                    .ordinalPosition(34)
                    .autoGenerated(false)
                    .columnType(TableColumnType.INT)
                    .name("halt_punkt_id_nach")
                    .internalName("halt_punkt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build());

    public final static List<ColumnDto> TABLE_3_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(9L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(true)
                    .columnType(ColumnTypeDto.BIGINT)
                    .name("id")
                    .internalName("id")
                    .isNullAllowed(false)
                    .isPrimaryKey(true)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(10L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("linie")
                    .internalName("linie")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(11L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("richtung")
                    .internalName("richtung")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(12L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.DATE)
                    .name("betriebsdatum")
                    .internalName("betriebsdatum")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(IMAGE_DATE_2_DTO)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(13L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("fahrzeug")
                    .internalName("fahrzeug")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(14L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("kurs")
                    .internalName("kurs")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(15L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("seq_von")
                    .internalName("seq_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(16L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_diva_von")
                    .internalName("halt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(17L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_diva_von")
                    .internalName("halt_punkt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(18L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_kurz_von1")
                    .internalName("halt_kurz_von1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(19L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.DATE)
                    .name("datum_von")
                    .internalName("datum_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(IMAGE_DATE_2_DTO)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(20L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_an_von")
                    .internalName("soll_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(21L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_an_von")
                    .internalName("ist_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(22L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_ab_von")
                    .internalName("soll_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(23L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_ab_von")
                    .internalName("ist_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(24L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("seq_nach")
                    .internalName("seq_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(25L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_diva_nach")
                    .internalName("halt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(26L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_diva_nach")
                    .internalName("halt_punkt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(27L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_kurz_nach1")
                    .internalName("halt_kurz_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(28L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.DATE)
                    .name("datum_nach")
                    .internalName("datum_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(IMAGE_DATE_2_DTO)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(29L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_an_nach")
                    .internalName("soll_an_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(30L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_an_nach1")
                    .internalName("ist_an_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(31L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("soll_ab_nach")
                    .internalName("soll_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(32L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("ist_ab_nach")
                    .internalName("ist_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(33L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("fahrt_id")
                    .internalName("fahrt_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(34L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("fahrweg_id")
                    .internalName("fahrweg_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(35L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_no")
                    .internalName("fw_no")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(36L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_typ")
                    .internalName("fw_typ")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(37L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_kurz")
                    .internalName("fw_kurz")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(38L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("fw_lang")
                    .internalName("fw_lang")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(39L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("umlauf_von")
                    .internalName("umlauf_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(40L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_id_von")
                    .internalName("halt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(41L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_id_nach")
                    .internalName("halt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(42L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_id_von")
                    .internalName("halt_punkt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build(),
            ColumnDto.builder()
                    .id(43L)
                    .tableId(TABLE_3_ID)
                    .databaseId(DATABASE_1_ID)
                    .autoGenerated(false)
                    .columnType(ColumnTypeDto.INT)
                    .name("halt_punkt_id_nach")
                    .internalName("halt_punkt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enums(List.of())
                    .sets(List.of())
                    .build());

    public final static Unique TABLE_3_UNIQUE_CONSTRAINT_1 = Unique.builder()
            .name("UK_1")
            .columns(List.of(TABLE_3_COLUMNS.get(0)))
            .table(TABLE_3)
            .build();

    public final static ConstraintsDto TABLE_3_CONSTRAINTS_DTO = ConstraintsDto.builder()
            .uniques(List.of(UniqueDto.builder().columns(List.of(TABLE_3_COLUMNS_DTO.get(0))).build()))
            .foreignKeys(List.of())
            .checks(Set.of())
            .build();

    public final static List<TableColumn> TABLE_5_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_4_1_ID)
                    .ordinalPosition(COLUMN_4_1_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_1_NAME)
                    .internalName(COLUMN_4_1_INTERNAL_NAME)
                    .columnType(COLUMN_4_1_TYPE)
                    .isNullAllowed(COLUMN_4_1_NULL)
                    .autoGenerated(COLUMN_4_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_1_PRIMARY)
                    .enums(COLUMN_4_1_ENUM_VALUES)
                    .sets(COLUMN_4_1_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_2_ID)
                    .ordinalPosition(COLUMN_4_2_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_2_NAME)
                    .internalName(COLUMN_4_2_INTERNAL_NAME)
                    .columnType(COLUMN_4_2_TYPE)
                    .isNullAllowed(COLUMN_4_2_NULL)
                    .autoGenerated(COLUMN_4_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_2_PRIMARY)
                    .enums(COLUMN_4_2_ENUM_VALUES)
                    .sets(COLUMN_4_2_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_3_ID)
                    .ordinalPosition(COLUMN_4_3_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_3_NAME)
                    .internalName(COLUMN_4_3_INTERNAL_NAME)
                    .columnType(COLUMN_4_3_TYPE)
                    .isNullAllowed(COLUMN_4_3_NULL)
                    .autoGenerated(COLUMN_4_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_3_PRIMARY)
                    .enums(COLUMN_4_3_ENUM_VALUES)
                    .sets(COLUMN_4_3_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_4_ID)
                    .ordinalPosition(COLUMN_4_4_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_4_NAME)
                    .internalName(COLUMN_4_4_INTERNAL_NAME)
                    .columnType(COLUMN_4_4_TYPE)
                    .isNullAllowed(COLUMN_4_4_NULL)
                    .autoGenerated(COLUMN_4_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_4_PRIMARY)
                    .enums(COLUMN_4_4_ENUM_VALUES)
                    .sets(COLUMN_4_4_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_5_ID)
                    .ordinalPosition(COLUMN_4_5_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_5_NAME)
                    .internalName(COLUMN_4_5_INTERNAL_NAME)
                    .columnType(COLUMN_4_5_TYPE)
                    .isNullAllowed(COLUMN_4_5_NULL)
                    .autoGenerated(COLUMN_4_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_5_PRIMARY)
                    .enums(COLUMN_4_5_ENUM_VALUES)
                    .sets(COLUMN_4_5_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_6_ID)
                    .ordinalPosition(COLUMN_4_6_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_6_NAME)
                    .internalName(COLUMN_4_6_INTERNAL_NAME)
                    .columnType(COLUMN_4_6_TYPE)
                    .isNullAllowed(COLUMN_4_6_NULL)
                    .autoGenerated(COLUMN_4_6_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_6_PRIMARY)
                    .enums(COLUMN_4_6_ENUM_VALUES)
                    .sets(COLUMN_4_6_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_7_ID)
                    .ordinalPosition(COLUMN_4_7_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_7_NAME)
                    .internalName(COLUMN_4_7_INTERNAL_NAME)
                    .columnType(COLUMN_4_7_TYPE)
                    .isNullAllowed(COLUMN_4_7_NULL)
                    .autoGenerated(COLUMN_4_7_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_7_PRIMARY)
                    .enums(COLUMN_4_7_ENUM_VALUES)
                    .sets(COLUMN_4_7_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_8_ID)
                    .ordinalPosition(COLUMN_4_8_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_8_NAME)
                    .internalName(COLUMN_4_8_INTERNAL_NAME)
                    .columnType(COLUMN_4_8_TYPE)
                    .isNullAllowed(COLUMN_4_8_NULL)
                    .autoGenerated(COLUMN_4_8_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_8_PRIMARY)
                    .enums(COLUMN_4_8_ENUM_VALUES)
                    .sets(COLUMN_4_8_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_9_ID)
                    .ordinalPosition(COLUMN_4_9_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_9_NAME)
                    .internalName(COLUMN_4_9_INTERNAL_NAME)
                    .columnType(COLUMN_4_9_TYPE)
                    .isNullAllowed(COLUMN_4_9_NULL)
                    .autoGenerated(COLUMN_4_9_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_9_PRIMARY)
                    .enums(COLUMN_4_9_ENUM_VALUES)
                    .sets(COLUMN_4_9_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_10_ID)
                    .ordinalPosition(COLUMN_4_10_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_10_NAME)
                    .internalName(COLUMN_4_10_INTERNAL_NAME)
                    .columnType(COLUMN_4_10_TYPE)
                    .isNullAllowed(COLUMN_4_10_NULL)
                    .autoGenerated(COLUMN_4_10_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_10_PRIMARY)
                    .enums(COLUMN_4_10_ENUM_VALUES)
                    .sets(COLUMN_4_10_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_11_ID)
                    .ordinalPosition(COLUMN_4_11_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_11_NAME)
                    .internalName(COLUMN_4_11_INTERNAL_NAME)
                    .columnType(COLUMN_4_11_TYPE)
                    .isNullAllowed(COLUMN_4_11_NULL)
                    .autoGenerated(COLUMN_4_11_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_11_PRIMARY)
                    .enums(COLUMN_4_11_ENUM_VALUES)
                    .sets(COLUMN_4_11_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_12_ID)
                    .ordinalPosition(COLUMN_4_12_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_12_NAME)
                    .internalName(COLUMN_4_12_INTERNAL_NAME)
                    .columnType(COLUMN_4_12_TYPE)
                    .isNullAllowed(COLUMN_4_12_NULL)
                    .autoGenerated(COLUMN_4_12_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_12_PRIMARY)
                    .enums(COLUMN_4_12_ENUM_VALUES)
                    .sets(COLUMN_4_12_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_13_ID)
                    .ordinalPosition(COLUMN_4_13_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_13_NAME)
                    .internalName(COLUMN_4_13_INTERNAL_NAME)
                    .columnType(COLUMN_4_13_TYPE)
                    .isNullAllowed(COLUMN_4_13_NULL)
                    .autoGenerated(COLUMN_4_13_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_13_PRIMARY)
                    .enums(COLUMN_4_13_ENUM_VALUES)
                    .sets(COLUMN_4_13_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_14_ID)
                    .ordinalPosition(COLUMN_4_14_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_14_NAME)
                    .internalName(COLUMN_4_14_INTERNAL_NAME)
                    .columnType(COLUMN_4_14_TYPE)
                    .isNullAllowed(COLUMN_4_14_NULL)
                    .autoGenerated(COLUMN_4_14_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_14_PRIMARY)
                    .enums(COLUMN_4_14_ENUM_VALUES)
                    .sets(COLUMN_4_14_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_15_ID)
                    .ordinalPosition(COLUMN_4_15_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_15_NAME)
                    .internalName(COLUMN_4_15_INTERNAL_NAME)
                    .columnType(COLUMN_4_15_TYPE)
                    .isNullAllowed(COLUMN_4_15_NULL)
                    .autoGenerated(COLUMN_4_15_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_15_PRIMARY)
                    .enums(COLUMN_4_15_ENUM_VALUES)
                    .sets(COLUMN_4_15_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_16_ID)
                    .ordinalPosition(COLUMN_4_16_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_16_NAME)
                    .internalName(COLUMN_4_16_INTERNAL_NAME)
                    .columnType(COLUMN_4_16_TYPE)
                    .isNullAllowed(COLUMN_4_16_NULL)
                    .autoGenerated(COLUMN_4_16_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_16_PRIMARY)
                    .enums(COLUMN_4_16_ENUM_VALUES)
                    .sets(COLUMN_4_16_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_17_ID)
                    .ordinalPosition(COLUMN_4_17_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_17_NAME)
                    .internalName(COLUMN_4_17_INTERNAL_NAME)
                    .columnType(COLUMN_4_17_TYPE)
                    .isNullAllowed(COLUMN_4_17_NULL)
                    .autoGenerated(COLUMN_4_17_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_17_PRIMARY)
                    .enums(COLUMN_4_17_ENUM_VALUES)
                    .sets(COLUMN_4_17_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_18_ID)
                    .ordinalPosition(COLUMN_4_18_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_18_NAME)
                    .internalName(COLUMN_4_18_INTERNAL_NAME)
                    .columnType(COLUMN_4_18_TYPE)
                    .isNullAllowed(COLUMN_4_18_NULL)
                    .autoGenerated(COLUMN_4_18_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_18_PRIMARY)
                    .enums(COLUMN_4_18_ENUM_VALUES)
                    .sets(COLUMN_4_18_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_19_ID)
                    .ordinalPosition(COLUMN_4_19_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_19_NAME)
                    .internalName(COLUMN_4_19_INTERNAL_NAME)
                    .columnType(COLUMN_4_19_TYPE)
                    .isNullAllowed(COLUMN_4_19_NULL)
                    .autoGenerated(COLUMN_4_19_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_19_PRIMARY)
                    .enums(COLUMN_4_19_ENUM_VALUES)
                    .sets(COLUMN_4_19_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_20_ID)
                    .ordinalPosition(COLUMN_4_20_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_20_NAME)
                    .internalName(COLUMN_4_20_INTERNAL_NAME)
                    .columnType(COLUMN_4_20_TYPE)
                    .isNullAllowed(COLUMN_4_20_NULL)
                    .autoGenerated(COLUMN_4_20_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_20_PRIMARY)
                    .enums(COLUMN_4_20_ENUM_VALUES)
                    .sets(COLUMN_4_20_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_21_ID)
                    .ordinalPosition(COLUMN_4_21_ORDINALPOS)
                    .table(TABLE_5)
                    .name(COLUMN_4_21_NAME)
                    .internalName(COLUMN_4_21_INTERNAL_NAME)
                    .columnType(COLUMN_4_21_TYPE)
                    .isNullAllowed(COLUMN_4_21_NULL)
                    .autoGenerated(COLUMN_4_21_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_21_PRIMARY)
                    .enums(COLUMN_4_21_ENUM_VALUES)
                    .sets(COLUMN_4_21_SET_VALUES)
                    .build());

    public final static List<ColumnDto> TABLE_5_COLUMNS_DTO = List.of(ColumnDto.builder()
                    .id(COLUMN_4_1_ID)
                    .name(COLUMN_4_1_NAME)
                    .internalName(COLUMN_4_1_INTERNAL_NAME)
                    .columnType(COLUMN_4_1_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_1_NULL)
                    .autoGenerated(COLUMN_4_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_1_PRIMARY)
                    .enums(COLUMN_4_1_ENUM_VALUES)
                    .sets(COLUMN_4_1_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_2_ID)
                    .name(COLUMN_4_2_NAME)
                    .internalName(COLUMN_4_2_INTERNAL_NAME)
                    .columnType(COLUMN_4_2_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_2_NULL)
                    .autoGenerated(COLUMN_4_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_2_PRIMARY)
                    .enums(COLUMN_4_2_ENUM_VALUES)
                    .sets(COLUMN_4_2_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_3_ID)
                    .name(COLUMN_4_3_NAME)
                    .internalName(COLUMN_4_3_INTERNAL_NAME)
                    .columnType(COLUMN_4_3_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_3_NULL)
                    .autoGenerated(COLUMN_4_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_3_PRIMARY)
                    .enums(COLUMN_4_3_ENUM_VALUES)
                    .sets(COLUMN_4_3_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_4_ID)
                    .name(COLUMN_4_4_NAME)
                    .internalName(COLUMN_4_4_INTERNAL_NAME)
                    .columnType(COLUMN_4_4_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_4_NULL)
                    .autoGenerated(COLUMN_4_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_4_PRIMARY)
                    .enums(COLUMN_4_4_ENUM_VALUES)
                    .sets(COLUMN_4_4_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_5_ID)
                    .name(COLUMN_4_5_NAME)
                    .internalName(COLUMN_4_5_INTERNAL_NAME)
                    .columnType(COLUMN_4_5_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_5_NULL)
                    .autoGenerated(COLUMN_4_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_5_PRIMARY)
                    .enums(COLUMN_4_5_ENUM_VALUES)
                    .sets(COLUMN_4_5_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_6_ID)
                    .name(COLUMN_4_6_NAME)
                    .internalName(COLUMN_4_6_INTERNAL_NAME)
                    .columnType(COLUMN_4_6_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_6_NULL)
                    .autoGenerated(COLUMN_4_6_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_6_PRIMARY)
                    .enums(COLUMN_4_6_ENUM_VALUES)
                    .sets(COLUMN_4_6_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_7_ID)
                    .name(COLUMN_4_7_NAME)
                    .internalName(COLUMN_4_7_INTERNAL_NAME)
                    .columnType(COLUMN_4_7_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_7_NULL)
                    .autoGenerated(COLUMN_4_7_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_7_PRIMARY)
                    .enums(COLUMN_4_7_ENUM_VALUES)
                    .sets(COLUMN_4_7_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_8_ID)
                    .name(COLUMN_4_8_NAME)
                    .internalName(COLUMN_4_8_INTERNAL_NAME)
                    .columnType(COLUMN_4_8_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_8_NULL)
                    .autoGenerated(COLUMN_4_8_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_8_PRIMARY)
                    .enums(COLUMN_4_8_ENUM_VALUES)
                    .sets(COLUMN_4_8_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_9_ID)
                    .name(COLUMN_4_9_NAME)
                    .internalName(COLUMN_4_9_INTERNAL_NAME)
                    .columnType(COLUMN_4_9_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_9_NULL)
                    .autoGenerated(COLUMN_4_9_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_9_PRIMARY)
                    .enums(COLUMN_4_9_ENUM_VALUES)
                    .sets(COLUMN_4_9_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_10_ID)
                    .name(COLUMN_4_10_NAME)
                    .internalName(COLUMN_4_10_INTERNAL_NAME)
                    .columnType(COLUMN_4_10_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_10_NULL)
                    .autoGenerated(COLUMN_4_10_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_10_PRIMARY)
                    .enums(COLUMN_4_10_ENUM_VALUES)
                    .sets(COLUMN_4_10_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_11_ID)
                    .name(COLUMN_4_11_NAME)
                    .internalName(COLUMN_4_11_INTERNAL_NAME)
                    .columnType(COLUMN_4_11_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_11_NULL)
                    .autoGenerated(COLUMN_4_11_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_11_PRIMARY)
                    .enums(COLUMN_4_11_ENUM_VALUES)
                    .sets(COLUMN_4_11_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_12_ID)
                    .name(COLUMN_4_12_NAME)
                    .internalName(COLUMN_4_12_INTERNAL_NAME)
                    .columnType(COLUMN_4_12_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_12_NULL)
                    .autoGenerated(COLUMN_4_12_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_12_PRIMARY)
                    .enums(COLUMN_4_12_ENUM_VALUES)
                    .sets(COLUMN_4_12_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_13_ID)
                    .name(COLUMN_4_13_NAME)
                    .internalName(COLUMN_4_13_INTERNAL_NAME)
                    .columnType(COLUMN_4_13_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_13_NULL)
                    .autoGenerated(COLUMN_4_13_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_13_PRIMARY)
                    .enums(COLUMN_4_13_ENUM_VALUES)
                    .sets(COLUMN_4_13_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_14_ID)
                    .name(COLUMN_4_14_NAME)
                    .internalName(COLUMN_4_14_INTERNAL_NAME)
                    .columnType(COLUMN_4_14_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_14_NULL)
                    .autoGenerated(COLUMN_4_14_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_14_PRIMARY)
                    .enums(COLUMN_4_14_ENUM_VALUES)
                    .sets(COLUMN_4_14_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_15_ID)
                    .name(COLUMN_4_15_NAME)
                    .internalName(COLUMN_4_15_INTERNAL_NAME)
                    .columnType(COLUMN_4_15_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_15_NULL)
                    .autoGenerated(COLUMN_4_15_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_15_PRIMARY)
                    .enums(COLUMN_4_15_ENUM_VALUES)
                    .sets(COLUMN_4_15_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_16_ID)
                    .name(COLUMN_4_16_NAME)
                    .internalName(COLUMN_4_16_INTERNAL_NAME)
                    .columnType(COLUMN_4_16_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_16_NULL)
                    .autoGenerated(COLUMN_4_16_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_16_PRIMARY)
                    .enums(COLUMN_4_16_ENUM_VALUES)
                    .sets(COLUMN_4_16_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_17_ID)
                    .name(COLUMN_4_17_NAME)
                    .internalName(COLUMN_4_17_INTERNAL_NAME)
                    .columnType(COLUMN_4_17_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_17_NULL)
                    .autoGenerated(COLUMN_4_17_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_17_PRIMARY)
                    .enums(COLUMN_4_17_ENUM_VALUES)
                    .sets(COLUMN_4_17_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_18_ID)
                    .name(COLUMN_4_18_NAME)
                    .internalName(COLUMN_4_18_INTERNAL_NAME)
                    .columnType(COLUMN_4_18_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_18_NULL)
                    .autoGenerated(COLUMN_4_18_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_18_PRIMARY)
                    .enums(COLUMN_4_18_ENUM_VALUES)
                    .sets(COLUMN_4_18_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_19_ID)
                    .name(COLUMN_4_19_NAME)
                    .internalName(COLUMN_4_19_INTERNAL_NAME)
                    .columnType(COLUMN_4_19_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_19_NULL)
                    .autoGenerated(COLUMN_4_19_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_19_PRIMARY)
                    .enums(COLUMN_4_19_ENUM_VALUES)
                    .sets(COLUMN_4_19_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_20_ID)
                    .name(COLUMN_4_20_NAME)
                    .internalName(COLUMN_4_20_INTERNAL_NAME)
                    .columnType(COLUMN_4_20_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_20_NULL)
                    .autoGenerated(COLUMN_4_20_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_20_PRIMARY)
                    .enums(COLUMN_4_20_ENUM_VALUES)
                    .sets(COLUMN_4_20_SET_VALUES)
                    .build(),
            ColumnDto.builder()
                    .id(COLUMN_4_21_ID)
                    .name(COLUMN_4_21_NAME)
                    .internalName(COLUMN_4_21_INTERNAL_NAME)
                    .columnType(COLUMN_4_21_TYPE_DTO)
                    .isNullAllowed(COLUMN_4_21_NULL)
                    .autoGenerated(COLUMN_4_21_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_21_PRIMARY)
                    .enums(COLUMN_4_21_ENUM_VALUES)
                    .sets(COLUMN_4_21_SET_VALUES)
                    .build());

    public final static Constraints TABLE_5_CONSTRAINTS = Constraints.builder()
            .uniques(List.of(Unique.builder()
                    .name("UK_1")
                    .columns(List.of(TABLE_5_COLUMNS.get(0)))
                    .build()))
            .build();

    public final static List<ForeignKeyCreateDto> TABLE_5_FOREIGN_KEYS_INVALID_CREATE = List.of(ForeignKeyCreateDto.builder()
            .columns(List.of("somecolumn"))
            .referencedTable("sometable")
            .referencedColumns(List.of("someothercolumn"))
            .build());

    public final static ConstraintsCreateDto TABLE_5_CONSTRAINTS_INVALID_CREATE = ConstraintsCreateDto.builder()
            .foreignKeys(TABLE_5_FOREIGN_KEYS_INVALID_CREATE)
            .build();

    public final static List<ColumnCreateDto> TABLE_5_COLUMNS_INVALID_CREATE = List.of(ColumnCreateDto.builder()
            .name(COLUMN_4_2_NAME)
            .type(COLUMN_4_2_TYPE_DTO)
            .nullAllowed(COLUMN_4_2_NULL)
            .primaryKey(COLUMN_4_2_PRIMARY)
            .enums(COLUMN_4_2_ENUM_VALUES_ARR)
            .build());

    public final static List<ColumnCreateDto> TABLE_5_COLUMNS_CREATE = List.of(ColumnCreateDto.builder()
                    .name(COLUMN_4_1_NAME)
                    .type(COLUMN_4_1_TYPE_DTO)
                    .nullAllowed(COLUMN_4_1_NULL)
                    .primaryKey(COLUMN_4_1_PRIMARY)
                    .enums(COLUMN_4_2_ENUM_VALUES_ARR)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_4_2_NAME)
                    .type(COLUMN_4_2_TYPE_DTO)
                    .nullAllowed(COLUMN_4_2_NULL)
                    .primaryKey(COLUMN_4_2_PRIMARY)
                    .enums(COLUMN_4_2_ENUM_VALUES_ARR)
                    .build());

    public final static TableCreateDto TABLE_5_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_5_NAME)
            .description(TABLE_5_DESCRIPTION)
            .columns(TABLE_5_COLUMNS_CREATE)
            .constraints(null)
            .build();

    public final static TableCreateDto TABLE_5_INVALID_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_5_NAME)
            .description(TABLE_5_DESCRIPTION)
            .columns(TABLE_5_COLUMNS_CREATE)
            .constraints(TABLE_5_CONSTRAINTS_INVALID_CREATE)
            .build();

    public final static List<TableColumn> TABLE_6_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_5_1_ID)
                    .ordinalPosition(COLUMN_5_1_ORDINALPOS)
                    .table(TABLE_6)
                    .name(COLUMN_5_1_NAME)
                    .internalName(COLUMN_5_1_INTERNAL_NAME)
                    .columnType(COLUMN_5_1_TYPE)
                    .isNullAllowed(COLUMN_5_1_NULL)
                    .autoGenerated(COLUMN_5_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_1_PRIMARY)
                    .enums(COLUMN_5_1_ENUM_VALUES)
                    .sets(COLUMN_5_1_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_2_ID)
                    .ordinalPosition(COLUMN_5_2_ORDINALPOS)
                    .table(TABLE_6)
                    .name(COLUMN_5_2_NAME)
                    .internalName(COLUMN_5_2_INTERNAL_NAME)
                    .columnType(COLUMN_5_2_TYPE)
                    .isNullAllowed(COLUMN_5_2_NULL)
                    .autoGenerated(COLUMN_5_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_2_PRIMARY)
                    .enums(COLUMN_5_2_ENUM_VALUES)
                    .sets(COLUMN_5_2_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_3_ID)
                    .ordinalPosition(COLUMN_5_3_ORDINALPOS)
                    .table(TABLE_6)
                    .name(COLUMN_5_3_NAME)
                    .internalName(COLUMN_5_3_INTERNAL_NAME)
                    .columnType(COLUMN_5_3_TYPE)
                    .isNullAllowed(COLUMN_5_3_NULL)
                    .autoGenerated(COLUMN_5_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_3_PRIMARY)
                    .enums(COLUMN_5_3_ENUM_VALUES)
                    .sets(COLUMN_5_3_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_4_ID)
                    .ordinalPosition(COLUMN_5_4_ORDINALPOS)
                    .table(TABLE_6)
                    .name(COLUMN_5_4_NAME)
                    .internalName(COLUMN_5_4_INTERNAL_NAME)
                    .columnType(COLUMN_5_4_TYPE)
                    .isNullAllowed(COLUMN_5_4_NULL)
                    .autoGenerated(COLUMN_5_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_4_PRIMARY)
                    .enums(COLUMN_5_4_ENUM_VALUES)
                    .sets(COLUMN_5_4_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_5_ID)
                    .ordinalPosition(COLUMN_5_5_ORDINALPOS)
                    .table(TABLE_6)
                    .name(COLUMN_5_5_NAME)
                    .internalName(COLUMN_5_5_INTERNAL_NAME)
                    .columnType(COLUMN_5_5_TYPE)
                    .isNullAllowed(COLUMN_5_5_NULL)
                    .autoGenerated(COLUMN_5_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_5_PRIMARY)
                    .enums(COLUMN_5_5_ENUM_VALUES)
                    .sets(COLUMN_5_5_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_6_ID)
                    .ordinalPosition(COLUMN_5_6_ORDINALPOS)
                    .table(TABLE_6)
                    .name(COLUMN_5_6_NAME)
                    .internalName(COLUMN_5_6_INTERNAL_NAME)
                    .columnType(COLUMN_5_6_TYPE)
                    .isNullAllowed(COLUMN_5_6_NULL)
                    .autoGenerated(COLUMN_5_6_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_6_PRIMARY)
                    .enums(COLUMN_5_6_ENUM_VALUES)
                    .sets(COLUMN_5_6_SET_VALUES)
                    .build());

    public final static Constraints TABLE_6_CONSTRAINTS = Constraints.builder()
            .uniques(List.of(Unique.builder()
                    .name("UK_1")
                    .columns(List.of(TABLE_6_COLUMNS.get(0)))
                    .build()))
            .build();

    public final static List<ColumnCreateDto> TABLE_6_COLUMNS_CREATE = List.of(
            ColumnCreateDto.builder()
                    .name(COLUMN_5_1_NAME)
                    .type(COLUMN_5_1_TYPE_DTO)
                    .nullAllowed(COLUMN_5_1_NULL)
                    .primaryKey(COLUMN_5_1_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_2_NAME)
                    .type(COLUMN_5_2_TYPE_DTO)
                    .size(COLUMN_5_2_SIZE)
                    .nullAllowed(COLUMN_5_2_NULL)
                    .primaryKey(COLUMN_5_2_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_3_NAME)
                    .type(COLUMN_5_3_TYPE_DTO)
                    .size(COLUMN_5_3_SIZE)
                    .nullAllowed(COLUMN_5_3_NULL)
                    .primaryKey(COLUMN_5_3_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_4_NAME)
                    .type(COLUMN_5_4_TYPE_DTO)
                    .nullAllowed(COLUMN_5_4_NULL)
                    .primaryKey(COLUMN_5_4_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_5_NAME)
                    .type(COLUMN_5_5_TYPE_DTO)
                    .nullAllowed(COLUMN_5_5_NULL)
                    .primaryKey(COLUMN_5_5_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_6_NAME)
                    .type(COLUMN_5_6_TYPE_DTO)
                    .nullAllowed(COLUMN_5_6_NULL)
                    .primaryKey(COLUMN_5_6_PRIMARY)
                    .build());

    public final static List<List<String>> TABLE_6_UNIQUES_CREATE = List.of(
            List.of(COLUMN_5_1_NAME),
            List.of(COLUMN_5_2_NAME, COLUMN_5_3_NAME));

    public final static List<ForeignKeyCreateDto> TABLE_6_FOREIGN_KEYS_CREATE = List.of(ForeignKeyCreateDto.builder()
            .columns(List.of(COLUMN_5_6_NAME))
            .referencedTable(TABLE_5_NAME)
            .referencedColumns(List.of(COLUMN_4_1_NAME))
            .build());

    public final static Set<String> TABLE_6_CHECKS_CREATE = Set.of(
            COLUMN_5_2_NAME + " != " + COLUMN_5_3_NAME);

    public final static ConstraintsCreateDto TABLE_6_CONSTRAINTS_CREATE = ConstraintsCreateDto.builder()
            .uniques(TABLE_6_UNIQUES_CREATE)
            .foreignKeys(TABLE_6_FOREIGN_KEYS_CREATE)
            .checks(TABLE_6_CHECKS_CREATE)
            .build();

    public final static TableCreateDto TABLE_6_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_6_NAME)
            .description(TABLE_6_DESCRIPTION)
            .columns(TABLE_6_COLUMNS_CREATE)
            .constraints(TABLE_6_CONSTRAINTS_CREATE)
            .build();

    public final static Long COLUMN_6_1_ID = 26L;
    public final static Integer COLUMN_6_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_6_1_PRIMARY = true;
    public final static String COLUMN_6_1_NAME = "name_id";
    public final static String COLUMN_6_1_INTERNAL_NAME = "name_id";
    public final static TableColumnType COLUMN_6_1_TYPE = TableColumnType.BIGINT;
    public final static Long COLUMN_6_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_6_1_NULL = false;
    public final static Boolean COLUMN_6_1_AUTO_GENERATED = false;
    public final static String COLUMN_6_1_FOREIGN_KEY = null;
    public final static String COLUMN_6_1_CHECK = null;
    public final static List<String> COLUMN_6_1_ENUM_VALUES = null;
    public final static List<String> COLUMN_6_1_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_6_1_SET_VALUES = null;
    public final static List<String> COLUMN_6_1_SET_VALUES_DTO = null;

    public final static Long COLUMN_6_2_ID = 27L;
    public final static Integer COLUMN_6_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_6_2_PRIMARY = true;
    public final static String COLUMN_6_2_NAME = "zoo_id";
    public final static String COLUMN_6_2_INTERNAL_NAME = "zoo_id";
    public final static TableColumnType COLUMN_6_2_TYPE = TableColumnType.BIGINT;
    public final static Long COLUMN_6_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_6_2_NULL = false;
    public final static Boolean COLUMN_6_2_AUTO_GENERATED = false;
    public final static String COLUMN_6_2_FOREIGN_KEY = null;
    public final static String COLUMN_6_2_CHECK = null;
    public final static List<String> COLUMN_6_2_ENUM_VALUES = null;
    public final static List<String> COLUMN_6_2_ENUM_VALUES_DTO = null;
    public final static List<String> COLUMN_6_2_SET_VALUES = null;
    public final static List<String> COLUMN_6_2_SET_VALUES_DTO = null;

    public final static List<TableColumn> TABLE_7_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_6_1_ID)
                    .ordinalPosition(COLUMN_6_1_ORDINALPOS)
                    .table(TABLE_7)
                    .name(COLUMN_6_1_NAME)
                    .internalName(COLUMN_6_1_INTERNAL_NAME)
                    .columnType(COLUMN_6_1_TYPE)
                    .isNullAllowed(COLUMN_6_1_NULL)
                    .autoGenerated(COLUMN_6_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_6_1_PRIMARY)
                    .enums(COLUMN_6_1_ENUM_VALUES)
                    .sets(COLUMN_6_1_SET_VALUES)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_6_2_ID)
                    .ordinalPosition(COLUMN_6_2_ORDINALPOS)
                    .table(TABLE_7)
                    .name(COLUMN_6_2_NAME)
                    .internalName(COLUMN_6_2_INTERNAL_NAME)
                    .columnType(COLUMN_6_2_TYPE)
                    .isNullAllowed(COLUMN_6_2_NULL)
                    .autoGenerated(COLUMN_6_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_6_2_PRIMARY)
                    .enums(COLUMN_6_2_ENUM_VALUES)
                    .sets(COLUMN_6_2_SET_VALUES)
                    .build());

    public final static Long VIEW_1_ID = 1L;
    public final static Boolean VIEW_1_INITIAL_VIEW = false;
    public final static String VIEW_1_NAME = "JUnit";
    public final static String VIEW_1_INTERNAL_NAME = "junit";
    public final static Long VIEW_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_1_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_1_PUBLIC = true;
    public final static String VIEW_1_QUERY = "select `location`, `lat`, `lng` from `weather_location`";
    public final static String VIEW_1_QUERY_HASH = "dc81a6877c7c51a6a6f406e1fc2a255e44a0d49a20548596e0d583c3eb849c23";

    public final static List<ColumnDto> VIEW_1_COLUMNS_DTO = List.of(
            TABLE_2_COLUMNS_DTO.get(0),
            TABLE_2_COLUMNS_DTO.get(1),
            TABLE_2_COLUMNS_DTO.get(2)
    );

    public final static View VIEW_1 = View.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .createdBy(USER_1_ID)
            .columns(null) /* VIEW_1_COLUMNS */
            .build();

    public final static List<ViewColumn> VIEW_1_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(1L)
                    .ordinalPosition(0)
                    .column(TABLE_2_COLUMNS.get(0))
                    .view(VIEW_1)
                    .build(),
            ViewColumn.builder()
                    .id(2L)
                    .ordinalPosition(1)
                    .column(TABLE_2_COLUMNS.get(1))
                    .view(VIEW_1)
                    .build(),
            ViewColumn.builder()
                    .id(3L)
                    .ordinalPosition(2)
                    .column(TABLE_2_COLUMNS.get(2))
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
            .createdBy(USER_1_ID)
            .query(VIEW_1_QUERY)
            .queryHash(VIEW_1_QUERY_HASH)
            .columns(VIEW_1_COLUMNS_DTO)
            .build();

    public final static ViewBriefDto VIEW_1_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .createdBy(USER_1_ID)
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
    public final static String VIEW_2_QUERY = "select `date`, `location` as loc, `location`, `rainfall`, `mintemp` from `weather_aus` where `location` = 'Albury'";
    public final static String VIEW_2_QUERY_HASH = "987fc946772ffb6d85060262dcb5df419692a1f6772ea995e3dedb53c191e984";

    public final static List<ColumnDto> VIEW_2_COLUMNS_DTO = List.of(
            TABLE_1_COLUMNS_DTO.get(1),
            TABLE_1_COLUMNS_DTO.get(2),
            TABLE_1_COLUMNS_DTO.get(4),
            TABLE_1_COLUMNS_DTO.get(3)
    );

    public final static View VIEW_2 = View.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .columns(null)  /* VIEW_2_COLUMNS */
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .createdBy(USER_1_ID)
            .build();

    public final static List<ViewColumn> VIEW_2_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(4L)
                    .ordinalPosition(0)
                    .column(TABLE_1_COLUMNS.get(1))
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(5L)
                    .ordinalPosition(1)
                    .alias("loc")
                    .column(TABLE_1_COLUMNS.get(2))
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(6L)
                    .ordinalPosition(2)
                    .column(TABLE_1_COLUMNS.get(4))
                    .view(VIEW_2)
                    .build(),
            ViewColumn.builder()
                    .id(7L)
                    .ordinalPosition(3)
                    .column(TABLE_1_COLUMNS.get(3))
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
            .columns(VIEW_2_COLUMNS_DTO)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .createdBy(USER_1_ID)
            .build();

    public final static ViewBriefDto VIEW_2_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .query(VIEW_2_QUERY)
            .queryHash(VIEW_2_QUERY_HASH)
            .createdBy(USER_1_ID)
            .build();

    public final static Long VIEW_3_ID = 3L;
    public final static Boolean VIEW_3_INITIAL_VIEW = false;
    public final static String VIEW_3_NAME = "JUnit3";
    public final static String VIEW_3_INTERNAL_NAME = "junit3";
    public final static Long VIEW_3_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_3_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_3_PUBLIC = false;
    public final static String VIEW_3_QUERY = "select w.`mintemp`, w.`rainfall`, w.`location`, m.`date` from `weather_aus` w join `junit2` m on m.`location` = w.`location` and m.`date` = w.`date`";
    public final static String VIEW_3_QUERY_HASH = "bbbaa56a5206b3dc3e6cf9301b0db9344eb6f19b100c7b88550ffb597a0bd255";

    public final static List<ColumnDto> VIEW_3_COLUMNS_DTO = List.of(
            TABLE_1_COLUMNS_DTO.get(3),
            TABLE_1_COLUMNS_DTO.get(4),
            TABLE_1_COLUMNS_DTO.get(2),
            TABLE_1_COLUMNS_DTO.get(1)
    );

    public final static View VIEW_3 = View.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .columns(null)  /* VIEW_3_COLUMNS */
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .createdBy(USER_1_ID)
            .build();

    public final static List<ViewColumn> VIEW_3_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(8L)
                    .ordinalPosition(0)
                    .column(TABLE_1_COLUMNS.get(3))
                    .view(VIEW_3)
                    .build(),
            ViewColumn.builder()
                    .id(9L)
                    .ordinalPosition(1)
                    .column(TABLE_1_COLUMNS.get(4))
                    .view(VIEW_3)
                    .build(),
            ViewColumn.builder()
                    .id(10L)
                    .ordinalPosition(2)
                    .column(TABLE_1_COLUMNS.get(2))
                    .view(VIEW_3)
                    .build(),
            ViewColumn.builder()
                    .id(11L)
                    .ordinalPosition(3)
                    .column(TABLE_1_COLUMNS.get(1))
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
            .columns(VIEW_3_COLUMNS_DTO)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .createdBy(USER_1_ID)
            .build();

    public final static ViewBriefDto VIEW_3_BRIEF_DTO = ViewBriefDto.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .query(VIEW_3_QUERY)
            .queryHash(VIEW_3_QUERY_HASH)
            .createdBy(USER_1_ID)
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
    public final static String VIEW_4_QUERY = "SELECT `animal_name`, `hair`, `feathers`, `eggs`, `milk`, `airborne`, `aquatic`, `predator`, `backbone`, `breathes`, `venomous`, `fins`, `legs`, `tail`, `domestic`, `catsize`, `class_type` FROM `zoo` WHERE `class_type` = 1";
    public final static String VIEW_4_QUERY_HASH = "3561cd0bb0b0e94d6f15ae602134252a5760d09d660a71a4fb015b6991c8ba0b";

    public final static List<ColumnDto> VIEW_4_COLUMNS_DTO = List.of(
            TABLE_5_COLUMNS_DTO.get(1),
            TABLE_5_COLUMNS_DTO.get(2),
            TABLE_5_COLUMNS_DTO.get(3),
            TABLE_5_COLUMNS_DTO.get(5),
            TABLE_5_COLUMNS_DTO.get(6),
            TABLE_5_COLUMNS_DTO.get(8),
            TABLE_5_COLUMNS_DTO.get(10),
            TABLE_5_COLUMNS_DTO.get(11),
            TABLE_5_COLUMNS_DTO.get(12),
            TABLE_5_COLUMNS_DTO.get(13),
            TABLE_5_COLUMNS_DTO.get(14),
            TABLE_5_COLUMNS_DTO.get(15),
            TABLE_5_COLUMNS_DTO.get(16),
            TABLE_5_COLUMNS_DTO.get(17),
            TABLE_5_COLUMNS_DTO.get(18),
            TABLE_5_COLUMNS_DTO.get(19),
            TABLE_5_COLUMNS_DTO.get(20)
    );

    public final static View VIEW_4 = View.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vdbid(VIEW_4_DATABASE_ID)
            .isPublic(VIEW_4_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .createdBy(USER_1_ID)
            .columns(null) /* VIEW_4_COLUMNS */
            .build();

    public final static List<ViewColumn> VIEW_4_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(12L)
                    .ordinalPosition(0)
                    .column(TABLE_5_COLUMNS.get(1))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(13L)
                    .ordinalPosition(1)
                    .column(TABLE_5_COLUMNS.get(2))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(14L)
                    .ordinalPosition(2)
                    .column(TABLE_5_COLUMNS.get(3))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(15L)
                    .ordinalPosition(3)
                    .column(TABLE_5_COLUMNS.get(5))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(16L)
                    .ordinalPosition(4)
                    .column(TABLE_5_COLUMNS.get(6))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(17L)
                    .ordinalPosition(5)
                    .column(TABLE_5_COLUMNS.get(8))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(18L)
                    .ordinalPosition(6)
                    .column(TABLE_5_COLUMNS.get(10))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(19L)
                    .ordinalPosition(7)
                    .column(TABLE_5_COLUMNS.get(11))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(20L)
                    .ordinalPosition(8)
                    .column(TABLE_5_COLUMNS.get(12))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(21L)
                    .ordinalPosition(9)
                    .column(TABLE_5_COLUMNS.get(13))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(22L)
                    .ordinalPosition(10)
                    .column(TABLE_5_COLUMNS.get(14))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(23L)
                    .ordinalPosition(11)
                    .column(TABLE_5_COLUMNS.get(15))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(24L)
                    .ordinalPosition(12)
                    .column(TABLE_5_COLUMNS.get(16))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(25L)
                    .ordinalPosition(13)
                    .column(TABLE_5_COLUMNS.get(17))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(26L)
                    .ordinalPosition(14)
                    .column(TABLE_5_COLUMNS.get(18))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(27L)
                    .ordinalPosition(15)
                    .column(TABLE_5_COLUMNS.get(19))
                    .view(VIEW_4)
                    .build(),
            ViewColumn.builder()
                    .id(28L)
                    .ordinalPosition(16)
                    .column(TABLE_5_COLUMNS.get(20))
                    .view(VIEW_4)
                    .build()
    );

    public final static ViewDto VIEW_4_DTO = ViewDto.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vdbid(VIEW_4_DATABASE_ID)
            .isPublic(VIEW_4_PUBLIC)
            .query(VIEW_4_QUERY)
            .queryHash(VIEW_4_QUERY_HASH)
            .createdBy(USER_1_ID)
            .columns(VIEW_4_COLUMNS_DTO)
            .build();

    public final static Long VIEW_5_ID = 5L;
    public final static Boolean VIEW_5_INITIAL_VIEW = false;
    public final static String VIEW_5_NAME = "Mock View";
    public final static String VIEW_5_INTERNAL_NAME = "mock_view";
    public final static Long VIEW_5_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long VIEW_5_DATABASE_ID = DATABASE_3_ID;
    public final static Boolean VIEW_5_PUBLIC = true;
    public final static String VIEW_5_QUERY = "SELECT `location`, `lat`, `lng` FROM `weather_location` WHERE `location` = 'Albury'";
    public final static String VIEW_5_QUERY_HASH = "120f32478aaff874c25ab32eceb9f00b64cc9d422831046f2f5d43953aca01e7";

    public final static View VIEW_5 = View.builder()
            .id(VIEW_5_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .vdbid(VIEW_5_DATABASE_ID)
            .isPublic(VIEW_5_PUBLIC)
            .query(VIEW_5_QUERY)
            .queryHash(VIEW_5_QUERY_HASH)
            .createdBy(USER_1_ID)
            .columns(null)
            .build();

    public final static List<ViewColumn> VIEW_5_COLUMNS = List.of(
            ViewColumn.builder()
                    .id(29L)
                    .ordinalPosition(0)
                    .column(TABLE_2_COLUMNS.get(0))
                    .view(VIEW_5)
                    .build(),
            ViewColumn.builder()
                    .id(30L)
                    .ordinalPosition(1)
                    .column(TABLE_2_COLUMNS.get(1))
                    .view(VIEW_5)
                    .build(),
            ViewColumn.builder()
                    .id(31L)
                    .ordinalPosition(2)
                    .column(TABLE_2_COLUMNS.get(2))
                    .view(VIEW_5)
                    .build());

    public final static Long QUERY_1_RESULT_ID = 1L;
    public final static List<Map<String, Object>> QUERY_1_RESULT_RESULT = List.of(
            new HashMap<>() {{
                put("location", "Albury");
                put("lat", -36.0653583);
                put("lng", 146.9112214);
            }}, new HashMap<>() {{
                put("location", "Sydney");
                put("lat", -33.847927);
                put("lng", 150.6517942);
            }});

    public final static QueryResultDto QUERY_1_RESULT_DTO = QueryResultDto.builder()
            .id(QUERY_1_RESULT_ID)
            .result(QUERY_1_RESULT_RESULT)
            .build();

    public final static TableCsvDto TABLE_1_CSV_DTO = TableCsvDto.builder()
            .data(new HashMap<>() {{
                put("id", 1);
                put("date", "2022-12-20");
                put("location", "Vienna");
                put("mintemp", -2.3);
                put("rainfall", 34.3);
            }})
            .build();

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
    public final static String IDENTIFIER_1_DOI = null;
    public final static String IDENTIFIER_1_DOI_NOT_NULL = "10.1000/183";
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
    public final static UUID IDENTIFIER_1_CREATED_BY = USER_1_ID;

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
            .description(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION)
            .descriptionType(IDENTIFIER_1_DESCRIPTION_1_TYPE_DTO)
            .language(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO)
            .build();

    public final static Creator IDENTIFIER_1_CREATOR_1 = Creator.builder()
            .id(CREATOR_1_ID)
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

    public final static CreatorDto IDENTIFIER_1_CREATOR_1_DTO = CreatorDto.builder()
            .id(CREATOR_1_ID)
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

    public final static CreatorSaveDto IDENTIFIER_1_CREATOR_1_CREATE_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
            .build();

    public final static CreatorSaveDto IDENTIFIER_1_CREATOR_1_MODIFY_DTO = CreatorSaveDto.builder()
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .creatorName(CREATOR_1_NAME)
            .nameIdentifier(CREATOR_1_ORCID)
            .nameIdentifierScheme(NameIdentifierSchemeTypeDto.ORCID)
            .affiliation("JKU Linz")
            .affiliationIdentifier(CREATOR_1_AFFIL_ROR)
            .affiliationIdentifierScheme(CREATOR_1_AFFIL_TYPE_DTO)
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

    public final static Identifier IDENTIFIER_1 = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .titles(List.of(IDENTIFIER_1_TITLE_1, IDENTIFIER_1_TITLE_2))
            .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1))
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
            .createdBy(USER_1_ID)
            .licenses(List.of(LICENSE_1))
            .creators(List.of(IDENTIFIER_1_CREATOR_1))
            .funders(List.of(IDENTIFIER_1_FUNDER_1))
            .build();

    public final static Identifier IDENTIFIER_1_WITH_DOI = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1))
            .titles(List.of(IDENTIFIER_1_TITLE_1, IDENTIFIER_1_TITLE_2))
            .doi(IDENTIFIER_1_DOI_NOT_NULL)
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
            .createdBy(USER_1_ID)
            .licenses(List.of(LICENSE_1))
            .creators(List.of(IDENTIFIER_1_CREATOR_1))
            .funders(List.of(IDENTIFIER_1_FUNDER_1))
            .build();

    public final static IdentifierDto IDENTIFIER_1_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_DTO))
            .titles(List.of(IDENTIFIER_1_TITLE_1_DTO, IDENTIFIER_1_TITLE_2_DTO))
            .doi(IDENTIFIER_1_DOI)
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
            .type(IDENTIFIER_1_TYPE_DTO)
            .creator(USER_1_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .creators(List.of(IDENTIFIER_1_CREATOR_1_DTO))
            .funders(List.of(IDENTIFIER_1_FUNDER_1_DTO))
            .build();

    public final static IdentifierDto IDENTIFIER_1_WITH_DOI_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_DTO))
            .titles(List.of(IDENTIFIER_1_TITLE_1_DTO, IDENTIFIER_1_TITLE_2_DTO))
            .doi(IDENTIFIER_1_DOI_NOT_NULL)
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
            .type(IDENTIFIER_1_TYPE_DTO)
            .creator(USER_1_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .creators(List.of(IDENTIFIER_1_CREATOR_1_DTO))
            .funders(List.of(IDENTIFIER_1_FUNDER_1_DTO))
            .build();


    public final static IdentifierDto IDENTIFIER_1_MODIFY_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_2_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_DTO_MODIFY))
            .titles(List.of(IDENTIFIER_1_TITLE_1_DTO_MODIFY, IDENTIFIER_1_TITLE_2_DTO))
            .doi(IDENTIFIER_1_DOI)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .type(IDENTIFIER_1_TYPE_DTO)
            .created(IDENTIFIER_1_CREATED)
            .lastModified(IDENTIFIER_1_MODIFIED)
            .licenses(List.of(LICENSE_1_DTO))
            .creators(List.of(IDENTIFIER_1_CREATOR_1_DTO))
            .build();

    public final static IdentifierSaveDto IDENTIFIER_1_DTO_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
            .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO, IDENTIFIER_1_TITLE_2_CREATE_DTO))
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO))
            .funders(List.of(IDENTIFIER_1_FUNDER_1_CREATE_DTO))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .build();

    public final static IdentifierSaveDto IDENTIFIER_1_DTO_UPDATE_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
            .titles(List.of(IDENTIFIER_1_TITLE_1_UPDATE_DTO, IDENTIFIER_1_TITLE_2_UPDATE_DTO))
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(List.of(IDENTIFIER_1_CREATOR_1_MODIFY_DTO)) /* <<<< */
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .build();

    public final static Long IDENTIFIER_5_ID = 5L;
    public final static Long IDENTIFIER_5_QUERY_ID = QUERY_2_ID;
    public final static Long IDENTIFIER_5_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long IDENTIFIER_5_DATABASE_ID = DATABASE_2_ID;
    public final static String IDENTIFIER_5_DOI = "10.4225/13/50BBFCFE08A12";
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
            .databaseId(DATABASE_2_ID)
            .queryId(IDENTIFIER_5_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_5_DESCRIPTION_1))
            .titles(List.of(IDENTIFIER_5_TITLE_1))
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
            .createdBy(USER_2_ID)
            .creators(List.of(IDENTIFIER_5_CREATOR_1, IDENTIFIER_5_CREATOR_2))
            .build();

    public final static IdentifierDto IDENTIFIER_5_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_5_ID)
            .databaseId(DATABASE_2_ID)
            .queryId(IDENTIFIER_5_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_5_DESCRIPTION_1_DTO))
            .titles(List.of(IDENTIFIER_5_TITLE_1_DTO))
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
            .type(IDENTIFIER_5_TYPE_DTO)
            .creator(USER_2_DTO)
            .creators(List.of(IDENTIFIER_5_CREATOR_1_DTO, IDENTIFIER_5_CREATOR_2_DTO))
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

    public final static IdentifierSaveDto IDENTIFIER_5_DTO_REQUEST = IdentifierSaveDto.builder()
            .queryId(IDENTIFIER_5_QUERY_ID)
            .databaseId(IDENTIFIER_5_DATABASE_ID)
            .descriptions(List.of(IDENTIFIER_5_DESCRIPTION_1_CREATE_DTO))
            .titles(List.of(IDENTIFIER_5_TITLE_1_CREATE_DTO))
            .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
            .publicationDay(IDENTIFIER_5_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_5_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .creators(List.of(IDENTIFIER_5_CREATOR_1_CREATE_DTO, IDENTIFIER_5_CREATOR_2_CREATE_DTO))
            .publisher(IDENTIFIER_5_PUBLISHER)
            .licenses(List.of(LICENSE_1_DTO))
            .type(IDENTIFIER_5_TYPE_DTO)
            .build();

    public final static IdentifierSaveDto IDENTIFIER_5_DTO_UPDATE_REQUEST = IdentifierSaveDto.builder()
            .queryId(IDENTIFIER_5_QUERY_ID)
            .databaseId(IDENTIFIER_5_DATABASE_ID)
            .descriptions(List.of(IDENTIFIER_5_DESCRIPTION_1_CREATE_DTO))
            .titles(List.of(IDENTIFIER_5_TITLE_1_CREATE_DTO))
            .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
            .publicationDay(IDENTIFIER_5_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_5_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
            .creators(List.of(IDENTIFIER_5_CREATOR_1_MODIFY_DTO, IDENTIFIER_5_CREATOR_2_MODIFY_DTO))
            .publisher(IDENTIFIER_5_PUBLISHER)
            .licenses(List.of(LICENSE_1_DTO))
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
            .affiliationIdentifier(CREATOR_3_AFFIL_ROR)
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
            .build();

    public final static Identifier IDENTIFIER_6 = Identifier.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(IDENTIFIER_6_DATABASE_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_6_DESCRIPTION_1))
            .titles(List.of(IDENTIFIER_6_TITLE_1))
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
            .createdBy(USER_3_ID)
            .licenses(List.of(LICENSE_1))
            .creators(List.of(IDENTIFIER_6_CREATOR_1, IDENTIFIER_6_CREATOR_2, IDENTIFIER_6_CREATOR_3))
            .build();

    public final static IdentifierDto IDENTIFIER_6_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_6_ID)
            .databaseId(DATABASE_3_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_6_DESCRIPTION_1_DTO))
            .titles(List.of(IDENTIFIER_6_TITLE_1_DTO))
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
            .type(IDENTIFIER_6_TYPE_DTO)
            .creator(USER_3_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .creators(List.of(IDENTIFIER_6_CREATOR_1_DTO, IDENTIFIER_6_CREATOR_2_DTO, IDENTIFIER_6_CREATOR_3_DTO))
            .build();

    public final static IdentifierSaveDto IDENTIFIER_6_DTO_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_6_DATABASE_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_6_DESCRIPTION_1_CREATE_DTO))
            .titles(List.of(IDENTIFIER_6_TITLE_1_CREATE_DTO))
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_6_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .creators(List.of(IDENTIFIER_6_CREATOR_1_CREATE_DTO))
            .publisher(IDENTIFIER_6_PUBLISHER)
            .type(IDENTIFIER_6_TYPE_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .build();

    public final static IdentifierSaveDto IDENTIFIER_6_DTO_UPDATE_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_6_DATABASE_ID)
            .queryId(IDENTIFIER_6_QUERY_ID)
            .descriptions(List.of(IDENTIFIER_6_DESCRIPTION_1_CREATE_DTO))
            .titles(List.of(IDENTIFIER_6_TITLE_1_CREATE_DTO))
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_6_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_6_PUBLICATION_YEAR)
            .creators(List.of(IDENTIFIER_6_CREATOR_1_MODIFY_DTO))
            .publisher(IDENTIFIER_6_PUBLISHER)
            .type(IDENTIFIER_6_TYPE_DTO)
            .licenses(List.of(LICENSE_1_DTO))
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
    public final static String IDENTIFIER_7_QUERY_HASH = "abc";
    public final static String IDENTIFIER_7_RESULT_HASH = "def";
    public final static String IDENTIFIER_7_QUERY = "SELECT `id` FROM `foobar`";
    public final static String IDENTIFIER_7_NORMALIZED = "SELECT `id` FROM `foobar`";
    public final static Long IDENTIFIER_7_RESULT_NUMBER = 2L;
    public final static String IDENTIFIER_7_PUBLISHER = "Swedish Government";
    public final static IdentifierType IDENTIFIER_7_TYPE = IdentifierType.DATABASE;
    public final static IdentifierTypeDto IDENTIFIER_7_TYPE_DTO = IdentifierTypeDto.DATABASE;

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
            .descriptions(List.of())
            .titles(List.of())
            .doi(IDENTIFIER_7_DOI)
            .created(IDENTIFIER_7_CREATED)
            .lastModified(IDENTIFIER_7_MODIFIED)
            .execution(IDENTIFIER_7_EXECUTION)
            .publicationDay(IDENTIFIER_7_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_7_QUERY_HASH)
            .resultHash(IDENTIFIER_7_RESULT_HASH)
            .query(IDENTIFIER_7_QUERY)
            .queryNormalized(IDENTIFIER_7_NORMALIZED)
            .resultNumber(IDENTIFIER_7_RESULT_NUMBER)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE_DTO)
            .creator(USER_4_DTO)
            .licenses(List.of())
            .funders(List.of())
            .creators(List.of())
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

    public final static IdentifierSaveDto IDENTIFIER_7_DTO_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_7_DATABASE_ID)
            .descriptions(List.of())
            .titles(List.of())
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .creators(List.of(IDENTIFIER_7_CREATOR_1_CREATE_DTO))
            .funders(List.of())
            .licenses(List.of())
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

    public final static Identifier IDENTIFIER_2 = Identifier.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .descriptions(List.of())
            .titles(List.of())
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
            .createdBy(USER_1_ID)
            .licenses(List.of(LICENSE_1))
            .creators(List.of())
            .build();

    public final static IdentifierDto IDENTIFIER_2_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_2_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .descriptions(List.of())
            .titles(List.of())
            .doi(IDENTIFIER_2_DOI)
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
            .type(IDENTIFIER_2_TYPE_DTO)
            .creator(USER_1_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .creators(List.of())
            .build();

    public final static IdentifierSaveDto IDENTIFIER_2_DTO_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .descriptions(List.of())
            .titles(List.of())
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .creators(List.of())
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .licenses(List.of(LICENSE_1_DTO))
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

    public final static Identifier IDENTIFIER_3 = Identifier.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .descriptions(List.of())
            .titles(List.of())
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
            .createdBy(USER_1_ID)
            .licenses(List.of(LICENSE_1))
            .creators(List.of())
            .build();

    public final static IdentifierDto IDENTIFIER_3_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .descriptions(List.of())
            .titles(List.of())
            .doi(IDENTIFIER_3_DOI)
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
            .type(IDENTIFIER_3_TYPE_DTO)
            .creator(USER_1_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .creators(List.of())
            .build();

    public final static IdentifierSaveDto IDENTIFIER_3_DTO_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .viewId(IDENTIFIER_3_VIEW_ID)
            .descriptions(List.of())
            .titles(List.of())
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_3_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .creators(List.of())
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .licenses(List.of(LICENSE_1_DTO))
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

    public final static Identifier IDENTIFIER_4 = Identifier.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .tableId(IDENTIFIER_4_TABLE_ID)
            .descriptions(List.of())
            .titles(List.of())
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
            .createdBy(USER_1_ID)
            .licenses(List.of(LICENSE_1))
            .creators(List.of())
            .build();

    public final static IdentifierDto IDENTIFIER_4_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .tableId(IDENTIFIER_4_TABLE_ID)
            .descriptions(List.of())
            .titles(List.of())
            .doi(IDENTIFIER_4_DOI)
            .created(IDENTIFIER_4_CREATED)
            .lastModified(IDENTIFIER_4_MODIFIED)
            .execution(IDENTIFIER_4_EXECUTION)
            .publicationDay(IDENTIFIER_4_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .resultHash(IDENTIFIER_4_RESULT_HASH)
            .resultNumber(IDENTIFIER_4_RESULT_NUMBER)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE_DTO)
            .creator(USER_1_DTO)
            .licenses(List.of(LICENSE_1_DTO))
            .creators(List.of())
            .build();

    public final static IdentifierSaveDto IDENTIFIER_4_DTO_REQUEST = IdentifierSaveDto.builder()
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .tableId(IDENTIFIER_4_TABLE_ID)
            .descriptions(List.of())
            .titles(List.of())
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .creators(List.of())
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE_DTO)
            .licenses(List.of(LICENSE_1_DTO))
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
    public final static Instant BANNER_MESSAGE_1_START = Instant.ofEpochSecond(1684577786L);
    public final static Instant BANNER_MESSAGE_1_END = null;

    public final static BannerMessage BANNER_MESSAGE_1 = BannerMessage.builder()
            .id(BANNER_MESSAGE_1_ID)
            .message(BANNER_MESSAGE_1_MESSAGE)
            .type(BANNER_MESSAGE_1_TYPE)
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
    public final static Instant BANNER_MESSAGE_2_START = Instant.ofEpochSecond(1671836400L);
    public final static Instant BANNER_MESSAGE_2_END = Instant.ofEpochSecond(1672009200L);

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
            .name(DATABASE_1_NAME)
            .description(DATABASE_1_DESCRIPTION)
            .identifiers(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4))
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .created(DATABASE_1_CREATED)
            .lastModified(DATABASE_1_LAST_MODIFIED)
            .createdBy(DATABASE_1_CREATOR)
            .creator(USER_1)
            .ownedBy(DATABASE_1_OWNER)
            .owner(USER_1)
            .contactPerson(USER_1_ID)
            .contact(USER_1)
            .tables(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_4))
            .views(List.of(VIEW_1, VIEW_2, VIEW_3))
            .accesses(List.of() /* set in junit tests */)
            .build();

    public final static DatabaseDto DATABASE_1_DTO = DatabaseDto.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .isPublic(DATABASE_1_PUBLIC)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .identifiers(List.of(IDENTIFIER_1_DTO, IDENTIFIER_2_DTO, IDENTIFIER_3_DTO, IDENTIFIER_4_DTO))
            .tables(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO))
            .views(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO))
            .build();

    public final static DatabaseAccess DATABASE_1_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_1_ID)
            .user(USER_1)
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

    public final static DatabaseAccess DATABASE_1_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .huserid(USER_2_ID)
            .user(USER_2)
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

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_2_CREATED)
            .lastModified(Instant.now())
            .isPublic(DATABASE_2_PUBLIC)
            .name(DATABASE_2_NAME)
            .description(DATABASE_2_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .identifiers(List.of(IDENTIFIER_5))
            .container(CONTAINER_1)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .created(DATABASE_2_CREATED)
            .lastModified(DATABASE_2_LAST_MODIFIED)
            .createdBy(DATABASE_2_CREATOR)
            .creator(USER_2)
            .ownedBy(DATABASE_2_OWNER)
            .owner(USER_2)
            .contactPerson(USER_2_ID)
            .contact(USER_2)
            .tables(List.of(TABLE_5, TABLE_6, TABLE_7))
            .views(List.of(VIEW_4))
            .accesses(List.of() /* set in junit tests */)
            .build();

    public final static DatabaseDto DATABASE_2_DTO = DatabaseDto.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_2_CREATED)
            .isPublic(DATABASE_2_PUBLIC)
            .name(DATABASE_2_NAME)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .identifiers(List.of(IDENTIFIER_5_DTO))
            .tables(List.of(TABLE_5_DTO, TABLE_6_DTO, TABLE_7_DTO))
            .views(List.of(VIEW_4_DTO))
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

    public final static DatabaseAccess DATABASE_2_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .database(DATABASE_2)
            .huserid(USER_3_ID)
            .user(USER_3)
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
            .name(DATABASE_3_NAME)
            .description(DATABASE_3_DESCRIPTION)
            .identifiers(List.of(IDENTIFIER_6))
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .created(DATABASE_3_CREATED)
            .lastModified(DATABASE_3_LAST_MODIFIED)
            .createdBy(DATABASE_3_CREATOR)
            .creator(USER_3)
            .ownedBy(DATABASE_3_OWNER)
            .owner(USER_3)
            .contactPerson(USER_3_ID)
            .contact(USER_3)
            .tables(List.of(TABLE_8))
            .views(List.of(VIEW_5))
            .accesses(List.of() /* set in junit tests */)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_1_ID)
            .user(USER_1)
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

    public final static DatabaseAccess DATABASE_3_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .database(DATABASE_3)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static Identifier IDENTIFIER_7 = Identifier.builder()
            .id(IDENTIFIER_7_ID)
            .databaseId(DATABASE_4_ID)
            .descriptions(List.of())
            .titles(List.of())
            .doi(IDENTIFIER_7_DOI)
            .created(IDENTIFIER_7_CREATED)
            .lastModified(IDENTIFIER_7_MODIFIED)
            .execution(IDENTIFIER_7_EXECUTION)
            .publicationDay(IDENTIFIER_7_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_7_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_7_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_7_QUERY_HASH)
            .resultHash(IDENTIFIER_7_RESULT_HASH)
            .query(IDENTIFIER_7_QUERY)
            .queryNormalized(IDENTIFIER_7_NORMALIZED)
            .resultNumber(IDENTIFIER_7_RESULT_NUMBER)
            .publisher(IDENTIFIER_7_PUBLISHER)
            .type(IDENTIFIER_7_TYPE)
            .createdBy(USER_4_ID)
            .licenses(List.of())
            .creators(List.of(IDENTIFIER_7_CREATOR_1))
            .funders(List.of())
            .build();

    public final static Database DATABASE_4 = Database.builder()
            .id(DATABASE_4_ID)
            .created(Instant.now().minus(4, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_4_PUBLIC)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .identifiers(List.of(IDENTIFIER_7))
            .cid(CONTAINER_4_ID)
            .container(CONTAINER_4)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .created(DATABASE_4_CREATED)
            .lastModified(DATABASE_4_LAST_MODIFIED)
            .createdBy(DATABASE_4_CREATOR)
            .creator(USER_4)
            .ownedBy(DATABASE_4_OWNER)
            .owner(USER_4)
            .contactPerson(USER_4_ID)
            .contact(USER_4)
            .tables(List.of())
            .views(List.of())
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

}
