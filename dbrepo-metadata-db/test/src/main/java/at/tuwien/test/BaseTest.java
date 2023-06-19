package at.tuwien.test;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageDateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.columns.concepts.*;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyCreateDto;
import at.tuwien.api.identifier.*;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageTypeDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.api.user.*;
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
import at.tuwien.entities.user.Realm;
import at.tuwien.entities.user.Role;
import at.tuwien.entities.user.User;
import at.tuwien.entities.user.UserAttribute;
import at.tuwien.querystore.Query;
import at.tuwien.utils.ArrayUtil;
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
 * Database 1 (Private, User 1)
 * <ul>
 * <li>Table 1</li>
 * <li>Table 2</li>
 * <li>Table 3</li>
 * <li>Table 7</li>
 * <li>Query 1</li>
 * <li>View 2</li>
 * <li>View 3</li>
 * </ul>
 * <p>
 * Database 2 (Private, User 2)
 * <ul>
 * <li>Table 4</li>
 * <li>Table 5</li>
 * <li>Table 6</li>
 * <li>Query 2</li>
 * <li>Query 6</li>
 * <li>View 4</li>
 * </ul>
 * <p>
 * Database 3 (Public, User 3)
 * <ul>
 * <li>Table 8</li>
 * <li>Query 3</li>
 * <li>Query 4</li>
 * <li>Query 5</li>
 * <li>View 5</li>
 * </ul>
 * <p>
 * Database 4 (Public, User 4)
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
            "delete-database-access", "check-database-access", "list-databases",
            "create-database-access", "find-database"};

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
            "list-tables", "create-table", "modify-table-column-semantics", "find-table"};

    public final static String[] ESCALATED_TABLE_HANDLING = new String[]{"escalated-table-handling",
            "delete-table"};

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
            .themeDark(true)
            .build();

    public final static UserThemeSetDto USER_THEME_LIGHT_DTO = UserThemeSetDto.builder()
            .themeDark(false)
            .build();

    public final static UUID REALM_DBREPO_ID = UUID.fromString("6264bf7b-d1d3-4562-9c07-ce4364a8f9d3");
    public final static String REALM_DBREPO_NAME = "dbrepo";
    public final static Boolean REALM_DBREPO_ENABLED = true;

    public final static Realm REALM_DBREPO = Realm.builder()
            .id(REALM_DBREPO_ID)
            .name(REALM_DBREPO_NAME)
            .enabled(REALM_DBREPO_ENABLED)
            .build();

    public final static UUID ROLE_DEFAULT_RESEARCHER_ROLES_ID = UUID.fromString("c74cbbe7-3ab1-4472-9211-cc9045672682");
    public final static String ROLE_DEFAULT_RESEARCHER_ROLES_NAME = "default-researcher-roles";
    public final static UUID ROLE_DEFAULT_RESEARCHER_ROLES_REALM_ID = REALM_DBREPO_ID;

    public final static Role ROLE_DEFAULT_RESEARCHER_ROLES = Role.builder()
            .id(ROLE_DEFAULT_RESEARCHER_ROLES_ID)
            .name(ROLE_DEFAULT_RESEARCHER_ROLES_NAME)
            .realmId(ROLE_DEFAULT_RESEARCHER_ROLES_REALM_ID)
            .build();

    public final static String USER_BROKER_USERNAME = "guest";
    public final static String USER_BROKER_PASSWORD = "guest";

    public final static User USER_BROKER = User.builder()
            .username(USER_BROKER_USERNAME)
            .build();

    public final static UUID USER_1_ID = UUID.fromString("cd5bab0d-7799-4069-85fb-c5d738572a0b");
    public final static String USER_1_EMAIL = "john.doe@example.com";
    public final static String USER_1_USERNAME = "junit1";
    public final static String USER_1_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_1_PASSWORD_ENCODED = "$2a$10$0dtdedA/RLTrFbUsvpbUw.I73AXOKeQP3t5UXj96OvnDEaDb3d3M6";
    public final static String USER_1_DATABASE_PASSWORD = "*440BA4FD1A87A0999647DB67C0EE258198B247BA" /* junit1 */;
    public final static String USER_1_FIRSTNAME = "John";
    public final static String USER_1_LASTNAME = "Doe";
    public final static String USER_1_AFFILIATION = "TU Graz";
    public final static String USER_1_ORCID = "000000034216302X";
    public final static String USER_1_ORCID_UNCOMPRESSED = "0000-0003-4216-302X";
    public final static String USER_1_TITLES_BEFORE = "Dr.";
    public final static String USER_1_TITLES_AFTER = "MSc BSc";
    public final static Boolean USER_1_VERIFIED = false;
    public final static Boolean USER_1_ENABLED = true;
    public final static Boolean USER_1_THEME_DARK = false;
    public final static Instant USER_1_CREATED = Instant.ofEpochSecond(1677399441) /* 2023-02-26 08:17:21 (UTC) */;
    public final static Instant USER_1_LAST_MODIFIED = USER_1_CREATED;
    public final static UUID USER_1_REALM_ID = REALM_DBREPO_ID;

    public final static CreateUserDto USER_1_RABBITMQ_CREATE_DTO = CreateUserDto.builder()
            .passwordHash("")
            .tags("")
            .build();

    public final static GrantVirtualHostPermissionsDto USER_1_RABBITMQ_GRANT_DTO = GrantVirtualHostPermissionsDto.builder()
            .configure("")
            .read("")
            .write("")
            .build();

    public final static List<UserAttribute> USER_1_ATTRIBUTES = List.of(UserAttribute.builder()
                    .id(UUID.fromString("c466a105-5bbd-4afc-87ae-751d5037d9ab"))
                    .userId(USER_1_ID)
                    .name("theme_dark")
                    .value("false")
                    .build(),
            UserAttribute.builder()
                    .id(UUID.fromString("0870498b-d6ac-4543-bef1-830142de96d7"))
                    .userId(USER_1_ID)
                    .name("orcid")
                    .value(USER_1_ORCID_UNCOMPRESSED)
                    .build(),
            UserAttribute.builder()
                    .id(UUID.fromString("42b06e7f-9df2-4b1c-bdfb-904401d6ad36"))
                    .userId(USER_1_ID)
                    .name("affiliation")
                    .value(USER_1_AFFILIATION)
                    .build());

    public final static List<UserAttributeDto> USER_1_ATTRIBUTES_DTO = List.of(UserAttributeDto.builder()
                    .id(UUID.fromString("c466a105-5bbd-4afc-87ae-751d5037d9ab"))
                    .userId(USER_1_ID)
                    .name("theme_dark")
                    .value("false")
                    .build(),
            UserAttributeDto.builder()
                    .id(UUID.fromString("0870498b-d6ac-4543-bef1-830142de96d7"))
                    .userId(USER_1_ID)
                    .name("orcid")
                    .value(USER_1_ORCID_UNCOMPRESSED)
                    .build(),
            UserAttributeDto.builder()
                    .id(UUID.fromString("42b06e7f-9df2-4b1c-bdfb-904401d6ad36"))
                    .userId(USER_1_ID)
                    .name("affiliation")
                    .value(USER_1_AFFILIATION)
                    .build());

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .databasePassword(USER_1_DATABASE_PASSWORD)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .emailVerified(USER_1_VERIFIED)
            .enabled(USER_1_ENABLED)
            .realmId(USER_1_REALM_ID)
            .attributes(USER_1_ATTRIBUTES)
            .build();

    public final static User USER_1_SIMPLE = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .databasePassword(USER_1_DATABASE_PASSWORD)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .emailVerified(USER_1_VERIFIED)
            .enabled(USER_1_ENABLED)
            .realmId(USER_1_REALM_ID)
            .attributes(List.of() /* for jpa */)
            .build();

    public final static UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .emailVerified(USER_1_VERIFIED)
            .attributes(USER_1_ATTRIBUTES_DTO)
            .build();

    public final static UserBriefDto USER_1_BRIEF_DTO = UserBriefDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .emailVerified(USER_1_VERIFIED)
            .build();

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
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
    public final static String USER_2_AFFILIATION = "TU Wien";
    public final static String USER_2_ORCID = "0000000292726225";
    public final static String USER_2_ORCID_UNCOMPRESSED = "0000-0002-9272-6225";
    public final static String USER_2_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_2_DATABASE_PASSWORD = "*9AA70A8B0EEFAFCB5BED5BDEF6EE264D5DA915AE" /* junit2 */;
    public final static Boolean USER_2_VERIFIED = true;
    public final static Boolean USER_2_ENABLED = true;
    public final static Boolean USER_2_THEME_DARK = false;
    public final static Instant USER_2_CREATED = Instant.ofEpochSecond(1677399528) /* 2023-02-26 08:18:48 (UTC) */;
    public final static Instant USER_2_LAST_MODIFIED = USER_1_CREATED;
    public final static UUID USER_2_REALM_ID = REALM_DBREPO_ID;

    public final static List<UserAttribute> USER_2_ATTRIBUTES = List.of(UserAttribute.builder()
                    .id(UUID.fromString("23da2c08-cb8a-4e18-a7f0-70c30de2771e"))
                    .userId(USER_2_ID)
                    .name("theme_dark")
                    .value("false")
                    .build(),
            UserAttribute.builder()
                    .id(UUID.fromString("83223dfd-1c80-4132-8c74-a38994f45f4a"))
                    .userId(USER_2_ID)
                    .name("orcid")
                    .value(USER_2_ORCID_UNCOMPRESSED)
                    .build(),
            UserAttribute.builder()
                    .id(UUID.fromString("a29879fd-9801-4adf-bf3a-16bbff6ea207"))
                    .userId(USER_2_ID)
                    .name("affiliation")
                    .value(USER_2_AFFILIATION)
                    .build());

    public final static List<UserAttributeDto> USER_2_ATTRIBUTES_DTO = List.of(UserAttributeDto.builder()
                    .id(UUID.fromString("23da2c08-cb8a-4e18-a7f0-70c30de2771e"))
                    .userId(USER_2_ID)
                    .name("theme_dark")
                    .value("false")
                    .build(),
            UserAttributeDto.builder()
                    .id(UUID.fromString("83223dfd-1c80-4132-8c74-a38994f45f4a"))
                    .userId(USER_2_ID)
                    .name("orcid")
                    .value(USER_2_ORCID_UNCOMPRESSED)
                    .build(),
            UserAttributeDto.builder()
                    .id(UUID.fromString("a29879fd-9801-4adf-bf3a-16bbff6ea207"))
                    .userId(USER_2_ID)
                    .name("affiliation")
                    .value(USER_2_AFFILIATION)
                    .build());

    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(true)
            .databasePassword(USER_2_DATABASE_PASSWORD)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .emailVerified(USER_2_VERIFIED)
            .enabled(USER_2_ENABLED)
            .realmId(USER_2_REALM_ID)
            .attributes(USER_2_ATTRIBUTES)
            .build();

    public final static User USER_2_SIMPLE = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(true)
            .databasePassword(USER_2_DATABASE_PASSWORD)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .emailVerified(USER_2_VERIFIED)
            .enabled(USER_2_ENABLED)
            .realmId(USER_2_REALM_ID)
            .attributes(List.of() /* for jpa */)
            .build();

    public final static UserDto USER_2_DTO = UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .emailVerified(USER_2_VERIFIED)
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
    public final static String USER_3_AFFILIATION = "TU Wien";
    public final static String USER_3_ORCID = null;
    public final static String USER_3_EMAIL = "system@example.com";
    public final static String USER_3_PASSWORD = "password";
    public final static String USER_3_DATABASE_PASSWORD = "*D65FCA043964B63E849DD6334699ECB065905DA4" /* junit3 */;
    public final static Boolean USER_3_VERIFIED = true;
    public final static Boolean USER_3_ENABLED = true;
    public final static Boolean USER_3_THEME_DARK = false;
    public final static Instant USER_3_CREATED = Instant.ofEpochSecond(1677399559) /* 2023-02-26 08:19:19 (UTC) */;
    public final static UUID USER_3_REALM_ID = REALM_DBREPO_ID;

    public final static List<UserAttribute> USER_3_ATTRIBUTES = List.of(UserAttribute.builder()
                    .id(UUID.fromString("58062219-7b99-4c0d-b00b-136b7d916c04"))
                    .userId(USER_3_ID)
                    .name("theme_dark")
                    .value(USER_3_THEME_DARK.toString())
                    .build(),
            UserAttribute.builder()
                    .id(UUID.fromString("384851ee-83c4-4cda-805e-be0c1bab71eb"))
                    .userId(USER_3_ID)
                    .name("orcid")
                    .value(null)
                    .build(),
            UserAttribute.builder()
                    .id(UUID.fromString("c2cb2357-5e34-453f-b080-ca1c97f56d4a"))
                    .userId(USER_3_ID)
                    .name("affiliation")
                    .value(USER_3_AFFILIATION)
                    .build());

    public final static List<UserAttributeDto> USER_3_ATTRIBUTES_DTO = List.of(UserAttributeDto.builder()
                    .id(UUID.fromString("58062219-7b99-4c0d-b00b-136b7d916c04"))
                    .userId(USER_3_ID)
                    .name("theme_dark")
                    .value(USER_3_THEME_DARK.toString())
                    .build(),
            UserAttributeDto.builder()
                    .id(UUID.fromString("384851ee-83c4-4cda-805e-be0c1bab71eb"))
                    .userId(USER_3_ID)
                    .name("orcid")
                    .value(null)
                    .build(),
            UserAttributeDto.builder()
                    .id(UUID.fromString("c2cb2357-5e34-453f-b080-ca1c97f56d4a"))
                    .userId(USER_3_ID)
                    .name("affiliation")
                    .value(USER_3_AFFILIATION)
                    .build());

    public final static User USER_3 = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .emailVerified(true)
            .databasePassword(USER_3_DATABASE_PASSWORD)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .emailVerified(USER_3_VERIFIED)
            .enabled(USER_3_ENABLED)
            .realmId(USER_3_REALM_ID)
            .attributes(USER_3_ATTRIBUTES)
            .build();

    public final static User USER_3_SIMPLE = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .emailVerified(true)
            .databasePassword(USER_3_DATABASE_PASSWORD)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .emailVerified(USER_3_VERIFIED)
            .enabled(USER_3_ENABLED)
            .realmId(USER_3_REALM_ID)
            .attributes(List.of() /* for jpa */)
            .build();

    public final static UserDto USER_3_DTO = UserDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .emailVerified(USER_3_VERIFIED)
            .build();

    public final static UserDetails USER_3_DETAILS = UserDetailsDto.builder()
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_DATA_STEWARD_AUTHORITIES)
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
    public final static String USER_4_AFFILIATION = "TU Wien";
    public final static String USER_4_ORCID = null;
    public final static String USER_4_PASSWORD = "junit4";
    public final static String USER_4_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit4 */;
    public final static String USER_4_EMAIL = "junit4@ossdip.at";
    public final static Boolean USER_4_VERIFIED = true;
    public final static Boolean USER_4_ENABLED = true;
    public final static Boolean USER_4_THEME_DARK = false;
    public final static Instant USER_4_CREATED = Instant.ofEpochSecond(1677399592) /* 2023-02-26 08:19:52 (UTC) */;
    public final static UUID USER_4_REALM_ID = REALM_DBREPO_ID;

    public final static User USER_4 = User.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .emailVerified(USER_4_VERIFIED)
            .databasePassword(USER_4_DATABASE_PASSWORD)
            .enabled(USER_4_ENABLED)
            .realmId(USER_4_REALM_ID)
            .build();

    public final static UserDto USER_4_DTO = UserDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .emailVerified(USER_4_VERIFIED)
            .build();

    public final static UserDetails USER_4_DETAILS = UserDetailsDto.builder()
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
    public final static String USER_5_AFFILIATION = "TU Wien";
    public final static String USER_5_ORCID = null;
    public final static String USER_5_PASSWORD = "junit5";
    public final static String USER_5_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit5 */;
    public final static String USER_5_EMAIL = "system@ossdip.at";
    public final static Boolean USER_5_VERIFIED = true;
    public final static Boolean USER_5_THEME_DARK = false;
    public final static Instant USER_5_CREATED = Instant.ofEpochSecond(1677399592) /* 2023-02-26 08:19:52 (UTC) */;
    public final static UUID USER_5_REALM_ID = REALM_DBREPO_ID;

    public final static User USER_5 = User.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .email(USER_5_EMAIL)
            .emailVerified(USER_5_VERIFIED)
            .databasePassword(USER_5_DATABASE_PASSWORD)
            .realmId(USER_5_REALM_ID)
            .build();

    public final static UserDto USER_5_DTO = UserDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .email(USER_5_EMAIL)
            .firstname(USER_5_FIRSTNAME)
            .lastname(USER_5_LASTNAME)
            .emailVerified(USER_5_VERIFIED)
            .build();

    public final static UserDetails USER_5_DETAILS = UserDetailsDto.builder()
            .username(USER_5_USERNAME)
            .email(USER_5_EMAIL)
            .password(USER_5_PASSWORD)
            .authorities(AUTHORITY_DEFAULT_RESEARCHER_AUTHORITIES)
            .build();

    public final static Principal USER_5_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_5_DETAILS,
            USER_5_PASSWORD, USER_5_DETAILS.getAuthorities());

    public final static Long IMAGE_1_ID = 1L;
    public final static String IMAGE_1_REGISTRY = "docker.io/library";
    public final static String IMAGE_1_NAME = "mariadb";
    public final static String IMAGE_1_VERSION = "10.5";
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

    public final static ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_PORT)
            .dateFormats(List.of(IMAGE_DATE_1, IMAGE_DATE_2, IMAGE_DATE_3))
            .build();

    public final static ContainerImage IMAGE_1_SIMPLE = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .name(IMAGE_1_NAME)
            .version(IMAGE_1_VERSION)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .defaultPort(IMAGE_1_PORT)
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

    public final static Long IMAGE_2_ID = 2L;
    public final static String IMAGE_2_NAME = "mariadb";
    public final static String IMAGE_2_VERSION = "8.0";
    public final static Integer IMAGE_2_PORT = 3306;
    public final static String IMAGE_2_DIALECT = "org.hibernate.dialect.MySQLDialect";
    public final static String IMAGE_2_DRIVER = "com.mysql.jdbc.Driver";
    public final static String IMAGE_2_JDBC = "mysql";
    public final static Long IMAGE_2_SIZE = 12000L;
    public final static Instant IMAGE_2_BUILT = Instant.now().minus(38, HOURS);


    public final static Long CONTAINER_1_ID = 1L;
    public final static ContainerImage CONTAINER_1_IMAGE = IMAGE_1;
    public final static ImageBriefDto CONTAINER_1_IMAGE_BRIEF_DTO = IMAGE_1_BRIEF_DTO;
    public final static String CONTAINER_1_NAME = "u01";
    public final static String CONTAINER_1_INTERNALNAME = "dbrepo-userdb-u01";
    public final static String CONTAINER_1_IP = "127.0.0.1";
    public final static Integer CONTAINER_1_PORT = 3308;
    public final static String CONTAINER_1_PRIVILEGED_USERNAME = "root";
    public final static String CONTAINER_1_PRIVILEGED_PASSWORD = "dbrepo";
    public final static Instant CONTAINER_1_CREATED = Instant.ofEpochSecond(1677399629) /* 2023-02-26 08:20:29 (UTC) */;

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_1_IMAGE)
            .created(CONTAINER_1_CREATED)
            .host(CONTAINER_1_IP)
            .port(CONTAINER_1_PORT)
            .privilegedUsername(CONTAINER_1_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_1_PRIVILEGED_PASSWORD)
            .build();

    public final static Container CONTAINER_1_SIMPLE = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(null /* for jpa */)
            .created(CONTAINER_1_CREATED)
            .host(CONTAINER_1_IP)
            .port(CONTAINER_1_PORT)
            .privilegedUsername(CONTAINER_1_PRIVILEGED_USERNAME)
            .privilegedPassword(CONTAINER_1_PRIVILEGED_PASSWORD)
            .build();

    public final static ContainerDto CONTAINER_1_DTO = ContainerDto.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .image(CONTAINER_1_IMAGE_BRIEF_DTO)
            .created(CONTAINER_1_CREATED)
            .host(CONTAINER_1_IP)
            .owner(USER_1_BRIEF_DTO)
            .build();

    public final static Long CONTAINER_2_ID = 2L;
    public final static ContainerImage CONTAINER_2_IMAGE = IMAGE_1;
    public final static String CONTAINER_2_NAME = "u02";
    public final static String CONTAINER_2_INTERNALNAME = "dbrepo-userdb-u02";
    public final static String CONTAINER_2_IP = "172.30.0.6";
    public final static Instant CONTAINER_2_CREATED = Instant.ofEpochSecond(1677399655) /* 2023-02-26 08:20:55 (UTC) */;

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_2_IMAGE)
            .created(CONTAINER_2_CREATED)
            .host(CONTAINER_2_IP)
            .build();

    public final static Container CONTAINER_2_SIMPLE = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_2_IMAGE)
            .created(CONTAINER_2_CREATED)
            .host(CONTAINER_2_IP)
            .build();

    public final static Long CONTAINER_3_ID = 3L;
    public final static ContainerImage CONTAINER_3_IMAGE = IMAGE_1;
    public final static String CONTAINER_3_NAME = "u03";
    public final static String CONTAINER_3_INTERNALNAME = "dbrepo-userdb-u03";
    public final static String CONTAINER_3_IP = "172.30.0.7";
    public final static Instant CONTAINER_3_CREATED = Instant.ofEpochSecond(1677399672) /* 2023-02-26 08:21:12 (UTC) */;

    public final static Container CONTAINER_3 = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .internalName(CONTAINER_3_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_3_IMAGE)
            .created(CONTAINER_3_CREATED)
            .host(CONTAINER_3_IP)
            .build();

    public final static Container CONTAINER_3_SIMPLE = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .internalName(CONTAINER_3_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_3_IMAGE)
            .created(CONTAINER_3_CREATED)
            .host(CONTAINER_3_IP)
            .build();

    public final static Long CONTAINER_4_ID = 4L;
    public final static ContainerImage CONTAINER_4_IMAGE = IMAGE_1;
    public final static String CONTAINER_4_NAME = "u04";
    public final static String CONTAINER_4_INTERNALNAME = "dbrepo-userdb-u04";
    public final static String CONTAINER_4_IP = "172.30.0.8";
    public final static Instant CONTAINER_4_CREATED = Instant.ofEpochSecond(1677399688) /* 2023-02-26 08:21:28 (UTC) */;

    public final static Container CONTAINER_4 = Container.builder()
            .id(CONTAINER_4_ID)
            .name(CONTAINER_4_NAME)
            .internalName(CONTAINER_4_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_4_IMAGE)
            .created(CONTAINER_4_CREATED)
            .host(CONTAINER_4_IP)
            .build();

    public final static Container CONTAINER_4_SIMPLE = Container.builder()
            .id(CONTAINER_4_ID)
            .name(CONTAINER_4_NAME)
            .internalName(CONTAINER_4_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_4_IMAGE)
            .created(CONTAINER_4_CREATED)
            .host(CONTAINER_4_IP)
            .build();


    public final static Long DATABASE_1_ID = 1L;
    public final static String DATABASE_1_NAME = "Weather";
    public final static String DATABASE_1_DESCRIPTION = "Weather in Australia";
    public final static String DATABASE_1_INTERNALNAME = "weather";
    public final static Boolean DATABASE_1_PUBLIC = false;
    public final static String DATABASE_1_EXCHANGE = "dbrepo." + DATABASE_1_INTERNALNAME;
    public final static Instant DATABASE_1_CREATED = Instant.ofEpochSecond(1677399741) /* 2023-02-26 08:22:21 (UTC) */;
    public final static Instant DATABASE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399741) /* 2023-02-26 08:22:21 (UTC) */;
    public final static User DATABASE_1_OWNER = USER_1;
    public final static User DATABASE_1_CREATOR = USER_1;

    public final static Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_1_PUBLIC)
            .name(DATABASE_1_NAME)
            .description(DATABASE_1_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .created(DATABASE_1_CREATED)
            .lastModified(DATABASE_1_LAST_MODIFIED)
            .createdBy(USER_1_ID)
            .ownedBy(USER_1_ID)
            .contactPerson(USER_1_ID)
            .contact(USER_1)
            .creator(DATABASE_1_CREATOR)
            .owner(DATABASE_1_OWNER)
            .tables(List.of()) /* TABLE_1, TABLE_2, TABLE_3, TABLE_7 */
            .views(List.of()) /* VIEW_2, VIEW_3 */
            .build();

    public final static Database DATABASE_1_SIMPLE = Database.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_1_PUBLIC)
            .name(DATABASE_1_NAME)
            .description(DATABASE_1_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(null /* for jpa */)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .created(DATABASE_1_CREATED)
            .lastModified(DATABASE_1_LAST_MODIFIED)
            .createdBy(USER_1_ID)
            .ownedBy(USER_1_ID)
            .contactPerson(USER_1_ID)
            .contact(null /* for jpa */)
            .creator(null /* for jpa */)
            .owner(null /* for jpa */)
            .tables(List.of() /* for jpa */)
            .views(List.of() /* for jpa */)
            .build();

    public final static DatabaseDto DATABASE_1_DTO = DatabaseDto.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .isPublic(DATABASE_1_PUBLIC)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .tables(List.of()) /* TABLE_1, TABLE_2, TABLE_3, TABLE_7 */
            .views(List.of())
            .build();

    public final static DatabaseAccess DATABASE_1_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_1_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
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
    public final static String DATABASE_2_EXCHANGE = "dbrepo." + DATABASE_2_INTERNALNAME;
    public final static Instant DATABASE_2_CREATED = Instant.ofEpochSecond(1677399772) /* 2023-02-26 08:22:52 (UTC) */;
    public final static Instant DATABASE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677399772) /* 2023-02-26 08:22:52 (UTC) */;
    public final static User DATABASE_2_OWNER = USER_2;
    public final static User DATABASE_2_CREATOR = USER_2;

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_2_CREATED)
            .lastModified(Instant.now())
            .isPublic(DATABASE_2_PUBLIC)
            .name(DATABASE_2_NAME)
            .description(DATABASE_2_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .created(DATABASE_2_CREATED)
            .lastModified(DATABASE_2_LAST_MODIFIED)
            .createdBy(USER_2_ID)
            .contactPerson(USER_2_ID)
            .contact(USER_2)
            .ownedBy(USER_2_ID)
            .creator(DATABASE_2_CREATOR)
            .owner(DATABASE_2_OWNER)
            .tables(List.of()) /* TABLE_4, TABLE_5, TABLE_6 */
            .views(List.of()) /* VIEW_4 */
            .build();

    public final static Database DATABASE_2_SIMPLE = Database.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_2_CREATED)
            .lastModified(Instant.now())
            .isPublic(DATABASE_2_PUBLIC)
            .name(DATABASE_2_NAME)
            .description(DATABASE_2_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(null /* for jpa */)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .created(DATABASE_2_CREATED)
            .lastModified(DATABASE_2_LAST_MODIFIED)
            .createdBy(USER_2_ID)
            .ownedBy(USER_2_ID)
            .contactPerson(USER_1_ID)
            .contact(null /* for jpa */)
            .creator(null /* for jpa */)
            .owner(null /* for jpa */)
            .tables(List.of() /* for jpa */)
            .views(List.of() /* for jpa */)
            .build();

    public final static DatabaseDto DATABASE_2_DTO = DatabaseDto.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_2_CREATED)
            .isPublic(DATABASE_2_PUBLIC)
            .name(DATABASE_2_NAME)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .tables(List.of()) /* TABLE_2, TABLE_2, TABLE_3 */
            .views(List.of())
            .build();

    public final static DatabaseAccess DATABASE_2_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_2_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

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
    public final static String DATABASE_3_EXCHANGE = "dbrepo." + DATABASE_3_INTERNALNAME;
    public final static Instant DATABASE_3_CREATED = Instant.ofEpochSecond(1677399792) /* 2023-02-26 08:23:12 (UTC) */;
    public final static Instant DATABASE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677399792) /* 2023-02-26 08:23:12 (UTC) */;
    public final static User DATABASE_3_OWNER = USER_3;
    public final static User DATABASE_3_CREATOR = USER_3;

    public final static Database DATABASE_3 = Database.builder()
            .id(DATABASE_3_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_3_PUBLIC)
            .name(DATABASE_3_NAME)
            .description(DATABASE_3_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(CONTAINER_1)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .created(DATABASE_3_CREATED)
            .lastModified(DATABASE_3_LAST_MODIFIED)
            .contactPerson(USER_3_ID)
            .contact(USER_3)
            .createdBy(USER_3_ID)
            .ownedBy(USER_3_ID)
            .creator(DATABASE_3_CREATOR)
            .owner(DATABASE_3_OWNER)
            .tables(List.of()) /* TABLE_8 */
            .views(List.of()) /* VIEW_5 */
            .build();

    public final static Database DATABASE_3_SIMPLE = Database.builder()
            .id(DATABASE_3_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_3_PUBLIC)
            .name(DATABASE_3_NAME)
            .description(DATABASE_3_DESCRIPTION)
            .cid(CONTAINER_1_ID)
            .container(null /* for jpa */)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .created(DATABASE_3_CREATED)
            .lastModified(DATABASE_3_LAST_MODIFIED)
            .contactPerson(USER_3_ID)
            .contact(null /* for jpa */)
            .createdBy(USER_3_ID)
            .ownedBy(USER_3_ID)
            .creator(null /* for jpa */)
            .owner(null /* for jpa */)
            .tables(List.of() /* for jpa */)
            .views(List.of() /* for jpa */)
            .build();

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

    public final static DatabaseAccess DATABASE_3_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_3_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
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
    public final static String DATABASE_4_EXCHANGE = "dbrepo." + DATABASE_4_INTERNALNAME;
    public final static Instant DATABASE_4_CREATED = Instant.ofEpochSecond(1677399813) /* 2023-02-26 08:23:33 (UTC) */;
    public final static Instant DATABASE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677399813) /* 2023-02-26 08:23:33 (UTC) */;

    public final static Database DATABASE_4 = Database.builder()
            .id(DATABASE_4_ID)
            .created(Instant.now().minus(4, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_4_PUBLIC)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .cid(CONTAINER_4_ID)
            .container(CONTAINER_4)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .created(DATABASE_4_CREATED)
            .lastModified(DATABASE_4_LAST_MODIFIED)
            .contactPerson(USER_4_ID)
            .contact(USER_4)
            .createdBy(USER_4_ID)
            .ownedBy(USER_4_ID)
            .creator(USER_4)
            .owner(USER_4)
            .tables(List.of())
            .views(List.of())
            .build();

    public final static Database DATABASE_4_SIMPLE = Database.builder()
            .id(DATABASE_4_ID)
            .created(Instant.now().minus(4, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_4_PUBLIC)
            .name(DATABASE_4_NAME)
            .description(DATABASE_4_DESCRIPTION)
            .cid(CONTAINER_4_ID)
            .container(CONTAINER_4)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .created(DATABASE_4_CREATED)
            .lastModified(DATABASE_4_LAST_MODIFIED)
            .contactPerson(USER_4_ID)
            .contact(null /* for jpa */)
            .createdBy(USER_4_ID)
            .ownedBy(USER_4_ID)
            .creator(null /* for jpa */)
            .owner(null /* for jpa */)
            .tables(List.of() /* for jpa */)
            .views(List.of() /* for jpa */)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_1_ID)
            .user(USER_1)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_2_ID)
            .user(USER_2)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static DatabaseAccess DATABASE_4_USER_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_4_ID)
            .huserid(USER_3_ID)
            .user(USER_3)
            .build();

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "Weather AUS";
    public final static String TABLE_1_INTERNALNAME = "weather_aus";
    public final static String TABLE_1_DESCRIPTION = "Weather in the world";
    public final static String TABLE_1_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_1_INTERNALNAME;
    public final static String TABLE_1_ROUTING_KEY = TABLE_1_QUEUE_NAME;
    public final static UUID TABLE_1_CREATED_BY = USER_1_ID;
    public final static Long TABLE_1_DATABASE_ID = DATABASE_1_ID;
    public final static Instant TABLE_1_CREATED = Instant.ofEpochSecond(1677399975) /* 2023-02-26 08:26:15 (UTC) */;
    public final static Instant TABLE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399975) /* 2023-02-26 08:26:15 (UTC) */;

    public final static Long TABLE_2_ID = 2L;
    public final static String TABLE_2_NAME = "Weather Location";
    public final static String TABLE_2_INTERNALNAME = "weather_location";
    public final static String TABLE_2_DESCRIPTION = "Weather location";
    public final static String TABLE_2_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_2_INTERNALNAME;
    public final static String TABLE_2_ROUTING_KEY = TABLE_2_QUEUE_NAME;
    public final static UUID TABLE_2_CREATED_BY = USER_1_ID;
    public final static Long TABLE_2_DATABASE_ID = DATABASE_1_ID;
    public final static Instant TABLE_2_CREATED = Instant.ofEpochSecond(1677400007) /* 2023-02-26 08:26:47 (UTC) */;
    public final static Instant TABLE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677400007) /* 2023-02-26 08:26:47 (UTC) */;

    public final static Long TABLE_3_ID = 3L;
    public final static String TABLE_3_NAME = "Traffic Zürich";
    public final static String TABLE_3_INTERNALNAME = "traffic_zurich";
    public final static String TABLE_3_DESCRIPTION = "https://www.kaggle.com/laa283/zurich-public-transport/version/2";
    public final static String TABLE_3_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_3_INTERNALNAME;
    public final static String TABLE_3_ROUTING_KEY = TABLE_3_QUEUE_NAME;
    public final static UUID TABLE_3_CREATED_BY = USER_1_ID;
    public final static Long TABLE_3_DATABASE_ID = DATABASE_1_ID;
    public final static Instant TABLE_3_CREATED = Instant.ofEpochSecond(1677400031) /* 2023-02-26 08:27:11 (UTC) */;
    public final static Instant TABLE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677400031) /* 2023-02-26 08:27:11 (UTC) */;

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

    public final static Long TABLE_4_ID = 4L;
    public final static String TABLE_4_NAME = "zoo";
    public final static String TABLE_4_INTERNALNAME = "zoo";
    public final static String TABLE_4_DESCRIPTION = "Some Kaggle dataset";
    public final static String TABLE_4_QUEUE_NAME = DATABASE_2_EXCHANGE + "." + TABLE_4_INTERNALNAME;
    public final static String TABLE_4_ROUTING_KEY = TABLE_4_QUEUE_NAME;
    public final static Instant TABLE_4_CREATED = Instant.ofEpochSecond(1677400067) /* 2023-02-26 08:27:47 (UTC) */;
    public final static Instant TABLE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677400067) /* 2023-02-26 08:27:47 (UTC) */;

    public final static TableCsvDto TABLE_4_CSV_DTO = TableCsvDto.builder()
            .data(new HashMap<>() {{
                put("id", "102");
            }})
            .build();

    public final static Long TABLE_5_ID = 5L;
    public final static String TABLE_5_NAME = "names";
    public final static String TABLE_5_INTERNALNAME = "names";
    public final static String TABLE_5_DESCRIPTION = "Some names dataset";
    public final static String TABLE_5_QUEUE_NAME = DATABASE_2_EXCHANGE + "." + TABLE_5_INTERNALNAME;
    public final static String TABLE_5_ROUTING_KEY = TABLE_5_QUEUE_NAME;
    public final static Instant TABLE_5_CREATED = Instant.ofEpochSecond(1677400117) /* 2023-02-26 08:28:37 (UTC) */;
    public final static Instant TABLE_5_LAST_MODIFIED = Instant.ofEpochSecond(1677400117) /* 2023-02-26 08:28:37 (UTC) */;

    public final static Long TABLE_6_ID = 6L;
    public final static String TABLE_6_NAME = "likes";
    public final static String TABLE_6_INTERNAL_NAME = "likes";
    public final static String TABLE_6_DESCRIPTION = "Some likes dataset";
    public final static String TABLE_6_QUEUE_NAME = DATABASE_2_EXCHANGE + "." + TABLE_6_INTERNAL_NAME;
    public final static String TABLE_6_ROUTING_KEY = TABLE_6_QUEUE_NAME;
    public final static Instant TABLE_6_CREATED = Instant.ofEpochSecond(1677400147) /* 2023-02-26 08:29:07 (UTC) */;
    public final static Instant TABLE_6_LAST_MODIFIED = Instant.ofEpochSecond(1677400147) /* 2023-02-26 08:29:07 (UTC) */;

    public final static Long TABLE_7_ID = 4L;
    public final static String TABLE_7_NAME = "Sensor";
    public final static String TABLE_7_INTERNAL_NAME = "sensor";
    public final static String TABLE_7_DESCRIPTION = "Hello sensor";
    public final static String TABLE_7_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_7_INTERNAL_NAME;
    public final static String TABLE_7_ROUTING_KEY = TABLE_7_QUEUE_NAME;
    public final static Instant TABLE_7_CREATED = Instant.ofEpochSecond(1677400175) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_7_LAST_MODIFIED = Instant.ofEpochSecond(1677400175) /* 2023-02-26 08:29:35 (UTC) */;

    public final static List<TableColumn> TABLE_7_COLUMNS = List.of(TableColumn.builder()
                    .id(44L)
                    .ordinalPosition(0)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_7_ID)
                    .name("Timestamp")
                    .internalName("timestamp")
                    .columnType(TableColumnType.TIMESTAMP)
                    .dfid(IMAGE_DATE_3_ID)
                    .isNullAllowed(false)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(45L)
                    .ordinalPosition(1)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_7_ID)
                    .name("Value")
                    .internalName("value")
                    .columnType(TableColumnType.DECIMAL)
                    .dfid(null)
                    .isNullAllowed(true)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .creator(USER_1)
                    .build());

    public final static Table TABLE_7 = Table.builder()
            .id(TABLE_7_ID)
            .created(Instant.now())
            .internalName(TABLE_7_INTERNAL_NAME)
            .description(TABLE_7_DESCRIPTION)
            .database(DATABASE_1)
            .name(TABLE_7_NAME)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_7_QUEUE_NAME)
            .routingKey(TABLE_7_ROUTING_KEY)
            .columns(TABLE_7_COLUMNS)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_7_CREATED)
            .lastModified(TABLE_7_LAST_MODIFIED)
            .build();

    public final static Table TABLE_7_SIMPLE = Table.builder()
            .id(TABLE_7_ID)
            .created(Instant.now())
            .internalName(TABLE_7_INTERNAL_NAME)
            .description(TABLE_7_DESCRIPTION)
            .database(DATABASE_1_SIMPLE)
            .name(TABLE_7_NAME)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_7_QUEUE_NAME)
            .routingKey(TABLE_7_ROUTING_KEY)
            .columns(List.of() /* for jpa */)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_7_CREATED)
            .lastModified(TABLE_7_LAST_MODIFIED)
            .build();

    public final static Long TABLE_8_ID = 8L;
    public final static String TABLE_8_NAME = "mfcc";
    public final static String TABLE_8_INTERNAL_NAME = "mfcc";
    public final static String TABLE_8_DESCRIPTION = "Hello mfcc";
    public final static String TABLE_8_QUEUE_NAME = DATABASE_3_EXCHANGE + "." + TABLE_8_INTERNAL_NAME;
    public final static String TABLE_8_ROUTING_KEY = TABLE_8_QUEUE_NAME;
    public final static Instant TABLE_8_CREATED = Instant.ofEpochSecond(1688400185) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_8_LAST_MODIFIED = Instant.ofEpochSecond(1688400185) /* 2023-02-26 08:29:35 (UTC) */;

    public final static TableCsvDto TABLE_8_CSV_DTO = TableCsvDto.builder()
            .data(new HashMap<>() {{
                put("value", "2.1");
            }})
            .build();

    public final static Long ONTOLOGY_1_ID = 1L;
    public final static String ONTOLOGY_1_PREFIX = "om2";
    public final static String ONTOLOGY_1_NEW_PREFIX = "om-2";
    public final static String ONTOLOGY_1_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/";
    public final static String ONTOLOGY_1_SPARQL_ENDPOINT = null;
    public final static UUID ONTOLOGY_1_CREATED_BY = USER_1_ID;

    public final static Ontology ONTOLOGY_1 = Ontology.builder()
            .id(ONTOLOGY_1_ID)
            .prefix(ONTOLOGY_1_PREFIX)
            .uri(ONTOLOGY_1_URI)
            .sparqlEndpoint(ONTOLOGY_1_SPARQL_ENDPOINT)
            .createdBy(ONTOLOGY_1_CREATED_BY)
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
            .createdBy(ONTOLOGY_2_CREATED_BY)
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
            .createdBy(ONTOLOGY_3_CREATED_BY)
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
            .createdBy(ONTOLOGY_4_CREATED_BY)
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
            .createdBy(ONTOLOGY_5_CREATED_BY)
            .build();

    public final static OntologyCreateDto ONTOLOGY_5_CREATE_DTO = OntologyCreateDto.builder()
            .prefix(ONTOLOGY_5_PREFIX)
            .uri(ONTOLOGY_5_URI)
            .sparqlEndpoint(ONTOLOGY_5_SPARQL_ENDPOINT)
            .build();

    public final static String COLUMN_CONCEPT_TEMPERATURE_NAME = "temperature";
    public final static String COLUMN_CONCEPT_TEMPERATURE_URI = "http://www.wikidata.org/entity/Q11466";
    public final static String COLUMN_CONCEPT_TEMPERATURE_DESCRIPTION = "physical property of matter that quantitatively expresses the common notions of hot and cold";
    public final static Instant COLUMN_CONCEPT_TEMPERATURE_CREATED = Instant.now();

    public final static ConceptSaveDto COLUMN_CONCEPT_TEMPERATURE_SAVE_DTO = ConceptSaveDto.builder()
            .uri(COLUMN_CONCEPT_TEMPERATURE_URI)
            .name(COLUMN_CONCEPT_TEMPERATURE_NAME)
            .description(COLUMN_CONCEPT_TEMPERATURE_DESCRIPTION)
            .build();

    public final static ConceptDto COLUMN_CONCEPT_TEMPERATURE_DTO = ConceptDto.builder()
            .uri(COLUMN_CONCEPT_TEMPERATURE_URI)
            .name(COLUMN_CONCEPT_TEMPERATURE_NAME)
            .description(COLUMN_CONCEPT_TEMPERATURE_DESCRIPTION)
            .build();

    public final static TableColumnConcept COLUMN_CONCEPT_TEMPERATURE = TableColumnConcept.builder()
            .uri(COLUMN_CONCEPT_TEMPERATURE_URI)
            .name(COLUMN_CONCEPT_TEMPERATURE_NAME)
            .description(COLUMN_CONCEPT_TEMPERATURE_DESCRIPTION)
            .created(COLUMN_CONCEPT_TEMPERATURE_CREATED)
            .build();

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
            .uri(COLUMN_CONCEPT_FAIR_DATA_URI)
            .name(COLUMN_CONCEPT_FAIR_DATA_NAME)
            .description(COLUMN_CONCEPT_FAIR_DATA_DESCRIPTION)
            .build();

    public final static TableColumnConcept COLUMN_CONCEPT_FAIR_DATA = TableColumnConcept.builder()
            .uri(COLUMN_CONCEPT_FAIR_DATA_URI)
            .name(COLUMN_CONCEPT_FAIR_DATA_NAME)
            .description(COLUMN_CONCEPT_FAIR_DATA_DESCRIPTION)
            .created(COLUMN_CONCEPT_FAIR_DATA_CREATED)
            .build();

    public final static String COLUMN_UNIT_DEGREES_CELSIUS_NAME = "Degrees Celsius";
    public final static String COLUMN_UNIT_DEGREES_CELSIUS_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/degreeCelsius";
    public final static String COLUMN_UNIT_DEGREES_CELSIUS_DESCRIPTION = "The degree Celsius is a unit of temperature defined as 1 kelvin.";
    public final static Instant COLUMN_UNIT_DEGREES_CELSIUS_CREATED = Instant.now();

    public final static UnitSaveDto COLUMN_UNIT_DEGREES_CELSIUS_SAVE_DTO = UnitSaveDto.builder()
            .uri(COLUMN_UNIT_DEGREES_CELSIUS_URI)
            .name(COLUMN_UNIT_DEGREES_CELSIUS_NAME)
            .description(COLUMN_UNIT_DEGREES_CELSIUS_DESCRIPTION)
            .build();

    public final static UnitDto COLUMN_UNIT_DEGREES_CELSIUS_DTO = UnitDto.builder()
            .uri(COLUMN_UNIT_DEGREES_CELSIUS_URI)
            .name(COLUMN_UNIT_DEGREES_CELSIUS_NAME)
            .description(COLUMN_UNIT_DEGREES_CELSIUS_DESCRIPTION)
            .build();

    public final static TableColumnUnit COLUMN_UNIT_DEGREES_CELSIUS = TableColumnUnit.builder()
            .uri(COLUMN_UNIT_DEGREES_CELSIUS_URI)
            .name(COLUMN_UNIT_DEGREES_CELSIUS_NAME)
            .description(COLUMN_UNIT_DEGREES_CELSIUS_DESCRIPTION)
            .created(COLUMN_CONCEPT_TEMPERATURE_CREATED)
            .build();

    public final static String COLUMN_UNIT_TON_NAME = "tonne";
    public final static String COLUMN_UNIT_TON_URI = "http://www.ontology-of-units-of-measure.org/resource/om-2/tonne";
    public final static String COLUMN_UNIT_TON_DESCRIPTION = "The tonne is a unit of mass defined as 1000 kilogram.";
    public final static Instant COLUMN_UNIT_TON_CREATED = Instant.now();

    public final static UnitSaveDto COLUMN_UNIT_TON_SAVE_DTO = UnitSaveDto.builder()
            .uri(COLUMN_UNIT_TON_URI)
            .name(COLUMN_UNIT_TON_NAME)
            .description(COLUMN_UNIT_TON_DESCRIPTION)
            .build();

    public final static UnitDto COLUMN_UNIT_TON_DTO = UnitDto.builder()
            .uri(COLUMN_UNIT_TON_URI)
            .name(COLUMN_UNIT_TON_NAME)
            .description(COLUMN_UNIT_TON_DESCRIPTION)
            .build();

    public final static TableColumnUnit COLUMN_UNIT_TON = TableColumnUnit.builder()
            .uri(COLUMN_UNIT_TON_URI)
            .name(COLUMN_UNIT_TON_NAME)
            .description(COLUMN_UNIT_TON_DESCRIPTION)
            .created(COLUMN_UNIT_TON_CREATED)
            .build();

    public final static Long COLUMN_1_1_ID = 1L;
    public final static Integer COLUMN_1_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_1_1_PRIMARY = true;
    public final static String COLUMN_1_1_NAME = "id";
    public final static String COLUMN_1_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_1_1_TYPE = TableColumnType.NUMBER;
    public final static Long COLUMN_1_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_1_1_NULL = false;
    public final static Boolean COLUMN_1_1_UNIQUE = true;
    public final static Boolean COLUMN_1_1_AUTO_GENERATED = false;
    public final static String COLUMN_1_1_FOREIGN_KEY = null;
    public final static String COLUMN_1_1_CHECK = null;
    public final static List<String> COLUMN_1_1_ENUM_VALUES = null;

    public final static Long COLUMN_1_2_ID = 2L;
    public final static Integer COLUMN_1_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_1_2_PRIMARY = false;
    public final static String COLUMN_1_2_NAME = "Date";
    public final static String COLUMN_1_2_INTERNAL_NAME = "date";
    public final static TableColumnType COLUMN_1_2_TYPE = TableColumnType.DATE;
    public final static Long COLUMN_1_2_DATE_FORMAT = IMAGE_DATE_1_ID;
    public final static Boolean COLUMN_1_2_NULL = true;
    public final static Boolean COLUMN_1_2_UNIQUE = false;
    public final static Boolean COLUMN_1_2_AUTO_GENERATED = false;
    public final static String COLUMN_1_2_FOREIGN_KEY = null;
    public final static String COLUMN_1_2_CHECK = null;
    public final static List<String> COLUMN_1_2_ENUM_VALUES = null;

    public final static Long COLUMN_1_3_ID = 3L;
    public final static Integer COLUMN_1_3_ORDINALPOS = 2;
    public final static Boolean COLUMN_1_3_PRIMARY = false;
    public final static String COLUMN_1_3_NAME = "Location";
    public final static String COLUMN_1_3_INTERNAL_NAME = "location";
    public final static TableColumnType COLUMN_1_3_TYPE = TableColumnType.STRING;
    public final static Long COLUMN_1_3_DATE_FORMAT = null;
    public final static Boolean COLUMN_1_3_NULL = true;
    public final static Boolean COLUMN_1_3_UNIQUE = false;
    public final static Boolean COLUMN_1_3_AUTO_GENERATED = false;
    public final static String COLUMN_1_3_FOREIGN_KEY = null;
    public final static String COLUMN_1_3_CHECK = null;
    public final static List<String> COLUMN_1_3_ENUM_VALUES = null;

    public final static Long COLUMN_1_4_ID = 4L;
    public final static Integer COLUMN_1_4_ORDINALPOS = 3;
    public final static Boolean COLUMN_1_4_PRIMARY = false;
    public final static String COLUMN_1_4_NAME = "MinTemp";
    public final static String COLUMN_1_4_INTERNAL_NAME = "mintemp";
    public final static TableColumnType COLUMN_1_4_TYPE = TableColumnType.DECIMAL;
    public final static Long COLUMN_1_4_DATE_FORMAT = null;
    public final static Boolean COLUMN_1_4_NULL = true;
    public final static Boolean COLUMN_1_4_UNIQUE = false;
    public final static Boolean COLUMN_1_4_AUTO_GENERATED = false;
    public final static String COLUMN_1_4_FOREIGN_KEY = null;
    public final static String COLUMN_1_4_CHECK = null;
    public final static List<String> COLUMN_1_4_ENUM_VALUES = null;

    public final static ColumnSemanticsUpdateDto COLUMN_1_4_SEMANTICS_UPDATE_DTO = ColumnSemanticsUpdateDto.builder()
            .conceptUri(COLUMN_CONCEPT_TEMPERATURE_URI)
            .unitUri(COLUMN_UNIT_DEGREES_CELSIUS_URI)
            .build();

    public final static TableColumn COLUMN_1_4_WITH_SEMANTICS = TableColumn.builder()
            .id(COLUMN_1_4_ID)
            .ordinalPosition(COLUMN_1_4_ORDINALPOS)
            .cdbid(DATABASE_1_ID)
            .tid(TABLE_1_ID)
            .name(COLUMN_1_4_NAME)
            .internalName(COLUMN_1_4_INTERNAL_NAME)
            .columnType(COLUMN_1_4_TYPE)
            .dfid(COLUMN_1_4_DATE_FORMAT)
            .isNullAllowed(COLUMN_1_4_NULL)
            .autoGenerated(COLUMN_1_4_AUTO_GENERATED)
            .isPrimaryKey(COLUMN_1_4_PRIMARY)
            .enumValues(COLUMN_1_4_ENUM_VALUES)
            .concept(COLUMN_CONCEPT_TEMPERATURE)
            .unit(COLUMN_UNIT_DEGREES_CELSIUS)
            .build();

    public final static Long COLUMN_1_5_ID = 5L;
    public final static Integer COLUMN_1_5_ORDINALPOS = 4;
    public final static Boolean COLUMN_1_5_PRIMARY = false;
    public final static String COLUMN_1_5_NAME = "Rainfall";
    public final static String COLUMN_1_5_INTERNAL_NAME = "rainfall";
    public final static TableColumnType COLUMN_1_5_TYPE = TableColumnType.DECIMAL;
    public final static Long COLUMN_1_5_DATE_FORMAT = null;
    public final static Boolean COLUMN_1_5_NULL = true;
    public final static Boolean COLUMN_1_5_UNIQUE = false;
    public final static Boolean COLUMN_1_5_AUTO_GENERATED = false;
    public final static String COLUMN_1_5_FOREIGN_KEY = null;
    public final static String COLUMN_1_5_CHECK = null;
    public final static List<String> COLUMN_1_5_ENUM_VALUES = null;

    public final static Long COLUMN_2_1_ID = 6L;
    public final static Integer COLUMN_2_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_2_1_PRIMARY = true;
    public final static String COLUMN_2_1_NAME = "location";
    public final static String COLUMN_2_1_INTERNAL_NAME = "location";
    public final static TableColumnType COLUMN_2_1_TYPE = TableColumnType.STRING;
    public final static Long COLUMN_2_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_2_1_NULL = false;
    public final static Boolean COLUMN_2_1_UNIQUE = true;
    public final static Boolean COLUMN_2_1_AUTO_GENERATED = false;
    public final static String COLUMN_2_1_FOREIGN_KEY = null;
    public final static String COLUMN_2_1_CHECK = null;
    public final static List<String> COLUMN_2_1_ENUM_VALUES = null;

    public final static Long COLUMN_2_2_ID = 7L;
    public final static Integer COLUMN_2_2_ORDINALPOS = 0;
    public final static Boolean COLUMN_2_2_PRIMARY = false;
    public final static String COLUMN_2_2_NAME = "lat";
    public final static String COLUMN_2_2_INTERNAL_NAME = "lat";
    public final static TableColumnType COLUMN_2_2_TYPE = TableColumnType.DECIMAL;
    public final static Long COLUMN_2_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_2_2_NULL = true;
    public final static Boolean COLUMN_2_2_UNIQUE = false;
    public final static Boolean COLUMN_2_2_AUTO_GENERATED = false;
    public final static String COLUMN_2_2_FOREIGN_KEY = null;
    public final static String COLUMN_2_2_CHECK = null;
    public final static List<String> COLUMN_2_2_ENUM_VALUES = null;

    public final static Long COLUMN_2_3_ID = 8L;
    public final static Integer COLUMN_2_3_ORDINALPOS = 0;
    public final static Boolean COLUMN_2_3_PRIMARY = false;
    public final static String COLUMN_2_3_NAME = "lng";
    public final static String COLUMN_2_3_INTERNAL_NAME = "lng";
    public final static TableColumnType COLUMN_2_3_TYPE = TableColumnType.DECIMAL;
    public final static Long COLUMN_2_3_DATE_FORMAT = null;
    public final static Boolean COLUMN_2_3_NULL = true;
    public final static Boolean COLUMN_2_3_UNIQUE = false;
    public final static Boolean COLUMN_2_3_AUTO_GENERATED = false;
    public final static String COLUMN_2_3_FOREIGN_KEY = null;
    public final static String COLUMN_2_3_CHECK = null;
    public final static List<String> COLUMN_2_3_ENUM_VALUES = null;

    public final static Long COLUMN_4_1_ID = 44L;
    public final static Integer COLUMN_4_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_4_1_PRIMARY = true;
    public final static String COLUMN_4_1_NAME = "id";
    public final static String COLUMN_4_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_4_1_TYPE = TableColumnType.NUMBER;
    public final static Long COLUMN_4_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_1_NULL = false;
    public final static Boolean COLUMN_4_1_UNIQUE = true;
    public final static Boolean COLUMN_4_1_AUTO_GENERATED = true;
    public final static String COLUMN_4_1_FOREIGN_KEY = null;
    public final static String COLUMN_4_1_CHECK = null;
    public final static List<String> COLUMN_4_1_ENUM_VALUES = null;
    public final static ColumnTypeDto COLUMN_4_1_TYPE_DTO = ColumnTypeDto.NUMBER;
    public final static String[] COLUMN_4_1_ENUM_VALUES_ARRAY = null;

    public final static Long COLUMN_4_2_ID = 45L;
    public final static Integer COLUMN_4_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_4_2_PRIMARY = false;
    public final static String COLUMN_4_2_NAME = "Animal Name";
    public final static String COLUMN_4_2_INTERNAL_NAME = "animal_name";
    public final static TableColumnType COLUMN_4_2_TYPE = TableColumnType.STRING;
    public final static Long COLUMN_4_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_2_NULL = true;
    public final static Boolean COLUMN_4_2_UNIQUE = false;
    public final static Boolean COLUMN_4_2_AUTO_GENERATED = false;
    public final static String COLUMN_4_2_FOREIGN_KEY = null;
    public final static String COLUMN_4_2_CHECK = null;
    public final static List<String> COLUMN_4_2_ENUM_VALUES = null;
    public final static ColumnTypeDto COLUMN_4_2_TYPE_DTO = ColumnTypeDto.STRING;
    public final static String[] COLUMN_4_2_ENUM_VALUES_ARRAY = null;

    public final static Long COLUMN_4_3_ID = 46L;
    public final static Integer COLUMN_4_3_ORDINALPOS = 2;
    public final static Boolean COLUMN_4_3_PRIMARY = false;
    public final static String COLUMN_4_3_NAME = "Hair";
    public final static String COLUMN_4_3_INTERNAL_NAME = "hair";
    public final static TableColumnType COLUMN_4_3_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_3_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_3_NULL = true;
    public final static Boolean COLUMN_4_3_UNIQUE = false;
    public final static Boolean COLUMN_4_3_AUTO_GENERATED = false;
    public final static String COLUMN_4_3_FOREIGN_KEY = null;
    public final static String COLUMN_4_3_CHECK = null;
    public final static List<String> COLUMN_4_3_ENUM_VALUES = null;

    public final static Long COLUMN_4_4_ID = 47L;
    public final static Integer COLUMN_4_4_ORDINALPOS = 3;
    public final static Boolean COLUMN_4_4_PRIMARY = false;
    public final static String COLUMN_4_4_NAME = "Feathers";
    public final static String COLUMN_4_4_INTERNAL_NAME = "feathers";
    public final static TableColumnType COLUMN_4_4_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_4_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_4_NULL = true;
    public final static Boolean COLUMN_4_4_UNIQUE = false;
    public final static Boolean COLUMN_4_4_AUTO_GENERATED = false;
    public final static String COLUMN_4_4_FOREIGN_KEY = null;
    public final static String COLUMN_4_4_CHECK = null;
    public final static List<String> COLUMN_4_4_ENUM_VALUES = null;

    public final static Long COLUMN_4_5_ID = 48L;
    public final static Integer COLUMN_4_5_ORDINALPOS = 4;
    public final static Boolean COLUMN_4_5_PRIMARY = false;
    public final static String COLUMN_4_5_NAME = "Bread";
    public final static String COLUMN_4_5_INTERNAL_NAME = "bread";
    public final static TableColumnType COLUMN_4_5_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_5_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_5_NULL = true;
    public final static Boolean COLUMN_4_5_UNIQUE = false;
    public final static Boolean COLUMN_4_5_AUTO_GENERATED = false;
    public final static String COLUMN_4_5_FOREIGN_KEY = null;
    public final static String COLUMN_4_5_CHECK = null;
    public final static List<String> COLUMN_4_5_ENUM_VALUES = null;

    public final static Long COLUMN_4_6_ID = 49L;
    public final static Integer COLUMN_4_6_ORDINALPOS = 5;
    public final static Boolean COLUMN_4_6_PRIMARY = false;
    public final static String COLUMN_4_6_NAME = "Eggs";
    public final static String COLUMN_4_6_INTERNAL_NAME = "eggs";
    public final static TableColumnType COLUMN_4_6_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_6_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_6_NULL = true;
    public final static Boolean COLUMN_4_6_UNIQUE = false;
    public final static Boolean COLUMN_4_6_AUTO_GENERATED = false;
    public final static String COLUMN_4_6_FOREIGN_KEY = null;
    public final static String COLUMN_4_6_CHECK = null;
    public final static List<String> COLUMN_4_6_ENUM_VALUES = null;

    public final static Long COLUMN_4_7_ID = 50L;
    public final static Integer COLUMN_4_7_ORDINALPOS = 6;
    public final static Boolean COLUMN_4_7_PRIMARY = false;
    public final static String COLUMN_4_7_NAME = "Milk";
    public final static String COLUMN_4_7_INTERNAL_NAME = "milk";
    public final static TableColumnType COLUMN_4_7_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_7_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_7_NULL = true;
    public final static Boolean COLUMN_4_7_UNIQUE = false;
    public final static Boolean COLUMN_4_7_AUTO_GENERATED = false;
    public final static String COLUMN_4_7_FOREIGN_KEY = null;
    public final static String COLUMN_4_7_CHECK = null;
    public final static List<String> COLUMN_4_7_ENUM_VALUES = null;

    public final static Long COLUMN_4_8_ID = 51L;
    public final static Integer COLUMN_4_8_ORDINALPOS = 7;
    public final static Boolean COLUMN_4_8_PRIMARY = false;
    public final static String COLUMN_4_8_NAME = "Water";
    public final static String COLUMN_4_8_INTERNAL_NAME = "water";
    public final static TableColumnType COLUMN_4_8_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_8_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_8_NULL = true;
    public final static Boolean COLUMN_4_8_UNIQUE = false;
    public final static Boolean COLUMN_4_8_AUTO_GENERATED = false;
    public final static String COLUMN_4_8_FOREIGN_KEY = null;
    public final static String COLUMN_4_8_CHECK = null;
    public final static List<String> COLUMN_4_8_ENUM_VALUES = null;

    public final static Long COLUMN_4_9_ID = 52L;
    public final static Integer COLUMN_4_9_ORDINALPOS = 8;
    public final static Boolean COLUMN_4_9_PRIMARY = false;
    public final static String COLUMN_4_9_NAME = "Airborne";
    public final static String COLUMN_4_9_INTERNAL_NAME = "airborne";
    public final static TableColumnType COLUMN_4_9_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_9_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_9_NULL = true;
    public final static Boolean COLUMN_4_9_UNIQUE = false;
    public final static Boolean COLUMN_4_9_AUTO_GENERATED = false;
    public final static String COLUMN_4_9_FOREIGN_KEY = null;
    public final static String COLUMN_4_9_CHECK = null;
    public final static List<String> COLUMN_4_9_ENUM_VALUES = null;

    public final static Long COLUMN_4_10_ID = 53L;
    public final static Integer COLUMN_4_10_ORDINALPOS = 9;
    public final static Boolean COLUMN_4_10_PRIMARY = false;
    public final static String COLUMN_4_10_NAME = "Waterborne";
    public final static String COLUMN_4_10_INTERNAL_NAME = "waterborne";
    public final static TableColumnType COLUMN_4_10_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_10_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_10_NULL = true;
    public final static Boolean COLUMN_4_10_UNIQUE = false;
    public final static Boolean COLUMN_4_10_AUTO_GENERATED = false;
    public final static String COLUMN_4_10_FOREIGN_KEY = null;
    public final static String COLUMN_4_10_CHECK = null;
    public final static List<String> COLUMN_4_10_ENUM_VALUES = null;

    public final static Long COLUMN_4_11_ID = 54L;
    public final static Integer COLUMN_4_11_ORDINALPOS = 10;
    public final static Boolean COLUMN_4_11_PRIMARY = false;
    public final static String COLUMN_4_11_NAME = "Aquantic";
    public final static String COLUMN_4_11_INTERNAL_NAME = "aquatic";
    public final static TableColumnType COLUMN_4_11_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_11_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_11_NULL = true;
    public final static Boolean COLUMN_4_11_UNIQUE = false;
    public final static Boolean COLUMN_4_11_AUTO_GENERATED = false;
    public final static String COLUMN_4_11_FOREIGN_KEY = null;
    public final static String COLUMN_4_11_CHECK = null;
    public final static List<String> COLUMN_4_11_ENUM_VALUES = null;

    public final static Long COLUMN_4_12_ID = 55L;
    public final static Integer COLUMN_4_12_ORDINALPOS = 11;
    public final static Boolean COLUMN_4_12_PRIMARY = false;
    public final static String COLUMN_4_12_NAME = "Predator";
    public final static String COLUMN_4_12_INTERNAL_NAME = "predator";
    public final static TableColumnType COLUMN_4_12_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_12_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_12_NULL = true;
    public final static Boolean COLUMN_4_12_UNIQUE = false;
    public final static Boolean COLUMN_4_12_AUTO_GENERATED = false;
    public final static String COLUMN_4_12_FOREIGN_KEY = null;
    public final static String COLUMN_4_12_CHECK = null;
    public final static List<String> COLUMN_4_12_ENUM_VALUES = null;

    public final static Long COLUMN_4_13_ID = 56L;
    public final static Integer COLUMN_4_13_ORDINALPOS = 12;
    public final static Boolean COLUMN_4_13_PRIMARY = false;
    public final static String COLUMN_4_13_NAME = "Backbone";
    public final static String COLUMN_4_13_INTERNAL_NAME = "backbone";
    public final static TableColumnType COLUMN_4_13_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_13_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_13_NULL = true;
    public final static Boolean COLUMN_4_13_UNIQUE = false;
    public final static Boolean COLUMN_4_13_AUTO_GENERATED = false;
    public final static String COLUMN_4_13_FOREIGN_KEY = null;
    public final static String COLUMN_4_13_CHECK = null;
    public final static List<String> COLUMN_4_13_ENUM_VALUES = null;

    public final static Long COLUMN_4_14_ID = 57L;
    public final static Integer COLUMN_4_14_ORDINALPOS = 13;
    public final static Boolean COLUMN_4_14_PRIMARY = false;
    public final static String COLUMN_4_14_NAME = "Breathes";
    public final static String COLUMN_4_14_INTERNAL_NAME = "breathes";
    public final static TableColumnType COLUMN_4_14_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_14_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_14_NULL = true;
    public final static Boolean COLUMN_4_14_UNIQUE = false;
    public final static Boolean COLUMN_4_14_AUTO_GENERATED = false;
    public final static String COLUMN_4_14_FOREIGN_KEY = null;
    public final static String COLUMN_4_14_CHECK = null;
    public final static List<String> COLUMN_4_14_ENUM_VALUES = null;

    public final static Long COLUMN_4_15_ID = 58L;
    public final static Integer COLUMN_4_15_ORDINALPOS = 14;
    public final static Boolean COLUMN_4_15_PRIMARY = false;
    public final static String COLUMN_4_15_NAME = "Venomous";
    public final static String COLUMN_4_15_INTERNAL_NAME = "venomous";
    public final static TableColumnType COLUMN_4_15_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_15_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_15_NULL = true;
    public final static Boolean COLUMN_4_15_UNIQUE = false;
    public final static Boolean COLUMN_4_15_AUTO_GENERATED = false;
    public final static String COLUMN_4_15_FOREIGN_KEY = null;
    public final static String COLUMN_4_15_CHECK = null;
    public final static List<String> COLUMN_4_15_ENUM_VALUES = null;

    public final static Long COLUMN_4_16_ID = 59L;
    public final static Integer COLUMN_4_16_ORDINALPOS = 15;
    public final static Boolean COLUMN_4_16_PRIMARY = false;
    public final static String COLUMN_4_16_NAME = "Fin";
    public final static String COLUMN_4_16_INTERNAL_NAME = "fins";
    public final static TableColumnType COLUMN_4_16_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_16_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_16_NULL = true;
    public final static Boolean COLUMN_4_16_UNIQUE = false;
    public final static Boolean COLUMN_4_16_AUTO_GENERATED = false;
    public final static String COLUMN_4_16_FOREIGN_KEY = null;
    public final static String COLUMN_4_16_CHECK = null;
    public final static List<String> COLUMN_4_16_ENUM_VALUES = null;

    public final static Long COLUMN_4_17_ID = 60L;
    public final static Integer COLUMN_4_17_ORDINALPOS = 16;
    public final static Boolean COLUMN_4_17_PRIMARY = false;
    public final static String COLUMN_4_17_NAME = "Legs";
    public final static String COLUMN_4_17_INTERNAL_NAME = "legs";
    public final static TableColumnType COLUMN_4_17_TYPE = TableColumnType.NUMBER;
    public final static Long COLUMN_4_17_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_17_NULL = true;
    public final static Boolean COLUMN_4_17_UNIQUE = false;
    public final static Boolean COLUMN_4_17_AUTO_GENERATED = false;
    public final static String COLUMN_4_17_FOREIGN_KEY = null;
    public final static String COLUMN_4_17_CHECK = null;
    public final static List<String> COLUMN_4_17_ENUM_VALUES = null;

    public final static Long COLUMN_4_18_ID = 61L;
    public final static Integer COLUMN_4_18_ORDINALPOS = 17;
    public final static Boolean COLUMN_4_18_PRIMARY = false;
    public final static String COLUMN_4_18_NAME = "Tail";
    public final static String COLUMN_4_18_INTERNAL_NAME = "tail";
    public final static TableColumnType COLUMN_4_18_TYPE = TableColumnType.DECIMAL;
    public final static Long COLUMN_4_18_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_18_NULL = true;
    public final static Boolean COLUMN_4_18_UNIQUE = false;
    public final static Boolean COLUMN_4_18_AUTO_GENERATED = false;
    public final static String COLUMN_4_18_FOREIGN_KEY = null;
    public final static String COLUMN_4_18_CHECK = null;
    public final static List<String> COLUMN_4_18_ENUM_VALUES = null;

    public final static Long COLUMN_4_19_ID = 62L;
    public final static Integer COLUMN_4_19_ORDINALPOS = 18;
    public final static Boolean COLUMN_4_19_PRIMARY = false;
    public final static String COLUMN_4_19_NAME = "Domestic";
    public final static String COLUMN_4_19_INTERNAL_NAME = "domestic";
    public final static TableColumnType COLUMN_4_19_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_19_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_19_NULL = true;
    public final static Boolean COLUMN_4_19_UNIQUE = false;
    public final static Boolean COLUMN_4_19_AUTO_GENERATED = false;
    public final static String COLUMN_4_19_FOREIGN_KEY = null;
    public final static String COLUMN_4_19_CHECK = null;
    public final static List<String> COLUMN_4_19_ENUM_VALUES = null;

    public final static Long COLUMN_4_20_ID = 63L;
    public final static Integer COLUMN_4_20_ORDINALPOS = 19;
    public final static Boolean COLUMN_4_20_PRIMARY = false;
    public final static String COLUMN_4_20_NAME = "Cat Size";
    public final static String COLUMN_4_20_INTERNAL_NAME = "catsize";
    public final static TableColumnType COLUMN_4_20_TYPE = TableColumnType.BOOLEAN;
    public final static Long COLUMN_4_20_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_20_NULL = true;
    public final static Boolean COLUMN_4_20_UNIQUE = false;
    public final static Boolean COLUMN_4_20_AUTO_GENERATED = false;
    public final static String COLUMN_4_20_FOREIGN_KEY = null;
    public final static String COLUMN_4_20_CHECK = null;
    public final static List<String> COLUMN_4_20_ENUM_VALUES = null;

    public final static Long COLUMN_4_21_ID = 64L;
    public final static Integer COLUMN_4_21_ORDINALPOS = 20;
    public final static Boolean COLUMN_4_21_PRIMARY = false;
    public final static String COLUMN_4_21_NAME = "Class Type";
    public final static String COLUMN_4_21_INTERNAL_NAME = "class_type";
    public final static TableColumnType COLUMN_4_21_TYPE = TableColumnType.DECIMAL;
    public final static Long COLUMN_4_21_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_21_NULL = true;
    public final static Boolean COLUMN_4_21_UNIQUE = false;
    public final static Boolean COLUMN_4_21_AUTO_GENERATED = false;
    public final static String COLUMN_4_21_FOREIGN_KEY = null;
    public final static String COLUMN_4_21_CHECK = null;
    public final static List<String> COLUMN_4_21_ENUM_VALUES = null;

    public final static Long COLUMN_5_1_ID = 65L;
    public final static Integer COLUMN_5_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_5_1_PRIMARY = true;
    public final static String COLUMN_5_1_NAME = "id";
    public final static String COLUMN_5_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_5_1_TYPE = TableColumnType.NUMBER;
    public final static ColumnTypeDto COLUMN_5_1_TYPE_DTO = ColumnTypeDto.NUMBER;
    public final static Long COLUMN_5_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_1_NULL = false;
    public final static Boolean COLUMN_5_1_UNIQUE = true;
    public final static Boolean COLUMN_5_1_AUTO_GENERATED = true;
    public final static String COLUMN_5_1_FOREIGN_KEY = null;
    public final static String COLUMN_5_1_CHECK = null;
    public final static List<String> COLUMN_5_1_ENUM_VALUES = null;

    public final static Long COLUMN_5_2_ID = 66L;
    public final static Integer COLUMN_5_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_5_2_PRIMARY = false;
    public final static String COLUMN_5_2_NAME = "firstname";
    public final static String COLUMN_5_2_INTERNAL_NAME = "firstname";
    public final static TableColumnType COLUMN_5_2_TYPE = TableColumnType.STRING;
    public final static ColumnTypeDto COLUMN_5_2_TYPE_DTO = ColumnTypeDto.STRING;
    public final static Integer COLUMN_5_2_LENGTH = 20;
    public final static Long COLUMN_5_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_2_NULL = false;
    public final static Boolean COLUMN_5_2_UNIQUE = false;
    public final static Boolean COLUMN_5_2_AUTO_GENERATED = false;
    public final static String COLUMN_5_2_FOREIGN_KEY = null;
    public final static String COLUMN_5_2_CHECK = null;
    public final static List<String> COLUMN_5_2_ENUM_VALUES = null;

    public final static Long COLUMN_5_3_ID = 67L;
    public final static Integer COLUMN_5_3_ORDINALPOS = 2;
    public final static Boolean COLUMN_5_3_PRIMARY = false;
    public final static String COLUMN_5_3_NAME = "lastname";
    public final static String COLUMN_5_3_INTERNAL_NAME = "lastname";
    public final static TableColumnType COLUMN_5_3_TYPE = TableColumnType.STRING;
    public final static ColumnTypeDto COLUMN_5_3_TYPE_DTO = ColumnTypeDto.STRING;
    public final static Integer COLUMN_5_3_LENGTH = 40;
    public final static Long COLUMN_5_3_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_3_NULL = false;
    public final static Boolean COLUMN_5_3_UNIQUE = false;
    public final static Boolean COLUMN_5_3_AUTO_GENERATED = false;
    public final static String COLUMN_5_3_FOREIGN_KEY = null;
    public final static String COLUMN_5_3_CHECK = null;
    public final static List<String> COLUMN_5_3_ENUM_VALUES = null;

    public final static Long COLUMN_5_4_ID = 68L;
    public final static Integer COLUMN_5_4_ORDINALPOS = 2;
    public final static Boolean COLUMN_5_4_PRIMARY = false;
    public final static String COLUMN_5_4_NAME = "ref_id";
    public final static String COLUMN_5_4_INTERNAL_NAME = "ref_id";
    public final static TableColumnType COLUMN_5_4_TYPE = TableColumnType.NUMBER;
    public final static ColumnTypeDto COLUMN_5_4_TYPE_DTO = ColumnTypeDto.NUMBER;
    public final static Long COLUMN_5_4_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_4_NULL = false;
    public final static Boolean COLUMN_5_4_UNIQUE = false;
    public final static Boolean COLUMN_5_4_AUTO_GENERATED = false;
    public final static String COLUMN_5_4_FOREIGN_KEY = null;
    public final static String COLUMN_5_4_CHECK = null;
    public final static List<String> COLUMN_5_4_ENUM_VALUES = null;

    public final static Long COLUMN_8_1_ID = 69L;
    public final static Integer COLUMN_8_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_8_1_PRIMARY = true;
    public final static String COLUMN_8_1_NAME = "ID";
    public final static String COLUMN_8_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_8_1_TYPE = TableColumnType.NUMBER;
    public final static ColumnTypeDto COLUMN_8_1_TYPE_DTO = ColumnTypeDto.NUMBER;
    public final static Long COLUMN_8_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_8_1_NULL = false;
    public final static Boolean COLUMN_8_1_UNIQUE = true;
    public final static Boolean COLUMN_8_1_AUTO_GENERATED = true;
    public final static String COLUMN_8_1_FOREIGN_KEY = null;
    public final static String COLUMN_8_1_CHECK = null;
    public final static List<String> COLUMN_8_1_ENUM_VALUES = null;

    public final static Long COLUMN_8_2_ID = 70L;
    public final static Integer COLUMN_8_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_8_2_PRIMARY = true;
    public final static String COLUMN_8_2_NAME = "Value";
    public final static String COLUMN_8_2_INTERNAL_NAME = "value";
    public final static TableColumnType COLUMN_8_2_TYPE = TableColumnType.NUMBER;
    public final static ColumnTypeDto COLUMN_8_2_TYPE_DTO = ColumnTypeDto.NUMBER;
    public final static Long COLUMN_8_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_8_2_NULL = true;
    public final static Boolean COLUMN_8_2_UNIQUE = false;
    public final static Boolean COLUMN_8_2_AUTO_GENERATED = false;
    public final static String COLUMN_8_2_FOREIGN_KEY = null;
    public final static String COLUMN_8_2_CHECK = null;
    public final static List<String> COLUMN_8_2_ENUM_VALUES = null;

    public final static ColumnSemanticsUpdateDto COLUMN_8_2_SEMANTICS_UPDATE_DTO = ColumnSemanticsUpdateDto.builder()
            .conceptUri(COLUMN_CONCEPT_TEMPERATURE_URI)
            .unitUri(COLUMN_UNIT_DEGREES_CELSIUS_URI)
            .build();

    public final static TableColumn COLUMN_8_2_WITH_SEMANTICS = TableColumn.builder()
            .id(COLUMN_8_2_ID)
            .ordinalPosition(COLUMN_8_2_ORDINALPOS)
            .cdbid(DATABASE_3_ID)
            .tid(TABLE_8_ID)
            .name(COLUMN_8_2_NAME)
            .internalName(COLUMN_8_2_INTERNAL_NAME)
            .columnType(COLUMN_8_2_TYPE)
            .dfid(COLUMN_8_2_DATE_FORMAT)
            .isNullAllowed(COLUMN_8_2_NULL)
            .autoGenerated(COLUMN_8_2_AUTO_GENERATED)
            .isPrimaryKey(COLUMN_8_2_PRIMARY)
            .unit(COLUMN_UNIT_DEGREES_CELSIUS)
            .concept(COLUMN_CONCEPT_TEMPERATURE)
            .build();

    public final static List<TableColumn> TABLE_8_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_8_1_ID)
                    .ordinalPosition(COLUMN_8_1_ORDINALPOS)
                    .cdbid(DATABASE_3_ID)
                    .tid(TABLE_8_ID)
                    .name(COLUMN_8_1_NAME)
                    .internalName(COLUMN_8_1_INTERNAL_NAME)
                    .columnType(COLUMN_8_1_TYPE)
                    .dfid(COLUMN_8_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_8_1_NULL)
                    .autoGenerated(COLUMN_8_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_8_1_PRIMARY)
                    .creator(USER_3)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_8_2_ID)
                    .ordinalPosition(COLUMN_8_2_ORDINALPOS)
                    .cdbid(DATABASE_3_ID)
                    .tid(TABLE_8_ID)
                    .name(COLUMN_8_2_NAME)
                    .internalName(COLUMN_8_2_INTERNAL_NAME)
                    .columnType(COLUMN_8_2_TYPE)
                    .dfid(COLUMN_8_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_8_2_NULL)
                    .autoGenerated(COLUMN_8_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_8_2_PRIMARY)
                    .creator(USER_3)
                    .build());

    public final static Table TABLE_8 = Table.builder()
            .id(TABLE_8_ID)
            .created(Instant.now())
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .name(TABLE_8_NAME)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_8_QUEUE_NAME)
            .routingKey(TABLE_8_ROUTING_KEY)
            .columns(TABLE_8_COLUMNS)
            .creator(USER_1)
            .created(TABLE_8_CREATED)
            .lastModified(TABLE_8_LAST_MODIFIED)
            .build();

    public final static List<String> CONSTRAINTS_1_UNIQUE_1 = List.of(COLUMN_1_1_NAME);
    public final static List<String> CONSTRAINTS_2_UNIQUE_1 = List.of(COLUMN_2_1_NAME);
    public final static List<String> CONSTRAINTS_3_UNIQUE_1 = List.of("id");
    public final static List<String> CONSTRAINTS_4_UNIQUE_1 = List.of(COLUMN_4_1_NAME);
    public final static List<String> CONSTRAINTS_5_UNIQUE_1 = List.of(COLUMN_5_1_NAME);

    public final static Long CONCEPT_1_ID = 1L;
    public final static String CONCEPT_1_NAME = "Temperature";
    public final static Instant CONCEPT_1_CREATED = Instant.now().minus(1, HOURS);

    public final static TableColumnConcept CONCEPT_1 = TableColumnConcept.builder()
            .name(CONCEPT_1_NAME)
            .created(CONCEPT_1_CREATED)
            .uri("http://www.ontology-of-units-of-measure.org/resource/om-2/")
            .build();

    public final static Long QUERY_1_ID = 1L;
    public final static String QUERY_1_STATEMENT = "SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM " +
            "`weather_aus`";
    public final static String QUERY_1_DOI = null;
    public final static Long QUERY_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long QUERY_1_DATABASE_ID = DATABASE_1_ID;
    public final static Long QUERY_1_RESULT_NUMBER = 2L;
    public final static String QUERY_1_QUERY_HASH = "a3b8ac39e38167d14cf3a9c20a69e4b6954d049525390b973a2c23064953a992";
    public final static String QUERY_1_RESULT_HASH = "8358c8ade4849d2094ab5bb29127afdae57e6bb5acb1db7af603813d406c467a";
    public final static Instant QUERY_1_CREATED = Instant.ofEpochSecond(1677648377);
    public final static Instant QUERY_1_EXECUTION = Instant.now();
    public final static Boolean QUERY_1_PERSISTED = false;

    public final static Query QUERY_1 = Query.builder()
            .id(QUERY_1_ID)
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .resultNumber(QUERY_1_RESULT_NUMBER)
            .created(QUERY_1_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_1_PERSISTED)
            .executed(QUERY_1_EXECUTION)
            .build();

    public final static QueryDto QUERY_1_DTO = QueryDto.builder()
            .id(QUERY_1_ID)
            .cid(QUERY_1_CONTAINER_ID)
            .dbid(QUERY_1_DATABASE_ID)
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
            .cid(QUERY_1_CONTAINER_ID)
            .dbid(QUERY_1_DATABASE_ID)
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
    public final static Instant QUERY_2_LAST_MODIFIED = Instant.ofEpochSecond(1541588352);
    public final static Boolean QUERY_2_PERSISTED = false;

    public final static Query QUERY_2 = Query.builder()
            .id(QUERY_2_ID)
            .query(QUERY_2_STATEMENT)
            .queryHash(QUERY_2_QUERY_HASH)
            .resultHash(QUERY_2_RESULT_HASH)
            .resultNumber(QUERY_2_RESULT_NUMBER)
            .created(QUERY_2_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_2_PERSISTED)
            .created(QUERY_2_CREATED)
            .executed(QUERY_2_EXECUTION)
            .build();

    public final static QueryDto QUERY_2_DTO = QueryDto.builder()
            .id(QUERY_2_ID)
            .cid(QUERY_2_CONTAINER_ID)
            .dbid(QUERY_2_DATABASE_ID)
            .query(QUERY_2_STATEMENT)
            .queryNormalized(QUERY_2_STATEMENT)
            .resultNumber(QUERY_2_RESULT_NUMBER)
            .resultHash(QUERY_2_RESULT_HASH)
            .lastModified(QUERY_2_LAST_MODIFIED)
            .created(QUERY_2_CREATED)
            .queryHash(QUERY_2_QUERY_HASH)
            .execution(QUERY_2_EXECUTION)
            .build();

    public final static Long QUERY_3_ID = 3L;
    public final static String QUERY_3_STATEMENT = "SELECT `location`, `mintemp` FROM `weather_aus` WHERE `mintemp` > 10";
    public final static String QUERY_3_QUERY_HASH = "a3d3dd94ebc7653bb5a3b55dd8ed5e91d3d13c335c6855a1eb4eb7ca14c36ced";
    public final static Long QUERY_3_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long QUERY_3_DATABASE_ID = DATABASE_2_ID;
    public final static String QUERY_3_RESULT_HASH = "ff3f7cbe1b96d396957f6e39e55b8b1b577fa3d305d4795af99594cfd30cb80d";
    public final static Instant QUERY_3_CREATED = Instant.now().minus(3, MINUTES);
    public final static Instant QUERY_3_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_3_LAST_MODIFIED = Instant.ofEpochSecond(1541588353);
    public final static Long QUERY_3_RESULT_NUMBER = 2L;
    public final static Boolean QUERY_3_PERSISTED = true;

    public final static Query QUERY_3 = Query.builder()
            .id(QUERY_3_ID)
            .query(QUERY_3_STATEMENT)
            .queryHash(QUERY_3_QUERY_HASH)
            .resultHash(QUERY_3_RESULT_HASH)
            .created(QUERY_3_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_3_PERSISTED)
            .build();

    public final static QueryDto QUERY_3_DTO = QueryDto.builder()
            .id(QUERY_3_ID)
            .cid(QUERY_3_CONTAINER_ID)
            .dbid(QUERY_3_DATABASE_ID)
            .query(QUERY_3_STATEMENT)
            .queryNormalized(QUERY_3_STATEMENT)
            .resultNumber(QUERY_3_RESULT_NUMBER)
            .resultHash(QUERY_3_RESULT_HASH)
            .lastModified(QUERY_3_LAST_MODIFIED)
            .created(QUERY_3_CREATED)
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
    public final static Instant QUERY_4_LAST_MODIFIED = Instant.ofEpochSecond(1541588454);
    public final static Boolean QUERY_4_PERSISTED = false;

    public final static Query QUERY_4 = Query.builder()
            .id(QUERY_4_ID)
            .query(QUERY_4_STATEMENT)
            .queryHash(QUERY_4_QUERY_HASH)
            .resultHash(QUERY_4_RESULT_HASH)
            .created(QUERY_4_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_4_PERSISTED)
            .createdBy(USER_1_USERNAME)
            .build();

    public final static Long QUERY_4_RESULT_NUMBER = 6L;
    public final static Long QUERY_4_RESULT_ID = 4L;
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
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .result(QUERY_4_RESULT_RESULT)
            .build();

    public final static QueryDto QUERY_4_DTO = QueryDto.builder()
            .id(QUERY_4_ID)
            .cid(QUERY_4_CONTAINER_ID)
            .dbid(QUERY_4_DATABASE_ID)
            .query(QUERY_4_STATEMENT)
            .queryNormalized(QUERY_4_STATEMENT)
            .resultNumber(QUERY_4_RESULT_NUMBER)
            .resultHash(QUERY_4_RESULT_HASH)
            .lastModified(QUERY_4_LAST_MODIFIED)
            .created(QUERY_4_CREATED)
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
    public final static Instant QUERY_5_LAST_MODIFIED = Instant.ofEpochSecond(1551588555);
    public final static Long QUERY_5_RESULT_NUMBER = 6L;
    public final static Boolean QUERY_5_PERSISTED = true;

    public final static Query QUERY_5 = Query.builder()
            .id(QUERY_5_ID)
            .query(QUERY_5_STATEMENT)
            .queryHash(QUERY_5_QUERY_HASH)
            .resultHash(QUERY_5_RESULT_HASH)
            .created(QUERY_5_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_5_PERSISTED)
            .build();

    public final static QueryDto QUERY_5_DTO = QueryDto.builder()
            .id(QUERY_5_ID)
            .cid(QUERY_5_CONTAINER_ID)
            .dbid(QUERY_5_DATABASE_ID)
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
    public final static Instant QUERY_6_LAST_MODIFIED = Instant.ofEpochSecond(1551588555);
    public final static Long QUERY_6_RESULT_NUMBER = 1L;
    public final static Boolean QUERY_6_PERSISTED = true;

    public final static Query QUERY_6 = Query.builder()
            .id(QUERY_6_ID)
            .query(QUERY_6_STATEMENT)
            .queryHash(QUERY_6_QUERY_HASH)
            .resultHash(QUERY_6_RESULT_HASH)
            .created(QUERY_6_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_6_PERSISTED)
            .build();

    public final static QueryDto QUERY_6_DTO = QueryDto.builder()
            .id(QUERY_6_ID)
            .cid(QUERY_6_CONTAINER_ID)
            .dbid(QUERY_6_DATABASE_ID)
            .query(QUERY_6_STATEMENT)
            .queryNormalized(QUERY_6_STATEMENT)
            .resultNumber(QUERY_6_RESULT_NUMBER)
            .resultHash(QUERY_6_RESULT_HASH)
            .lastModified(QUERY_6_LAST_MODIFIED)
            .created(QUERY_6_CREATED)
            .queryHash(QUERY_6_QUERY_HASH)
            .execution(QUERY_6_EXECUTION)
            .build();

    public final static List<TableColumn> TABLE_1_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_1_1_ID)
                    .ordinalPosition(COLUMN_1_1_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_1_NAME)
                    .internalName(COLUMN_1_1_INTERNAL_NAME)
                    .columnType(COLUMN_1_1_TYPE)
                    .dfid(COLUMN_1_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_1_NULL)
                    .autoGenerated(COLUMN_1_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_1_PRIMARY)
                    .enumValues(COLUMN_1_1_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_2_ID)
                    .ordinalPosition(COLUMN_1_2_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_2_NAME)
                    .internalName(COLUMN_1_2_INTERNAL_NAME)
                    .columnType(COLUMN_1_2_TYPE)
                    .dfid(COLUMN_1_2_DATE_FORMAT)
                    .dateFormat(IMAGE_DATE_1)
                    .isNullAllowed(COLUMN_1_2_NULL)
                    .autoGenerated(COLUMN_1_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_2_PRIMARY)
                    .enumValues(COLUMN_1_2_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_3_ID)
                    .ordinalPosition(COLUMN_1_3_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_3_NAME)
                    .internalName(COLUMN_1_3_INTERNAL_NAME)
                    .columnType(COLUMN_1_3_TYPE)
                    .dfid(COLUMN_1_3_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_3_NULL)
                    .autoGenerated(COLUMN_1_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_3_PRIMARY)
                    .enumValues(COLUMN_1_3_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_4_ID)
                    .ordinalPosition(COLUMN_1_4_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_4_NAME)
                    .internalName(COLUMN_1_4_INTERNAL_NAME)
                    .columnType(COLUMN_1_4_TYPE)
                    .dfid(COLUMN_1_4_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_4_NULL)
                    .autoGenerated(COLUMN_1_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_4_PRIMARY)
                    .enumValues(COLUMN_1_4_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_5_ID)
                    .ordinalPosition(COLUMN_1_5_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_5_NAME)
                    .internalName(COLUMN_1_5_INTERNAL_NAME)
                    .columnType(COLUMN_1_5_TYPE)
                    .dfid(COLUMN_1_5_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_5_NULL)
                    .autoGenerated(COLUMN_1_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_5_PRIMARY)
                    .enumValues(COLUMN_1_5_ENUM_VALUES)
                    .creator(USER_1)
                    .build());

    public final static Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .database(DATABASE_1)
            .created(Instant.now())
            .internalName(TABLE_1_INTERNALNAME)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .columns(TABLE_1_COLUMNS)
            .constraints(null) /* TABLE_1_CONSTRAINTS */
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_1_CREATED)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .build();

    public final static Table TABLE_1_SIMPLE = Table.builder()
            .id(TABLE_1_ID)
            .database(null /* for jpa */)
            .created(Instant.now())
            .internalName(TABLE_1_INTERNALNAME)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .columns(List.of() /* for jpa */)
            .constraints(null /* for jpa */) /* TABLE_1_CONSTRAINTS */
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_1_CREATED)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .build();

    public final static List<TableColumn> TABLE_2_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_2_1_ID)
                    .ordinalPosition(COLUMN_2_1_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_2_ID)
                    .name(COLUMN_2_1_NAME)
                    .internalName(COLUMN_2_1_INTERNAL_NAME)
                    .columnType(COLUMN_2_1_TYPE)
                    .dfid(COLUMN_2_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_2_1_NULL)
                    .autoGenerated(COLUMN_2_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_1_PRIMARY)
                    .enumValues(COLUMN_2_1_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_2_2_ID)
                    .ordinalPosition(COLUMN_2_2_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_2_ID)
                    .name(COLUMN_2_2_NAME)
                    .internalName(COLUMN_2_2_INTERNAL_NAME)
                    .columnType(COLUMN_2_2_TYPE)
                    .dfid(COLUMN_2_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_2_2_NULL)
                    .autoGenerated(COLUMN_2_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_2_PRIMARY)
                    .enumValues(COLUMN_2_2_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_2_3_ID)
                    .ordinalPosition(COLUMN_2_3_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_2_ID)
                    .name(COLUMN_2_3_NAME)
                    .internalName(COLUMN_2_3_INTERNAL_NAME)
                    .columnType(COLUMN_2_3_TYPE)
                    .dfid(COLUMN_2_3_DATE_FORMAT)
                    .isNullAllowed(COLUMN_2_3_NULL)
                    .autoGenerated(COLUMN_2_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_3_PRIMARY)
                    .enumValues(COLUMN_2_3_ENUM_VALUES)
                    .creator(USER_1)
                    .build());

    public final static Table TABLE_2 = Table.builder()
            .id(TABLE_2_ID)
            .database(DATABASE_1)
            .created(Instant.now())
            .internalName(TABLE_2_INTERNALNAME)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .columns(TABLE_2_COLUMNS)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_2_CREATED)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .build();

    public final static Table TABLE_2_SIMPLE = Table.builder()
            .id(TABLE_2_ID)
            .database(null /* for jpa */)
            .created(Instant.now())
            .internalName(TABLE_2_INTERNALNAME)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .columns(List.of() /* for jpa */)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_2_CREATED)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .build();

    public final static Constraints TABLE_1_CONSTRAINTS = Constraints.builder()
            .foreignKeys(List.of(ForeignKey.builder()
                    .referencedTable(TABLE_2)
                    .references(List.of(
                            ForeignKeyReference.builder().column(TABLE_1_COLUMNS.get(2)).referencedColumn(TABLE_1_COLUMNS.get(0)).build())
                    ).build()
            ))
            .uniques(List.of(Unique.builder().columns(List.of(TABLE_1_COLUMNS.get(1))).build()))
            .checks(Set.of("`mintemp` > 0"))
            .build();

    public final static Constraints TABLE_2_CONSTRAINTS = Constraints.builder()
            .uniques(List.of(Unique.builder().columns(List.of(TABLE_2_COLUMNS.get(0))).build()))
            .build();

    public final static List<TableColumn> TABLE_3_COLUMNS = List.of(TableColumn.builder()
                    .id(9L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .autoGenerated(true)
                    .columnType(TableColumnType.NUMBER)
                    .name("id")
                    .internalName("id")
                    .isNullAllowed(false)
                    .isPrimaryKey(true)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(10L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("linie")
                    .internalName("linie")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(11L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("richtung")
                    .internalName("richtung")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(12L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("betriebsdatum")
                    .internalName("betriebsdatum")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dfid(IMAGE_DATE_2_ID)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(13L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(4)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fahrzeug")
                    .internalName("fahrzeug")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(14L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(5)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("kurs")
                    .internalName("kurs")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(15L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(6)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("seq_von")
                    .internalName("seq_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(16L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(7)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_diva_von")
                    .internalName("halt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(17L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(8)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_diva_von")
                    .internalName("halt_punkt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(18L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(9)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("halt_kurz_von1")
                    .internalName("halt_kurz_von1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(19L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(10)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("datum_von")
                    .internalName("datum_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dfid(IMAGE_DATE_2_ID)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(20L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(11)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_an_von")
                    .internalName("soll_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(21L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(12)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_an_von")
                    .internalName("ist_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(22L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(13)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_ab_von")
                    .internalName("soll_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(23L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(14)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_ab_von")
                    .internalName("ist_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(24L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(15)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("seq_nach")
                    .internalName("seq_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(25L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(16)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_diva_nach")
                    .internalName("halt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(26L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(17)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_diva_nach")
                    .internalName("halt_punkt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(27L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(18)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("halt_kurz_nach1")
                    .internalName("halt_kurz_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(28L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(19)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("datum_nach")
                    .internalName("datum_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dfid(IMAGE_DATE_2_ID)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(29L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(20)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_an_nach")
                    .internalName("soll_an_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(30L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(21)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_an_nach1")
                    .internalName("ist_an_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(31L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(22)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_ab_nach")
                    .internalName("soll_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(32L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(23)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_ab_nach")
                    .internalName("ist_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(33L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(24)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fahrt_id")
                    .internalName("fahrt_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(34L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(25)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fahrweg_id")
                    .internalName("fahrweg_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(35L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(26)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fw_no")
                    .internalName("fw_no")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(36L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(27)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fw_typ")
                    .internalName("fw_typ")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(37L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(28)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fw_kurz")
                    .internalName("fw_kurz")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(38L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(29)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("fw_lang")
                    .internalName("fw_lang")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(39L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(30)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("umlauf_von")
                    .internalName("umlauf_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(40L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(31)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_id_von")
                    .internalName("halt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(41L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(32)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_id_nach")
                    .internalName("halt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(42L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(33)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_id_von")
                    .internalName("halt_punkt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(43L)
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(34)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_id_nach")
                    .internalName("halt_punkt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .dateFormat(null)
                    .enumValues(null)
                    .creator(USER_1)
                    .build());

    public final static Constraints TABLE_3_CONSTRAINTS = Constraints.builder()
            .uniques(List.of(Unique.builder().columns(List.of(TABLE_3_COLUMNS.get(0))).build()))
            .build();

    public final static Table TABLE_3 = Table.builder()
            .id(TABLE_3_ID)
            .database(DATABASE_1)
            .created(Instant.now())
            .internalName(TABLE_3_INTERNALNAME)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_3_QUEUE_NAME)
            .routingKey(TABLE_3_ROUTING_KEY)
            .columns(TABLE_3_COLUMNS)
            .constraints(TABLE_3_CONSTRAINTS)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_3_CREATED)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .build();

    public final static Table TABLE_3_SIMPLE = Table.builder()
            .id(TABLE_3_ID)
            .database(null /* for jpa */)
            .created(Instant.now())
            .internalName(TABLE_3_INTERNALNAME)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_3_QUEUE_NAME)
            .routingKey(TABLE_3_ROUTING_KEY)
            .columns(List.of() /* for jpa */)
            .constraints(TABLE_3_CONSTRAINTS)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_3_CREATED)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .build();

    public final static List<TableColumn> TABLE_4_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_4_1_ID)
                    .ordinalPosition(COLUMN_4_1_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_1_NAME)
                    .internalName(COLUMN_4_1_INTERNAL_NAME)
                    .columnType(COLUMN_4_1_TYPE)
                    .dfid(COLUMN_4_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_1_NULL)
                    .autoGenerated(COLUMN_4_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_1_PRIMARY)
                    .enumValues(COLUMN_4_1_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_2_ID)
                    .ordinalPosition(COLUMN_4_2_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_2_NAME)
                    .internalName(COLUMN_4_2_INTERNAL_NAME)
                    .columnType(COLUMN_4_2_TYPE)
                    .dfid(COLUMN_4_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_2_NULL)
                    .autoGenerated(COLUMN_4_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_2_PRIMARY)
                    .enumValues(COLUMN_4_2_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_3_ID)
                    .ordinalPosition(COLUMN_4_3_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_3_NAME)
                    .internalName(COLUMN_4_3_INTERNAL_NAME)
                    .columnType(COLUMN_4_3_TYPE)
                    .dfid(COLUMN_4_3_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_3_NULL)
                    .autoGenerated(COLUMN_4_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_3_PRIMARY)
                    .enumValues(COLUMN_4_3_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_4_ID)
                    .ordinalPosition(COLUMN_4_4_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_4_NAME)
                    .internalName(COLUMN_4_4_INTERNAL_NAME)
                    .columnType(COLUMN_4_4_TYPE)
                    .dfid(COLUMN_4_4_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_4_NULL)
                    .autoGenerated(COLUMN_4_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_4_PRIMARY)
                    .enumValues(COLUMN_4_4_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_5_ID)
                    .ordinalPosition(COLUMN_4_5_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_5_NAME)
                    .internalName(COLUMN_4_5_INTERNAL_NAME)
                    .columnType(COLUMN_4_5_TYPE)
                    .dfid(COLUMN_4_5_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_5_NULL)
                    .autoGenerated(COLUMN_4_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_5_PRIMARY)
                    .enumValues(COLUMN_4_5_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_6_ID)
                    .ordinalPosition(COLUMN_4_6_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_6_NAME)
                    .internalName(COLUMN_4_6_INTERNAL_NAME)
                    .columnType(COLUMN_4_6_TYPE)
                    .dfid(COLUMN_4_6_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_6_NULL)
                    .autoGenerated(COLUMN_4_6_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_6_PRIMARY)
                    .enumValues(COLUMN_4_6_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_7_ID)
                    .ordinalPosition(COLUMN_4_7_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_7_NAME)
                    .internalName(COLUMN_4_7_INTERNAL_NAME)
                    .columnType(COLUMN_4_7_TYPE)
                    .dfid(COLUMN_4_7_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_7_NULL)
                    .autoGenerated(COLUMN_4_7_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_7_PRIMARY)
                    .enumValues(COLUMN_4_7_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_8_ID)
                    .ordinalPosition(COLUMN_4_8_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_8_NAME)
                    .internalName(COLUMN_4_8_INTERNAL_NAME)
                    .columnType(COLUMN_4_8_TYPE)
                    .dfid(COLUMN_4_8_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_8_NULL)
                    .autoGenerated(COLUMN_4_8_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_8_PRIMARY)
                    .enumValues(COLUMN_4_8_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_9_ID)
                    .ordinalPosition(COLUMN_4_9_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_9_NAME)
                    .internalName(COLUMN_4_9_INTERNAL_NAME)
                    .columnType(COLUMN_4_9_TYPE)
                    .dfid(COLUMN_4_9_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_9_NULL)
                    .autoGenerated(COLUMN_4_9_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_9_PRIMARY)
                    .enumValues(COLUMN_4_9_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_10_ID)
                    .ordinalPosition(COLUMN_4_10_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_10_NAME)
                    .internalName(COLUMN_4_10_INTERNAL_NAME)
                    .columnType(COLUMN_4_10_TYPE)
                    .dfid(COLUMN_4_10_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_10_NULL)
                    .autoGenerated(COLUMN_4_10_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_10_PRIMARY)
                    .enumValues(COLUMN_4_10_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_11_ID)
                    .ordinalPosition(COLUMN_4_11_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_11_NAME)
                    .internalName(COLUMN_4_11_INTERNAL_NAME)
                    .columnType(COLUMN_4_11_TYPE)
                    .dfid(COLUMN_4_11_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_11_NULL)
                    .autoGenerated(COLUMN_4_11_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_11_PRIMARY)
                    .enumValues(COLUMN_4_11_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_12_ID)
                    .ordinalPosition(COLUMN_4_12_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_12_NAME)
                    .internalName(COLUMN_4_12_INTERNAL_NAME)
                    .columnType(COLUMN_4_12_TYPE)
                    .dfid(COLUMN_4_12_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_12_NULL)
                    .autoGenerated(COLUMN_4_12_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_12_PRIMARY)
                    .enumValues(COLUMN_4_12_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_13_ID)
                    .ordinalPosition(COLUMN_4_13_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_13_NAME)
                    .internalName(COLUMN_4_13_INTERNAL_NAME)
                    .columnType(COLUMN_4_13_TYPE)
                    .dfid(COLUMN_4_13_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_13_NULL)
                    .autoGenerated(COLUMN_4_13_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_13_PRIMARY)
                    .enumValues(COLUMN_4_13_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_14_ID)
                    .ordinalPosition(COLUMN_4_14_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_14_NAME)
                    .internalName(COLUMN_4_14_INTERNAL_NAME)
                    .columnType(COLUMN_4_14_TYPE)
                    .dfid(COLUMN_4_14_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_14_NULL)
                    .autoGenerated(COLUMN_4_14_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_14_PRIMARY)
                    .enumValues(COLUMN_4_14_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_15_ID)
                    .ordinalPosition(COLUMN_4_15_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_15_NAME)
                    .internalName(COLUMN_4_15_INTERNAL_NAME)
                    .columnType(COLUMN_4_15_TYPE)
                    .dfid(COLUMN_4_15_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_15_NULL)
                    .autoGenerated(COLUMN_4_15_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_15_PRIMARY)
                    .enumValues(COLUMN_4_15_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_16_ID)
                    .ordinalPosition(COLUMN_4_16_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_16_NAME)
                    .internalName(COLUMN_4_16_INTERNAL_NAME)
                    .columnType(COLUMN_4_16_TYPE)
                    .dfid(COLUMN_4_16_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_16_NULL)
                    .autoGenerated(COLUMN_4_16_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_16_PRIMARY)
                    .enumValues(COLUMN_4_16_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_17_ID)
                    .ordinalPosition(COLUMN_4_17_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_17_NAME)
                    .internalName(COLUMN_4_17_INTERNAL_NAME)
                    .columnType(COLUMN_4_17_TYPE)
                    .dfid(COLUMN_4_17_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_17_NULL)
                    .autoGenerated(COLUMN_4_17_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_17_PRIMARY)
                    .enumValues(COLUMN_4_17_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_18_ID)
                    .ordinalPosition(COLUMN_4_18_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_18_NAME)
                    .internalName(COLUMN_4_18_INTERNAL_NAME)
                    .columnType(COLUMN_4_18_TYPE)
                    .dfid(COLUMN_4_18_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_18_NULL)
                    .autoGenerated(COLUMN_4_18_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_18_PRIMARY)
                    .enumValues(COLUMN_4_18_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_19_ID)
                    .ordinalPosition(COLUMN_4_19_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_19_NAME)
                    .internalName(COLUMN_4_19_INTERNAL_NAME)
                    .columnType(COLUMN_4_19_TYPE)
                    .dfid(COLUMN_4_19_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_19_NULL)
                    .autoGenerated(COLUMN_4_19_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_19_PRIMARY)
                    .enumValues(COLUMN_4_19_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_20_ID)
                    .ordinalPosition(COLUMN_4_20_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_20_NAME)
                    .internalName(COLUMN_4_20_INTERNAL_NAME)
                    .columnType(COLUMN_4_20_TYPE)
                    .dfid(COLUMN_4_20_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_20_NULL)
                    .autoGenerated(COLUMN_4_20_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_20_PRIMARY)
                    .enumValues(COLUMN_4_20_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_4_21_ID)
                    .ordinalPosition(COLUMN_4_21_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_4_ID)
                    .name(COLUMN_4_21_NAME)
                    .internalName(COLUMN_4_21_INTERNAL_NAME)
                    .columnType(COLUMN_4_21_TYPE)
                    .dfid(COLUMN_4_21_DATE_FORMAT)
                    .isNullAllowed(COLUMN_4_21_NULL)
                    .autoGenerated(COLUMN_4_21_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_21_PRIMARY)
                    .enumValues(COLUMN_4_21_ENUM_VALUES)
                    .creator(USER_2)
                    .build());

    public final static Constraints TABLE_4_CONSTRAINTS = Constraints.builder()
            .uniques(List.of(Unique.builder().columns(List.of(TABLE_4_COLUMNS.get(0))).build()))
            .build();

    public final static Table TABLE_4 = Table.builder()
            .id(TABLE_4_ID)
            .created(Instant.now())
            .internalName(TABLE_4_INTERNALNAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .lastModified(TABLE_4_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .queueName(TABLE_4_QUEUE_NAME)
            .routingKey(TABLE_4_ROUTING_KEY)
            .columns(TABLE_4_COLUMNS)
            .constraints(TABLE_4_CONSTRAINTS)
            .creator(USER_1)
            .owner(USER_1)
            .build();

    public final static Table TABLE_4_SIMPLE = Table.builder()
            .id(TABLE_4_ID)
            .created(Instant.now())
            .internalName(TABLE_4_INTERNALNAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .lastModified(TABLE_4_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .queueName(TABLE_4_QUEUE_NAME)
            .routingKey(TABLE_4_ROUTING_KEY)
            .columns(List.of() /* for jpa */)
            .constraints(TABLE_4_CONSTRAINTS)
            .creator(null /* for jpa */)
            .owner(null  /* for jpa */)
            .build();

    public final static List<ForeignKeyCreateDto> TABLE_4_FOREIGN_KEYS_INVALID_CREATE = List.of(ForeignKeyCreateDto.builder()
            .columns(List.of("somecolumn"))
            .referencedTable("sometable")
            .referencedColumns(List.of("someothercolumn"))
            .build());

    public final static ConstraintsCreateDto TABLE_4_CONSTRAINTS_INVALID_CREATE = ConstraintsCreateDto.builder()
            .foreignKeys(TABLE_4_FOREIGN_KEYS_INVALID_CREATE)
            .build();

    public final static List<ColumnCreateDto> TABLE_4_COLUMNS_INVALID_CREATE = List.of(ColumnCreateDto.builder()
            .name(COLUMN_4_2_NAME)
            .type(COLUMN_4_2_TYPE_DTO)
            .dfid(COLUMN_4_2_DATE_FORMAT)
            .nullAllowed(COLUMN_4_2_NULL)
            .primaryKey(COLUMN_4_2_PRIMARY)
            .enumValues(COLUMN_4_2_ENUM_VALUES_ARRAY)
            .build());

    public final static List<ColumnCreateDto> TABLE_4_COLUMNS_CREATE = List.of(ColumnCreateDto.builder()
                    .name(COLUMN_4_1_NAME)
                    .type(COLUMN_4_1_TYPE_DTO)
                    .dfid(COLUMN_4_1_DATE_FORMAT)
                    .nullAllowed(COLUMN_4_1_NULL)
                    .primaryKey(COLUMN_4_1_PRIMARY)
                    .enumValues(COLUMN_4_1_ENUM_VALUES_ARRAY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_4_2_NAME)
                    .type(COLUMN_4_2_TYPE_DTO)
                    .dfid(COLUMN_4_2_DATE_FORMAT)
                    .nullAllowed(COLUMN_4_2_NULL)
                    .primaryKey(COLUMN_4_2_PRIMARY)
                    .enumValues(COLUMN_4_2_ENUM_VALUES_ARRAY)
                    .build());

    public final static TableCreateDto TABLE_4_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_4_NAME)
            .description(TABLE_4_DESCRIPTION)
            .columns(TABLE_4_COLUMNS_CREATE)
            .constraints(null)
            .build();

    public final static TableCreateDto TABLE_4_INVALID_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_4_NAME)
            .description(TABLE_4_DESCRIPTION)
            .columns(TABLE_4_COLUMNS_CREATE)
            .constraints(TABLE_4_CONSTRAINTS_INVALID_CREATE)
            .build();

    public final static List<TableColumn> TABLE_5_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_5_1_ID)
                    .ordinalPosition(COLUMN_5_1_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_5_ID)
                    .name(COLUMN_5_1_NAME)
                    .internalName(COLUMN_5_1_INTERNAL_NAME)
                    .columnType(COLUMN_5_1_TYPE)
                    .dfid(COLUMN_5_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_5_1_NULL)
                    .autoGenerated(COLUMN_5_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_1_PRIMARY)
                    .enumValues(COLUMN_5_1_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_2_ID)
                    .ordinalPosition(COLUMN_5_2_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_5_ID)
                    .name(COLUMN_5_2_NAME)
                    .internalName(COLUMN_5_2_INTERNAL_NAME)
                    .columnType(COLUMN_5_2_TYPE)
                    .dfid(COLUMN_5_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_5_2_NULL)
                    .autoGenerated(COLUMN_5_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_2_PRIMARY)
                    .enumValues(COLUMN_5_2_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_5_3_ID)
                    .ordinalPosition(COLUMN_5_3_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_5_ID)
                    .name(COLUMN_5_3_NAME)
                    .internalName(COLUMN_5_3_INTERNAL_NAME)
                    .columnType(COLUMN_5_3_TYPE)
                    .dfid(COLUMN_5_3_DATE_FORMAT)
                    .isNullAllowed(COLUMN_5_3_NULL)
                    .autoGenerated(COLUMN_5_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_3_PRIMARY)
                    .enumValues(COLUMN_5_3_ENUM_VALUES)
                    .creator(USER_2)
                    .build());

    public final static Constraints TABLE_5_CONSTRAINTS = Constraints.builder()
            .uniques(List.of(Unique.builder().columns(List.of(TABLE_5_COLUMNS.get(0))).build()))
            .build();

    public final static Table TABLE_5 = Table.builder()
            .id(TABLE_5_ID)
            .created(Instant.now())
            .internalName(TABLE_5_INTERNALNAME)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .queueName(TABLE_5_QUEUE_NAME)
            .routingKey(TABLE_5_ROUTING_KEY)
            .columns(TABLE_5_COLUMNS)
            .constraints(TABLE_5_CONSTRAINTS)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_5_CREATED)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .build();

    public final static Table TABLE_5_SIMPLE = Table.builder()
            .id(TABLE_5_ID)
            .created(Instant.now())
            .internalName(TABLE_5_INTERNALNAME)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .queueName(TABLE_5_QUEUE_NAME)
            .routingKey(TABLE_5_ROUTING_KEY)
            .columns(List.of() /* for jpa */)
            .constraints(TABLE_5_CONSTRAINTS)
            .creator(null /* for jpa */)
            .owner(null /* for jpa */)
            .created(TABLE_5_CREATED)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .build();

    public final static List<ColumnCreateDto> TABLE_5_COLUMNS_CREATE = List.of(
            ColumnCreateDto.builder()
                    .name(COLUMN_5_1_NAME)
                    .type(COLUMN_5_1_TYPE_DTO)
                    .dfid(COLUMN_5_1_DATE_FORMAT)
                    .nullAllowed(COLUMN_5_1_NULL)
                    .primaryKey(COLUMN_5_1_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_2_NAME)
                    .type(COLUMN_5_2_TYPE_DTO)
                    .length(COLUMN_5_2_LENGTH)
                    .dfid(COLUMN_5_2_DATE_FORMAT)
                    .nullAllowed(COLUMN_5_2_NULL)
                    .primaryKey(COLUMN_5_2_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_3_NAME)
                    .type(COLUMN_5_3_TYPE_DTO)
                    .length(COLUMN_5_3_LENGTH)
                    .dfid(COLUMN_5_3_DATE_FORMAT)
                    .nullAllowed(COLUMN_5_3_NULL)
                    .primaryKey(COLUMN_5_3_PRIMARY)
                    .build(),
            ColumnCreateDto.builder()
                    .name(COLUMN_5_4_NAME)
                    .type(COLUMN_5_4_TYPE_DTO)
                    .dfid(COLUMN_5_4_DATE_FORMAT)
                    .nullAllowed(COLUMN_5_4_NULL)
                    .primaryKey(COLUMN_5_4_PRIMARY)
                    .build());

    public final static List<List<String>> TABLE_5_UNIQUES_CREATE = List.of(
            List.of(COLUMN_5_1_NAME),
            List.of(COLUMN_5_2_NAME, COLUMN_5_3_NAME));

    public final static List<ForeignKeyCreateDto> TABLE_5_FOREIGN_KEYS_CREATE = List.of(ForeignKeyCreateDto.builder()
            .columns(List.of(COLUMN_5_4_NAME))
            .referencedTable(TABLE_4_NAME)
            .referencedColumns(List.of(COLUMN_4_1_NAME))
            .build());

    public final static List<String> TABLE_5_CHECKS_CREATE = List.of(
            COLUMN_5_2_NAME + " != " + COLUMN_5_3_NAME);

    public final static ConstraintsCreateDto TABLE_5_CONSTRAINTS_CREATE = ConstraintsCreateDto.builder()
            .uniques(TABLE_5_UNIQUES_CREATE)
            .foreignKeys(TABLE_5_FOREIGN_KEYS_CREATE)
            .checks(TABLE_5_CHECKS_CREATE)
            .build();

    public final static TableCreateDto TABLE_5_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_5_NAME)
            .description(TABLE_5_DESCRIPTION)
            .columns(TABLE_5_COLUMNS_CREATE)
            .constraints(TABLE_5_CONSTRAINTS_CREATE)
            .build();

    public final static Long COLUMN_6_1_ID = 26L;
    public final static Integer COLUMN_6_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_6_1_PRIMARY = true;
    public final static String COLUMN_6_1_NAME = "name_id";
    public final static String COLUMN_6_1_INTERNAL_NAME = "name_id";
    public final static TableColumnType COLUMN_6_1_TYPE = TableColumnType.NUMBER;
    public final static Long COLUMN_6_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_6_1_NULL = false;
    public final static Boolean COLUMN_6_1_UNIQUE = false;
    public final static Boolean COLUMN_6_1_AUTO_GENERATED = false;
    public final static String COLUMN_6_1_FOREIGN_KEY = null;
    public final static String COLUMN_6_1_CHECK = null;
    public final static List<String> COLUMN_6_1_ENUM_VALUES = null;

    public final static Long COLUMN_6_2_ID = 27L;
    public final static Integer COLUMN_6_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_6_2_PRIMARY = true;
    public final static String COLUMN_6_2_NAME = "zoo_id";
    public final static String COLUMN_6_2_INTERNAL_NAME = "zoo_id";
    public final static TableColumnType COLUMN_6_2_TYPE = TableColumnType.NUMBER;
    public final static Long COLUMN_6_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_6_2_NULL = false;
    public final static Boolean COLUMN_6_2_UNIQUE = false;
    public final static Boolean COLUMN_6_2_AUTO_GENERATED = false;
    public final static String COLUMN_6_2_FOREIGN_KEY = null;
    public final static String COLUMN_6_2_CHECK = null;
    public final static List<String> COLUMN_6_2_ENUM_VALUES = null;

    public final static List<TableColumn> TABLE_6_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_6_1_ID)
                    .ordinalPosition(COLUMN_6_1_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_6_ID)
                    .name(COLUMN_6_1_NAME)
                    .internalName(COLUMN_6_1_INTERNAL_NAME)
                    .columnType(COLUMN_6_1_TYPE)
                    .dfid(COLUMN_6_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_6_1_NULL)
                    .autoGenerated(COLUMN_6_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_6_1_PRIMARY)
                    .enumValues(COLUMN_6_1_ENUM_VALUES)
                    .creator(USER_2)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_6_2_ID)
                    .ordinalPosition(COLUMN_6_2_ORDINALPOS)
                    .cdbid(DATABASE_2_ID)
                    .tid(TABLE_6_ID)
                    .name(COLUMN_6_2_NAME)
                    .internalName(COLUMN_6_2_INTERNAL_NAME)
                    .columnType(COLUMN_6_2_TYPE)
                    .dfid(COLUMN_6_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_6_2_NULL)
                    .autoGenerated(COLUMN_6_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_6_2_PRIMARY)
                    .enumValues(COLUMN_6_2_ENUM_VALUES)
                    .creator(USER_2)
                    .build());

    public final static Table TABLE_6 = Table.builder()
            .id(TABLE_6_ID)
            .created(Instant.now())
            .internalName(TABLE_6_INTERNAL_NAME)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .queueName(TABLE_6_QUEUE_NAME)
            .routingKey(TABLE_6_ROUTING_KEY)
            .columns(TABLE_6_COLUMNS)
            .creator(USER_1)
            .owner(USER_1)
            .created(TABLE_6_CREATED)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .build();

    public final static Table TABLE_6_SIMPLE = Table.builder()
            .id(TABLE_6_ID)
            .created(Instant.now())
            .internalName(TABLE_6_INTERNAL_NAME)
            .description(TABLE_6_DESCRIPTION)
            .name(TABLE_6_NAME)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .queueName(TABLE_6_QUEUE_NAME)
            .routingKey(TABLE_6_ROUTING_KEY)
            .columns(List.of() /* for jpa */)
            .creator(null /* for jpa */)
            .owner(null /* for jpa */)
            .created(TABLE_6_CREATED)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .build();

    public final static Long VIEW_1_ID = 1L;
    public final static Boolean VIEW_1_INITIAL_VIEW = false;
    public final static String VIEW_1_NAME = "JUnit";
    public final static String VIEW_1_INTERNAL_NAME = "junit";
    public final static Long VIEW_1_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_1_PUBLIC = true;
    public final static String VIEW_1_QUERY = "select `location`, `lat`, `lng` from `weather_location`";

    public final static List<TableColumn> VIEW_1_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_2_1_ID)
                    .ordinalPosition(COLUMN_2_1_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_2_ID)
                    .name(COLUMN_2_1_NAME)
                    .internalName(COLUMN_2_1_INTERNAL_NAME)
                    .columnType(COLUMN_2_1_TYPE)
                    .dfid(COLUMN_2_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_2_1_NULL)
                    .autoGenerated(COLUMN_2_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_1_PRIMARY)
                    .enumValues(COLUMN_2_1_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_2_2_ID)
                    .ordinalPosition(COLUMN_2_2_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_2_ID)
                    .name(COLUMN_2_2_NAME)
                    .internalName(COLUMN_2_2_INTERNAL_NAME)
                    .columnType(COLUMN_2_2_TYPE)
                    .dfid(COLUMN_2_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_2_2_NULL)
                    .autoGenerated(COLUMN_2_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_2_PRIMARY)
                    .enumValues(COLUMN_2_2_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_2_3_ID)
                    .ordinalPosition(COLUMN_2_3_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_2_ID)
                    .name(COLUMN_2_3_NAME)
                    .internalName(COLUMN_2_3_INTERNAL_NAME)
                    .columnType(COLUMN_2_3_TYPE)
                    .dfid(COLUMN_2_3_DATE_FORMAT)
                    .isNullAllowed(COLUMN_2_3_NULL)
                    .autoGenerated(COLUMN_2_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_3_PRIMARY)
                    .enumValues(COLUMN_2_3_ENUM_VALUES)
                    .creator(USER_1)
                    .build());

    public final static View VIEW_1 = View.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .query(VIEW_1_QUERY)
            .createdBy(USER_1_ID)
            .creator(USER_1)
            .columns(VIEW_1_COLUMNS)
            .build();

    public final static ViewDto VIEW_1_DTO = ViewDto.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .createdBy(USER_1_ID)
            .query(VIEW_1_QUERY)
            .build();

    public final static Long VIEW_2_ID = 2L;
    public final static Boolean VIEW_2_INITIAL_VIEW = false;
    public final static String VIEW_2_NAME = "JUnit2";
    public final static String VIEW_2_INTERNAL_NAME = "junit2";
    public final static Long VIEW_2_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_2_PUBLIC = true;
    public final static String VIEW_2_QUERY = "select `date`, `location`, `mintemp`, `rainfall` from `weather_aus` where `location` = 'Albury'";

    public final static List<TableColumn> VIEW_2_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_1_1_ID)
                    .ordinalPosition(COLUMN_1_1_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_1_NAME)
                    .internalName(COLUMN_1_1_INTERNAL_NAME)
                    .columnType(COLUMN_1_1_TYPE)
                    .dfid(COLUMN_1_1_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_1_NULL)
                    .autoGenerated(COLUMN_1_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_1_PRIMARY)
                    .enumValues(COLUMN_1_1_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_2_ID)
                    .ordinalPosition(COLUMN_1_2_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_2_NAME)
                    .internalName(COLUMN_1_2_INTERNAL_NAME)
                    .columnType(COLUMN_1_2_TYPE)
                    .dfid(COLUMN_1_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_2_NULL)
                    .autoGenerated(COLUMN_1_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_2_PRIMARY)
                    .enumValues(COLUMN_1_2_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_3_ID)
                    .ordinalPosition(COLUMN_1_3_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_3_NAME)
                    .internalName(COLUMN_1_3_INTERNAL_NAME)
                    .columnType(COLUMN_1_3_TYPE)
                    .dfid(COLUMN_1_3_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_3_NULL)
                    .autoGenerated(COLUMN_1_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_3_PRIMARY)
                    .enumValues(COLUMN_1_3_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_4_ID)
                    .ordinalPosition(COLUMN_1_4_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_4_NAME)
                    .internalName(COLUMN_1_4_INTERNAL_NAME)
                    .columnType(COLUMN_1_4_TYPE)
                    .dfid(COLUMN_1_4_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_4_NULL)
                    .autoGenerated(COLUMN_1_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_4_PRIMARY)
                    .enumValues(COLUMN_1_4_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_5_ID)
                    .ordinalPosition(COLUMN_1_5_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_5_NAME)
                    .internalName(COLUMN_1_5_INTERNAL_NAME)
                    .columnType(COLUMN_1_5_TYPE)
                    .dfid(COLUMN_1_5_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_5_NULL)
                    .autoGenerated(COLUMN_1_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_5_PRIMARY)
                    .enumValues(COLUMN_1_5_ENUM_VALUES)
                    .creator(USER_1)
                    .build());

    public final static View VIEW_2 = View.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .columns(VIEW_2_COLUMNS)
            .query(VIEW_2_QUERY)
            .creator(USER_1)
            .createdBy(USER_1_ID)
            .build();

    public final static ViewDto VIEW_2_DTO = ViewDto.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .query(VIEW_2_QUERY)
            .createdBy(USER_1_ID)
            .build();

    public final static Long VIEW_3_ID = 3L;
    public final static Boolean VIEW_3_INITIAL_VIEW = false;
    public final static String VIEW_3_NAME = "JUnit3";
    public final static String VIEW_3_INTERNAL_NAME = "junit3";
    public final static Long VIEW_3_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_3_PUBLIC = false;
    public final static String VIEW_3_QUERY = "select w.`mintemp`, w.`rainfall`, w.`location`, m.`date` from `weather_aus` w join `junit2` m on m.`location` = w.`location`";

    public final static List<TableColumn> VIEW_3_COLUMNS = List.of(TableColumn.builder()
                    .id(COLUMN_1_4_ID)
                    .ordinalPosition(COLUMN_1_4_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_4_NAME)
                    .internalName(COLUMN_1_4_INTERNAL_NAME)
                    .columnType(COLUMN_1_4_TYPE)
                    .dfid(COLUMN_1_4_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_4_NULL)
                    .autoGenerated(COLUMN_1_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_4_PRIMARY)
                    .enumValues(COLUMN_1_4_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_5_ID)
                    .ordinalPosition(COLUMN_1_5_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_5_NAME)
                    .internalName(COLUMN_1_5_INTERNAL_NAME)
                    .columnType(COLUMN_1_5_TYPE)
                    .dfid(COLUMN_1_5_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_5_NULL)
                    .autoGenerated(COLUMN_1_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_5_PRIMARY)
                    .enumValues(COLUMN_1_5_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_3_ID)
                    .ordinalPosition(COLUMN_1_3_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_3_NAME)
                    .internalName(COLUMN_1_3_INTERNAL_NAME)
                    .columnType(COLUMN_1_3_TYPE)
                    .dfid(COLUMN_1_3_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_3_NULL)
                    .autoGenerated(COLUMN_1_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_3_PRIMARY)
                    .enumValues(COLUMN_1_3_ENUM_VALUES)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .id(COLUMN_1_2_ID)
                    .ordinalPosition(COLUMN_1_2_ORDINALPOS)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name(COLUMN_1_2_NAME)
                    .internalName(COLUMN_1_2_INTERNAL_NAME)
                    .columnType(COLUMN_1_2_TYPE)
                    .dfid(COLUMN_1_2_DATE_FORMAT)
                    .isNullAllowed(COLUMN_1_2_NULL)
                    .autoGenerated(COLUMN_1_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_2_PRIMARY)
                    .enumValues(COLUMN_1_2_ENUM_VALUES)
                    .creator(USER_1)
                    .build());

    public final static View VIEW_3 = View.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .columns(VIEW_3_COLUMNS)
            .query(VIEW_3_QUERY)
            .creator(USER_1)
            .createdBy(USER_1_ID)
            .build();

    public final static ViewDto VIEW_3_DTO = ViewDto.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .query(VIEW_3_QUERY)
            .createdBy(USER_1_ID)
            .build();

    public final static Long VIEW_4_ID = 4L;
    public final static Boolean VIEW_4_INITIAL_VIEW = false;
    public final static String VIEW_4_NAME = "Mock View";
    public final static String VIEW_4_INTERNAL_NAME = "mock_view";
    public final static Long VIEW_4_DATABASE_ID = DATABASE_2_ID;
    public final static Long VIEW_4_TABLE_ID = TABLE_4_ID;
    public final static Boolean VIEW_4_PUBLIC = true;
    public final static String VIEW_4_QUERY = "SELECT `animal_name`, `hair`, `feathers`, `eggs`, `milk`, `airborne`, `aquatic`, `predator`, `toothed`, `backbone`, `breathes`, `venomous`, `fins`, `legs`, `tail`, `domestic`, `catsize`, `class_type` FROM `zoo` WHERE `class_type` = 1";

    public final static List<TableColumn> VIEW_4_COLUMNS = List.of(TableColumn.builder()
                    .ordinalPosition(0)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("animal_name")
                    .internalName("animal_name")
                    .columnType(TableColumnType.STRING)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(1)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("hair")
                    .internalName("hair")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(2)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("feathers")
                    .internalName("feathers")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(3)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("eggs")
                    .internalName("eggs")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(4)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("milk")
                    .internalName("milk")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(5)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("airborne")
                    .internalName("airborne")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(6)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("aquatic")
                    .internalName("aquatic")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(7)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("predator")
                    .internalName("predator")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(8)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("toothed")
                    .internalName("toothed")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(9)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("backbone")
                    .internalName("backbone")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(10)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("breathes")
                    .internalName("breathes")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(11)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("venomous")
                    .internalName("venomous")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(12)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("fins")
                    .internalName("fins")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(13)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("legs")
                    .internalName("legs")
                    .columnType(TableColumnType.NUMBER)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(14)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("tail")
                    .internalName("tail")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(15)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("domestic")
                    .internalName("domestic")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(16)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("catsize")
                    .internalName("catsize")
                    .columnType(TableColumnType.BOOLEAN)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build(),
            TableColumn.builder()
                    .ordinalPosition(17)
                    .cdbid(VIEW_4_DATABASE_ID)
                    .tid(VIEW_4_TABLE_ID)
                    .name("class_type")
                    .internalName("class_type")
                    .columnType(TableColumnType.NUMBER)
                    .dfid(null)
                    .isNullAllowed(null)
                    .autoGenerated(false)
                    .isPrimaryKey(false)
                    .enumValues(null)
                    .creator(USER_1)
                    .build());

    public final static View VIEW_4 = View.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vdbid(VIEW_4_DATABASE_ID)
            .isPublic(VIEW_4_PUBLIC)
            .query(VIEW_4_QUERY)
            .createdBy(USER_1_ID)
            .creator(USER_1)
            .columns(VIEW_4_COLUMNS)
            .build();

    public final static Long VIEW_5_ID = 5L;
    public final static Boolean VIEW_5_INITIAL_VIEW = false;
    public final static String VIEW_5_NAME = "Mock View";
    public final static String VIEW_5_INTERNAL_NAME = "mock_view";
    public final static Long VIEW_5_DATABASE_ID = DATABASE_2_ID;
    public final static Boolean VIEW_5_PUBLIC = true;
    public final static String VIEW_5_QUERY = "SELECT `location`, `lat`, `lng` FROM `weather_location` WHERE `location` = 'Albury'";

    public final static View VIEW_5 = View.builder()
            .id(VIEW_5_ID)
            .isInitialView(VIEW_5_INITIAL_VIEW)
            .name(VIEW_5_NAME)
            .internalName(VIEW_5_INTERNAL_NAME)
            .vdbid(VIEW_5_DATABASE_ID)
            .isPublic(VIEW_5_PUBLIC)
            .query(VIEW_5_QUERY)
            .creator(USER_1)
            .createdBy(USER_1_ID)
            .build();

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
            .resultNumber(QUERY_1_RESULT_NUMBER)
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
    public final static String LICENSE_1_URI = "https://opensource.org/licenses/MIT";

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
    public final static String CREATOR_1_FIRSTNAME = "Max";
    public final static String CREATOR_1_LASTNAME = "Mustermann";
    public final static Instant CREATOR_1_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant CREATOR_1_MODIFIED = Instant.ofEpochSecond(1541588352);

    public final static Long CREATOR_2_ID = 2L;
    public final static Long CREATOR_2_QUERY_ID = 1L;
    public final static String CREATOR_2_ORCID = "00000-00000-00000";
    public final static String CREATOR_2_AFFIL = "TU Wien";
    public final static String CREATOR_2_FIRSTNAME = "Martina";
    public final static String CREATOR_2_LASTNAME = "Mustermann";
    public final static Instant CREATOR_2_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant CREATOR_2_MODIFIED = Instant.ofEpochSecond(1541588352);

    public final static Long CREATOR_3_ID = 3L;
    public final static Long CREATOR_3_QUERY_ID = 1L;
    public final static String CREATOR_3_ORCID = "00000-00000-00000";
    public final static String CREATOR_3_AFFIL = "TU Graz";
    public final static String CREATOR_3_FIRSTNAME = "Max";
    public final static String CREATOR_3_LASTNAME = "Mustermann";
    public final static Instant CREATOR_3_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant CREATOR_3_MODIFIED = Instant.ofEpochSecond(1541588352);

    public final static Long CREATOR_4_ID = 4L;
    public final static Long CREATOR_4_QUERY_ID = 1L;
    public final static String CREATOR_4_ORCID = "00000-00000-00000";
    public final static String CREATOR_4_AFFIL = "TU Wien";
    public final static String CREATOR_4_FIRSTNAME = "Martina";
    public final static String CREATOR_4_LASTNAME = "Mustermann";
    public final static Instant CREATOR_4_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant CREATOR_4_MODIFIED = Instant.ofEpochSecond(1541588352);

    public final static Long IDENTIFIER_1_ID = 1L;
    public final static Long IDENTIFIER_1_QUERY_ID = QUERY_1_ID;
    public final static Long IDENTIFIER_1_DATABASE_ID = DATABASE_1_ID;
    public final static String IDENTIFIER_1_DESCRIPTION = "Selecting all from the weather Austrian table";
    public final static String IDENTIFIER_1_DESCRIPTION_MODIFY = "Selecting some from the weather Austrian table";
    public final static String IDENTIFIER_1_TITLE = "Austrian weather data";
    public final static String IDENTIFIER_1_TITLE_MODIFY = "Austrian weather some data";
    public final static String IDENTIFIER_1_DOI = null;
    public final static String IDENTIFIER_1_DOI_NOT_NULL = "10.1000/183";
    public final static Instant IDENTIFIER_1_CREATED = Instant.ofEpochSecond(1641588352) /* 2022-01-07 20:45:52 */;
    public final static Instant IDENTIFIER_1_MODIFIED = Instant.ofEpochSecond(1541588352) /* 2022-01-07 20:45:52 */;
    public final static Instant IDENTIFIER_1_EXECUTION = Instant.ofEpochSecond(1541588352) /* 2022-01-07 20:45:52 */;
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
    public final static User IDENTIFIER_1_CREATOR = USER_1;
    public final static VisibilityType IDENTIFIER_1_VISIBILITY = VisibilityType.EVERYONE;
    public final static VisibilityTypeDto IDENTIFIER_1_VISIBILITY_DTO = VisibilityTypeDto.EVERYONE;

    public final static Creator IDENTIFIER_1_CREATOR_1 = Creator.builder()
            .id(CREATOR_1_ID)
            .pid(IDENTIFIER_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .creator(IDENTIFIER_1_CREATOR)
            .build();

    public final static CreatorDto IDENTIFIER_1_CREATOR_1_DTO = CreatorDto.builder()
            .id(CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static Identifier IDENTIFIER_1 = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
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
            .type(IDENTIFIER_1_TYPE)
            .creator(USER_1)
            .creators(List.of(IDENTIFIER_1_CREATOR_1))
            .visibility(IDENTIFIER_1_VISIBILITY)
            .build();

    public final static Identifier IDENTIFIER_1_SIMPLE = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
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
            .type(IDENTIFIER_1_TYPE)
            .creator(null /* for jpa */)
            .creators(List.of() /* for jpa */)
            .visibility(IDENTIFIER_1_VISIBILITY)
            .build();

    public final static Identifier IDENTIFIER_1_WITH_DOI = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
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
            .type(IDENTIFIER_1_TYPE)
            .creator(USER_1)
            .creators(List.of(IDENTIFIER_1_CREATOR_1))
            .visibility(IDENTIFIER_1_VISIBILITY)
            .build();

    public final static IdentifierDto IDENTIFIER_1_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
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
            .creators(List.of(IDENTIFIER_1_CREATOR_1_DTO))
            .visibility(IDENTIFIER_1_VISIBILITY_DTO)
            .build();

    public final static IdentifierDto IDENTIFIER_1_WITH_DOI_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
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
            .creators(List.of(IDENTIFIER_1_CREATOR_1_DTO))
            .visibility(IDENTIFIER_1_VISIBILITY_DTO)
            .build();

    public final static Long IDENTIFIER_2_ID = 2L;
    public final static Long IDENTIFIER_2_QUERY_ID = QUERY_2_ID;
    public final static Long IDENTIFIER_2_DATABASE_ID = DATABASE_2_ID;
    public final static String IDENTIFIER_2_DESCRIPTION = "Selecting all from the weather Austria table";
    public final static String IDENTIFIER_2_TITLE = "Australian weather data";
    public final static String IDENTIFIER_2_DOI = null;
    public final static Instant IDENTIFIER_2_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant IDENTIFIER_2_MODIFIED = Instant.ofEpochSecond(1541588352);
    public final static Instant IDENTIFIER_2_EXECUTION = Instant.ofEpochSecond(1541588352);
    public final static Integer IDENTIFIER_2_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_2_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_2_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_2_QUERY_HASH = QUERY_2_QUERY_HASH;
    public final static String IDENTIFIER_2_RESULT_HASH = QUERY_2_RESULT_HASH;
    public final static String IDENTIFIER_2_QUERY = QUERY_2_STATEMENT;
    public final static String IDENTIFIER_2_NORMALIZED = QUERY_2_STATEMENT;
    public final static Long IDENTIFIER_2_RESULT_NUMBER = QUERY_2_RESULT_NUMBER;
    public final static String IDENTIFIER_2_PUBLISHER = "Australian Government";
    public final static IdentifierType IDENTIFIER_2_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_2_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static UUID IDENTIFIER_2_CREATED_BY = USER_2_ID;
    public final static User IDENTIFIER_2_CREATOR = USER_2;
    public final static VisibilityType IDENTIFIER_2_VISIBILITY = VisibilityType.SELF;
    public final static VisibilityTypeDto IDENTIFIER_2_VISIBILITY_DTO = VisibilityTypeDto.SELF;

    public final static Long IDENTIFIER_2_CREATOR_1_ID = 2L;

    public final static Creator IDENTIFIER_2_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_2_CREATOR_1_ID)
            .pid(IDENTIFIER_2_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .creator(IDENTIFIER_2_CREATOR)
            .build();

    public final static CreatorDto IDENTIFIER_2_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_2_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static Long IDENTIFIER_2_CREATOR_2_ID = 3L;

    public final static Creator IDENTIFIER_2_CREATOR_2 = Creator.builder()
            .id(IDENTIFIER_2_CREATOR_2_ID)
            .pid(IDENTIFIER_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .orcid(CREATOR_2_ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .creator(IDENTIFIER_2_CREATOR)
            .build();

    public final static CreatorDto IDENTIFIER_2_CREATOR_2_DTO = CreatorDto.builder()
            .id(IDENTIFIER_2_CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .orcid(CREATOR_2_ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static Identifier IDENTIFIER_2 = Identifier.builder()
            .id(IDENTIFIER_2_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
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
            .type(IDENTIFIER_2_TYPE)
            .creator(USER_2)
            .creators(List.of(IDENTIFIER_2_CREATOR_1, IDENTIFIER_2_CREATOR_2))
            .visibility(IDENTIFIER_2_VISIBILITY)
            .build();

    public final static Identifier IDENTIFIER_2_SIMPLE = Identifier.builder()
            .id(IDENTIFIER_2_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
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
            .type(IDENTIFIER_2_TYPE)
            .creator(null /* for jpa */)
            .creators(List.of() /* for jpa */)
            .visibility(IDENTIFIER_2_VISIBILITY)
            .build();

    public final static IdentifierDto IDENTIFIER_2_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_2_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
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
            .creator(USER_2_DTO)
            .creators(List.of(IDENTIFIER_2_CREATOR_1_DTO, IDENTIFIER_2_CREATOR_2_DTO))
            .visibility(IDENTIFIER_2_VISIBILITY_DTO)
            .build();

    public final static Creator CREATOR_1 = Creator.builder()
            .id(CREATOR_1_ID)
            .pid(IDENTIFIER_1_ID)
            .orcid(CREATOR_1_ORCID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .created(CREATOR_1_CREATED)
            .affiliation(CREATOR_1_AFFIL)
            .lastModified(CREATOR_1_MODIFIED)
            .build();

    public final static Creator CREATOR_2 = Creator.builder()
            .id(CREATOR_2_ID)
            .pid(IDENTIFIER_1_ID)
            .orcid(CREATOR_2_ORCID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .created(CREATOR_2_CREATED)
            .affiliation(CREATOR_2_AFFIL)
            .lastModified(CREATOR_2_MODIFIED)
            .build();

    public final static Creator CREATOR_3 = Creator.builder()
            .id(CREATOR_3_ID)
            .pid(IDENTIFIER_1_ID)
            .orcid(CREATOR_3_ORCID)
            .firstname(CREATOR_3_FIRSTNAME)
            .lastname(CREATOR_3_LASTNAME)
            .created(CREATOR_3_CREATED)
            .affiliation(CREATOR_3_AFFIL)
            .lastModified(CREATOR_3_MODIFIED)
            .build();

    public final static CreatorDto CREATOR_1_DTO = CreatorDto.builder()
            .id(CREATOR_1_ID)
            .affiliation(CREATOR_1_AFFIL)
            .orcid(CREATOR_1_ORCID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .build();

    public final static CreatorCreateDto CREATOR_1_CREATE_DTO = CreatorCreateDto.builder()
            .affiliation(CREATOR_1_AFFIL)
            .orcid(CREATOR_1_ORCID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .build();

    public final static CreatorDto CREATOR_2_DTO = CreatorDto.builder()
            .id(CREATOR_2_ID)
            .affiliation(CREATOR_2_AFFIL)
            .orcid(CREATOR_2_ORCID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .build();

    public final static CreatorCreateDto CREATOR_2_CREATE_DTO = CreatorCreateDto.builder()
            .affiliation(CREATOR_2_AFFIL)
            .orcid(CREATOR_2_ORCID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .build();

    public final static IdentifierDto IDENTIFIER_1_MODIFY_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .databaseId(DATABASE_1_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .description(IDENTIFIER_1_DESCRIPTION_MODIFY)
            .title(IDENTIFIER_1_TITLE_MODIFY)
            .doi(IDENTIFIER_1_DOI)
            .publisher(IDENTIFIER_1_PUBLISHER)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .type(IDENTIFIER_1_TYPE_DTO)
            .created(IDENTIFIER_1_CREATED)
            .lastModified(IDENTIFIER_1_MODIFIED)
            .creators(List.of(CREATOR_1_DTO))
            .visibility(IDENTIFIER_2_VISIBILITY_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_1_DTO_REQUEST = IdentifierCreateDto.builder()
            .dbid(IDENTIFIER_1_DATABASE_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_CREATE_DTO))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .visibility(IDENTIFIER_1_VISIBILITY_DTO)
            .build();

    public final static IdentifierUpdateDto IDENTIFIER_1_DTO_UPDATE_REQUEST = IdentifierUpdateDto.builder()
            .dbid(IDENTIFIER_1_DATABASE_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE_MODIFY)
            .doi(IDENTIFIER_1_DOI)
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_DTO))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .visibility(IDENTIFIER_1_VISIBILITY_DTO)
            .build();

    public final static Long RELATED_IDENTIFIER_2_ID = 1L;
    public final static Long RELATED_IDENTIFIER_2_IDENTIFIER_ID = 2L;
    public final static String RELATED_IDENTIFIER_2_VALUE = "10.5281/zenodo.6637333";
    public final static RelatedType RELATED_IDENTIFIER_2_TYPE = RelatedType.DOI;
    public final static RelatedTypeDto RELATED_IDENTIFIER_2_TYPE_DTO = RelatedTypeDto.DOI;
    public final static RelationType RELATED_IDENTIFIER_2_RELATION_TYPE = RelationType.CITES;
    public final static RelationTypeDto RELATED_IDENTIFIER_2_RELATION = RelationTypeDto.CITES;

    public final static RelatedIdentifier IDENTIFIER_1_RELATED_IDENTIFIER_1 = RelatedIdentifier.builder()
            .id(RELATED_IDENTIFIER_2_ID)
            .iid(RELATED_IDENTIFIER_2_IDENTIFIER_ID)
            .type(RELATED_IDENTIFIER_2_TYPE)
            .relation(RELATED_IDENTIFIER_2_RELATION_TYPE)
            .value(RELATED_IDENTIFIER_2_VALUE)
            .build();

    public final static RelatedIdentifierCreateDto IDENTIFIER_1_RELATED_IDENTIFIER_2_CREATE_DTO = RelatedIdentifierCreateDto.builder()
            .value(RELATED_IDENTIFIER_2_VALUE)
            .type(RELATED_IDENTIFIER_2_TYPE_DTO)
            .relation(RELATED_IDENTIFIER_2_RELATION)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_2_DTO_REQUEST = IdentifierCreateDto.builder()
            .qid(IDENTIFIER_2_QUERY_ID)
            .qid(IDENTIFIER_2_QUERY_ID)
            .dbid(IDENTIFIER_2_DATABASE_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
            .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_2_CREATE_DTO))
            .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_CREATE_DTO, CREATOR_2_CREATE_DTO))
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .visibility(IDENTIFIER_2_VISIBILITY_DTO)
            .build();

    public final static IdentifierUpdateDto IDENTIFIER_2_DTO_UPDATE_REQUEST = IdentifierUpdateDto.builder()
            .qid(IDENTIFIER_2_QUERY_ID)
            .qid(IDENTIFIER_2_QUERY_ID)
            .dbid(IDENTIFIER_2_DATABASE_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
            .doi(IDENTIFIER_2_DOI)
            .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_2_CREATE_DTO))
            .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_DTO, CREATOR_2_DTO))
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .visibility(IDENTIFIER_2_VISIBILITY_DTO)
            .build();

    public final static Long IDENTIFIER_3_ID = 3L;
    public final static Long IDENTIFIER_3_QUERY_ID = QUERY_3_ID;
    public final static Long IDENTIFIER_3_DATABASE_ID = DATABASE_3_ID;
    public final static String IDENTIFIER_3_DESCRIPTION = "Selecting all from the weather Norwegian table";
    public final static String IDENTIFIER_3_TITLE = "Norwegian weather data";
    public final static String IDENTIFIER_3_DOI = null;
    public final static Instant IDENTIFIER_3_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant IDENTIFIER_3_MODIFIED = Instant.ofEpochSecond(1541588352);
    public final static Instant IDENTIFIER_3_EXECUTION = Instant.ofEpochSecond(1541588352);
    public final static Integer IDENTIFIER_3_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_3_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_3_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_3_QUERY_HASH = QUERY_3_QUERY_HASH;
    public final static String IDENTIFIER_3_RESULT_HASH = QUERY_3_RESULT_HASH;
    public final static String IDENTIFIER_3_QUERY = QUERY_3_STATEMENT;
    public final static String IDENTIFIER_3_NORMALIZED = QUERY_3_STATEMENT;
    public final static Long IDENTIFIER_3_RESULT_NUMBER = QUERY_3_RESULT_NUMBER;
    public final static String IDENTIFIER_3_PUBLISHER = "Norwegian Government";
    public final static IdentifierType IDENTIFIER_3_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_3_TYPE_DTO = IdentifierTypeDto.SUBSET;
    public final static UUID IDENTIFIER_3_CREATOR_ID = USER_3_ID;
    public final static User IDENTIFIER_3_CREATOR = USER_3;
    public final static VisibilityType IDENTIFIER_3_VISIBILITY = VisibilityType.EVERYONE;
    public final static VisibilityTypeDto IDENTIFIER_3_VISIBILITY_DTO = VisibilityTypeDto.EVERYONE;

    private final static Long IDENTIFIER_3_CREATOR_1_ID = 4L;

    public final static Creator IDENTIFIER_3_CREATOR_1 = Creator.builder()
            .id(IDENTIFIER_3_CREATOR_1_ID)
            .pid(IDENTIFIER_3_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .creator(IDENTIFIER_3_CREATOR)
            .build();

    public final static CreatorDto IDENTIFIER_3_CREATOR_1_DTO = CreatorDto.builder()
            .id(IDENTIFIER_3_CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .orcid(CREATOR_1_ORCID)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    private final static Long IDENTIFIER_3_CREATOR_2_ID = 5L;

    public final static Creator IDENTIFIER_3_CREATOR_2 = Creator.builder()
            .id(IDENTIFIER_3_CREATOR_2_ID)
            .pid(IDENTIFIER_3_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .orcid(CREATOR_2_ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .creator(IDENTIFIER_3_CREATOR)
            .build();

    public final static CreatorDto IDENTIFIER_3_CREATOR_2_DTO = CreatorDto.builder()
            .id(IDENTIFIER_3_CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .orcid(CREATOR_2_ORCID)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    private final static Long IDENTIFIER_3_CREATOR_3_ID = 6L;

    public final static Creator IDENTIFIER_3_CREATOR_3 = Creator.builder()
            .id(IDENTIFIER_3_CREATOR_3_ID)
            .pid(IDENTIFIER_3_ID)
            .firstname(CREATOR_3_FIRSTNAME)
            .lastname(CREATOR_3_LASTNAME)
            .orcid(CREATOR_3_ORCID)
            .affiliation(CREATOR_3_AFFIL)
            .creator(IDENTIFIER_3_CREATOR)
            .build();

    public final static CreatorDto IDENTIFIER_3_CREATOR_3_DTO = CreatorDto.builder()
            .id(IDENTIFIER_3_CREATOR_3_ID)
            .firstname(CREATOR_3_FIRSTNAME)
            .lastname(CREATOR_3_LASTNAME)
            .orcid(CREATOR_3_ORCID)
            .affiliation(CREATOR_3_AFFIL)
            .build();

    public final static Identifier IDENTIFIER_3 = Identifier.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .queryId(IDENTIFIER_3_QUERY_ID)
            .description(IDENTIFIER_3_DESCRIPTION)
            .title(IDENTIFIER_3_TITLE)
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
            .type(IDENTIFIER_3_TYPE)
            .creator(IDENTIFIER_3_CREATOR)
            .creators(List.of(IDENTIFIER_3_CREATOR_1, IDENTIFIER_3_CREATOR_2, IDENTIFIER_3_CREATOR_3))
            .visibility(IDENTIFIER_3_VISIBILITY)
            .build();

    public final static Identifier IDENTIFIER_3_SIMPLE = Identifier.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .queryId(IDENTIFIER_3_QUERY_ID)
            .description(IDENTIFIER_3_DESCRIPTION)
            .title(IDENTIFIER_3_TITLE)
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
            .type(IDENTIFIER_3_TYPE)
            .creator(null /* for jpa */)
            .creators(List.of() /* for jpa */)
            .visibility(IDENTIFIER_3_VISIBILITY)
            .build();

    public final static IdentifierDto IDENTIFIER_3_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_3_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .queryId(IDENTIFIER_3_QUERY_ID)
            .description(IDENTIFIER_3_DESCRIPTION)
            .title(IDENTIFIER_3_TITLE)
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
            .creator(USER_3_DTO)
            .creators(List.of(IDENTIFIER_3_CREATOR_1_DTO, IDENTIFIER_3_CREATOR_2_DTO, IDENTIFIER_3_CREATOR_3_DTO))
            .visibility(IDENTIFIER_3_VISIBILITY_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_3_DTO_REQUEST = IdentifierCreateDto.builder()
            .dbid(IDENTIFIER_3_DATABASE_ID)
            .qid(IDENTIFIER_3_QUERY_ID)
            .description(IDENTIFIER_3_DESCRIPTION)
            .title(IDENTIFIER_3_TITLE)
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_3_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_CREATE_DTO))
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .visibility(IDENTIFIER_3_VISIBILITY_DTO)
            .build();

    public final static IdentifierUpdateDto IDENTIFIER_3_DTO_UPDATE_REQUEST = IdentifierUpdateDto.builder()
            .dbid(IDENTIFIER_3_DATABASE_ID)
            .qid(IDENTIFIER_3_QUERY_ID)
            .description(IDENTIFIER_3_DESCRIPTION)
            .title(IDENTIFIER_3_TITLE)
            .doi(IDENTIFIER_3_DOI)
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_3_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_3_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_DTO))
            .publisher(IDENTIFIER_3_PUBLISHER)
            .type(IDENTIFIER_3_TYPE_DTO)
            .visibility(IDENTIFIER_3_VISIBILITY_DTO)
            .build();

    public final static Long IDENTIFIER_4_ID = 4L;
    public final static Long IDENTIFIER_4_DATABASE_ID = DATABASE_4_ID;
    public final static String IDENTIFIER_4_DESCRIPTION = "Selecting all from the weather Sweden table";
    public final static String IDENTIFIER_4_TITLE = "Sweden weather data";
    public final static String IDENTIFIER_4_DOI = null;
    public final static Instant IDENTIFIER_4_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant IDENTIFIER_4_MODIFIED = Instant.ofEpochSecond(1541588352);
    public final static Instant IDENTIFIER_4_EXECUTION = Instant.ofEpochSecond(1541588352);
    public final static Integer IDENTIFIER_4_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_4_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_4_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_4_QUERY_HASH = "abc";
    public final static String IDENTIFIER_4_RESULT_HASH = "def";
    public final static String IDENTIFIER_4_QUERY = "SELECT `id` FROM `foobar`";
    public final static String IDENTIFIER_4_NORMALIZED = "SELECT `id` FROM `foobar`";
    public final static Long IDENTIFIER_4_RESULT_NUMBER = 2L;
    public final static String IDENTIFIER_4_PUBLISHER = "Swedish Government";
    public final static IdentifierType IDENTIFIER_4_TYPE = IdentifierType.DATABASE;
    public final static UUID IDENTIFIER_4_CREATOR_ID = USER_4_ID;
    public final static User IDENTIFIER_4_CREATOR = USER_4;
    public final static VisibilityType IDENTIFIER_4_VISIBILITY = VisibilityType.EVERYONE;
    public final static VisibilityTypeDto IDENTIFIER_4_VISIBILITY_DTO = VisibilityTypeDto.EVERYONE;

    public final static Identifier IDENTIFIER_4 = Identifier.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .description(IDENTIFIER_4_DESCRIPTION)
            .title(IDENTIFIER_4_TITLE)
            .doi(IDENTIFIER_4_DOI)
            .created(IDENTIFIER_4_CREATED)
            .lastModified(IDENTIFIER_4_MODIFIED)
            .execution(IDENTIFIER_4_EXECUTION)
            .publicationDay(IDENTIFIER_4_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_4_QUERY_HASH)
            .resultHash(IDENTIFIER_4_RESULT_HASH)
            .query(IDENTIFIER_4_QUERY)
            .queryNormalized(IDENTIFIER_4_NORMALIZED)
            .resultNumber(IDENTIFIER_4_RESULT_NUMBER)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE)
            .creator(USER_3)
            .creators(List.of())
            .visibility(IDENTIFIER_4_VISIBILITY)
            .build();

    public final static Identifier IDENTIFIER_4_SIMPLE = Identifier.builder()
            .id(IDENTIFIER_4_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .description(IDENTIFIER_4_DESCRIPTION)
            .title(IDENTIFIER_4_TITLE)
            .doi(IDENTIFIER_4_DOI)
            .created(IDENTIFIER_4_CREATED)
            .lastModified(IDENTIFIER_4_MODIFIED)
            .execution(IDENTIFIER_4_EXECUTION)
            .publicationDay(IDENTIFIER_4_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_4_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_4_PUBLICATION_YEAR)
            .queryHash(IDENTIFIER_4_QUERY_HASH)
            .resultHash(IDENTIFIER_4_RESULT_HASH)
            .query(IDENTIFIER_4_QUERY)
            .queryNormalized(IDENTIFIER_4_NORMALIZED)
            .resultNumber(IDENTIFIER_4_RESULT_NUMBER)
            .publisher(IDENTIFIER_4_PUBLISHER)
            .type(IDENTIFIER_4_TYPE)
            .creator(null /* for jpa */)
            .creators(List.of() /* for jpa */)
            .visibility(IDENTIFIER_4_VISIBILITY)
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
    public final static Instant BANNER_MESSAGE_1_START = Instant.ofEpochSecond(1684577786);
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
    public final static Instant BANNER_MESSAGE_2_START = Instant.ofEpochSecond(1671836400);
    public final static Instant BANNER_MESSAGE_2_END = Instant.ofEpochSecond(1672009200);

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

}
