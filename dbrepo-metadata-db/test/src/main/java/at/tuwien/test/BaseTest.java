package at.tuwien.test;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.container.image.ImageEnvItemDto;
import at.tuwien.api.container.image.ImageEnvItemTypeDto;
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
import at.tuwien.api.identifier.*;
import at.tuwien.api.user.*;
import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.entities.database.*;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.identifier.*;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.querystore.Query;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.github.dockerjava.api.model.HealthCheck;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.*;

/**
 * Database 1
 * <ul>
 * <li>Table 1</li>
 * <li>Table 2</li>
 * <li>Table 3</li>
 * <li>Table 7</li>
 * <li>Query 1</li>
 * <li>Query 2</li>
 * <li>Query 3</li>
 * </ul>
 * <p>
 * Database 2
 * <ul>
 * <li>Table 4</li>
 * <li>Table 5</li>
 * <li>Table 6</li>
 * <li>View 4</li>
 * </ul>
 * <p>
 * Database 3
 * <ul>
 * <li>Table 8</li>
 * <li>Query 4</li>
 * </ul>
 */
public abstract class BaseTest {

    public final static String JWT_1 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtd2Vpc2UiLCJybmQiOjk2NjIyNzAwMCwiZXhwIjoxNjczODg2MDk5LCJpYXQiOjE2NzM3OTk2OTl9.y1jqokCfZE7c_Ztt_nLQlf73jCYXPH5TZpCvo3RwS0C5azyrqLh03bphl6R8A24g6Kv_3qjzvnubNIwmO7y7pA";

    public final static GrantedAuthorityDto AUTHORITY_RESEARCHER_DTO = GrantedAuthorityDto.builder()
            .authority("ROLE_RESEARCHER")
            .build();

    public final static GrantedAuthorityDto AUTHORITY_DEVELOPER_DTO = GrantedAuthorityDto.builder()
            .authority("ROLE_DEVELOPER")
            .build();

    public final static GrantedAuthorityDto AUTHORITY_DATA_STEWARD_DTO = GrantedAuthorityDto.builder()
            .authority("ROLE_DATA_STEWARD")
            .build();

    public final static UserThemeSetDto USER_THEME_DARK_DTO = UserThemeSetDto.builder()
            .themeDark(true)
            .build();

    public final static UserThemeSetDto USER_THEME_LIGHT_DTO = UserThemeSetDto.builder()
            .themeDark(false)
            .build();

    public final static Long USER_1_ID = 1L;
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
    public final static Boolean USER_1_THEME_DARK = false;
    public final static Instant USER_1_CREATED = Instant.ofEpochSecond(1677399441) /* 2023-02-26 08:17:21 (UTC) */;
    public final static Instant USER_1_LAST_MODIFIED = USER_1_CREATED;

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD_ENCODED)
            .databasePassword(USER_1_DATABASE_PASSWORD)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .affiliation(USER_1_AFFILIATION)
            .orcid(USER_1_ORCID)
            .titlesBefore(USER_1_TITLES_BEFORE)
            .titlesAfter(USER_1_TITLES_AFTER)
            .emailVerified(USER_1_VERIFIED)
            .themeDark(USER_1_THEME_DARK)
            .created(USER_1_CREATED)
            .roles(List.of(RoleType.ROLE_RESEARCHER))
            .lastModified(USER_1_LAST_MODIFIED)
            .build();

    public final static UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD_ENCODED)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .affiliation(USER_1_AFFILIATION)
            .orcid(USER_1_ORCID)
            .titlesBefore(USER_1_TITLES_BEFORE)
            .titlesAfter(USER_1_TITLES_AFTER)
            .emailVerified(USER_1_VERIFIED)
            .themeDark(USER_1_THEME_DARK)
            .authorities(List.of(AUTHORITY_RESEARCHER_DTO))
            .roles(List.of("ROLE_RESEARCHER"))
            .build();

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
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

    public final static Long USER_2_ID = 2L;
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
    public final static Boolean USER_2_THEME_DARK = false;
    public final static Instant USER_2_CREATED = Instant.ofEpochSecond(1677399528) /* 2023-02-26 08:18:48 (UTC) */;
    public final static Instant USER_2_LAST_MODIFIED = USER_1_CREATED;

    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .databasePassword(USER_2_DATABASE_PASSWORD)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .affiliation(USER_2_AFFILIATION)
            .orcid(USER_2_ORCID)
            .emailVerified(USER_2_VERIFIED)
            .themeDark(USER_2_THEME_DARK)
            .created(USER_2_CREATED)
            .roles(List.of(RoleType.ROLE_DEVELOPER))
            .lastModified(USER_2_LAST_MODIFIED)
            .build();

    public final static UserDto USER_2_DTO = UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .firstname(USER_2_FIRSTNAME)
            .lastname(USER_2_LASTNAME)
            .affiliation(USER_2_AFFILIATION)
            .orcid(USER_2_ORCID)
            .emailVerified(USER_2_VERIFIED)
            .themeDark(USER_2_THEME_DARK)
            .authorities(List.of(AUTHORITY_DEVELOPER_DTO))
            .roles(List.of("ROLE_DEVELOPER"))
            .build();

    public final static SignupRequestDto USER_2_SIGNUP_REQUEST_DTO = SignupRequestDto.builder()
            .username(USER_2_USERNAME)
            .password(USER_2_PASSWORD)
            .email(USER_2_EMAIL)
            .build();

    public final static UserDetails USER_2_DETAILS = UserDetailsDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")))
            .build();

    public final static at.tuwien.api.amqp.UserDetailsDto USER_2_DETAILS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_2_USERNAME)
            .tags(new String[]{})
            .build();

    public final static Principal USER_2_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_2_DETAILS,
            USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());

    public final static Long USER_3_ID = 3L;
    public final static String USER_3_USERNAME = "junit3";
    public final static String USER_3_FIRSTNAME = "System";
    public final static String USER_3_LASTNAME = "System";
    public final static String USER_3_AFFILIATION = "TU Wien";
    public final static String USER_3_ORCID = null;
    public final static String USER_3_EMAIL = "system@example.com";
    public final static String USER_3_PASSWORD = "password";
    public final static String USER_3_DATABASE_PASSWORD = "*D65FCA043964B63E849DD6334699ECB065905DA4" /* junit3 */;
    public final static Boolean USER_3_VERIFIED = true;
    public final static Boolean USER_3_THEME_DARK = false;
    public final static Instant USER_3_CREATED = Instant.ofEpochSecond(1677399559) /* 2023-02-26 08:19:19 (UTC) */;

    public final static User USER_3 = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .emailVerified(true)
            .themeDark(false)
            .password(USER_3_PASSWORD)
            .databasePassword(USER_3_DATABASE_PASSWORD)
            .roles(Collections.singletonList(RoleType.ROLE_RESEARCHER))
            .created(USER_3_CREATED)
            .lastModified(USER_3_CREATED)
            .build();

    public final static UserDto USER_3_DTO = UserDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .firstname(USER_3_FIRSTNAME)
            .lastname(USER_3_LASTNAME)
            .affiliation(USER_3_AFFILIATION)
            .orcid(USER_3_ORCID)
            .emailVerified(USER_3_VERIFIED)
            .themeDark(USER_3_THEME_DARK)
            .authorities(List.of(AUTHORITY_RESEARCHER_DTO))
            .roles(List.of("ROLE_RESEARCHER"))
            .build();

    public final static UserDetails USER_3_DETAILS = UserDetailsDto.builder()
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")))
            .build();

    public final static Principal USER_3_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_3_DETAILS,
            USER_3_PASSWORD, USER_3_DETAILS.getAuthorities());

    public final static at.tuwien.api.amqp.UserDetailsDto USER_3_DETAILS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_3_USERNAME)
            .tags(new String[]{})
            .build();

    public final static Long USER_4_ID = 4L;
    public final static String USER_4_USERNAME = "junit4";
    public final static String USER_4_FIRSTNAME = "JUnit";
    public final static String USER_4_LASTNAME = "4";
    public final static String USER_4_AFFILIATION = "TU Wien";
    public final static String USER_4_ORCID = null;
    public final static String USER_4_PASSWORD = "junit4";
    public final static String USER_4_DATABASE_PASSWORD = "*C20EF5C6875857DEFA9BE6E9B62DD76AAAE51882" /* junit4 */;
    public final static String USER_4_EMAIL = "junit4@ossdip.at";
    public final static Boolean USER_4_VERIFIED = true;
    public final static Boolean USER_4_THEME_DARK = false;
    public final static Instant USER_4_CREATED = Instant.ofEpochSecond(1677399592) /* 2023-02-26 08:19:52 (UTC) */;

    public final static User USER_4 = User.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .emailVerified(USER_4_VERIFIED)
            .themeDark(USER_4_THEME_DARK)
            .password(USER_4_PASSWORD)
            .databasePassword(USER_4_DATABASE_PASSWORD)
            .created(USER_4_CREATED)
            .build();

    public final static UserDto USER_4_DTO = UserDto.builder()
            .id(USER_4_ID)
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .password(USER_4_PASSWORD)
            .firstname(USER_4_FIRSTNAME)
            .lastname(USER_4_LASTNAME)
            .affiliation(USER_4_AFFILIATION)
            .orcid(USER_4_ORCID)
            .emailVerified(USER_4_VERIFIED)
            .themeDark(USER_4_THEME_DARK)
            .authorities(List.of(AUTHORITY_RESEARCHER_DTO))
            .roles(List.of("ROLE_RESEARCHER"))
            .build();

    public final static UserDetails USER_4_DETAILS = UserDetailsDto.builder()
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .password(USER_4_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static Principal USER_4_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_4_DETAILS,
            USER_4_PASSWORD, USER_4_DETAILS.getAuthorities());

    public final static Long TIME_SECRET_1_ID = 1L;
    public final static Boolean TIME_SECRET_1_PROCESSED = false;
    public final static String TIME_SECRET_1_TOKEN = "mysecrettokenrandomlygenerated";
    public final static Instant TIME_SECRET_1_VALID_TO = Instant.now()
            .plus(1, ChronoUnit.DAYS);

    public final static TimeSecret TIME_SECRET_1 = TimeSecret.builder()
            .id(TIME_SECRET_1_ID)
            .uid(USER_1_ID)
            .user(USER_1)
            .token(TIME_SECRET_1_TOKEN)
            .processed(TIME_SECRET_1_PROCESSED)
            .validTo(TIME_SECRET_1_VALID_TO)
            .build();

    public final static Long TIME_SECRET_2_ID = 2L;
    public final static Boolean TIME_SECRET_2_PROCESSED = true;
    public final static String TIME_SECRET_2_TOKEN = "blahblahblah";
    public final static Instant TIME_SECRET_2_VALID_TO = Instant.now()
            .plus(1, ChronoUnit.DAYS);

    public final static TimeSecret TIME_SECRET_2 = TimeSecret.builder()
            .id(TIME_SECRET_2_ID)
            .uid(USER_2_ID)
            .user(USER_2)
            .token(TIME_SECRET_2_TOKEN)
            .processed(TIME_SECRET_2_PROCESSED)
            .validTo(TIME_SECRET_2_VALID_TO)
            .build();

    public final static Long TIME_SECRET_3_ID = 3L;
    public final static Boolean TIME_SECRET_3_PROCESSED = false;
    public final static String TIME_SECRET_3_TOKEN = "blahblahblah";
    public final static Instant TIME_SECRET_3_VALID_TO = Instant.now()
            .plus(1, ChronoUnit.DAYS);

    public final static Long TOKEN_1_ID = 1L;
    public final static String TOKEN_1_TOKEN = "Ul0ioy8oUl0ioy8o";
    public final static String TOKEN_1_TOKEN_HASH = "131290c0f8bbb4ab9348c3d95ae3b595b625bd130f2ee6a48803a4120ce9c147";
    public final static String TOKEN_1_AUTHORIZATION = "Bearer " + TOKEN_1_TOKEN;
    public final static Instant TOKEN_1_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_1 = Token.builder()
            .id(TOKEN_1_ID)
            .token(TOKEN_1_TOKEN)
            .tokenHash(TOKEN_1_TOKEN_HASH)
            .creator(USER_1_ID)
            .expires(TOKEN_1_EXPIRES)
            .build();

    public final static Long TOKEN_2_ID = 2L;
    public final static String TOKEN_2_TOKEN = "Ul0ioy8oUl0ioy8o";
    public final static String TOKEN_2_TOKEN_HASH = "131290c0f8bbb4ab9348c3d95ae3b595b625bd130f2ee6a48803a4120ce9c147";
    public final static String TOKEN_2_AUTHORIZATION = "Bearer " + TOKEN_2_TOKEN;
    public final static Instant TOKEN_2_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_2 = Token.builder()
            .id(TOKEN_2_ID)
            .token(TOKEN_2_TOKEN)
            .tokenHash(TOKEN_2_TOKEN_HASH)
            .creator(USER_2_ID)
            .expires(TOKEN_2_EXPIRES)
            .build();

    public final static Token TOKEN_2_EXPIRED = Token.builder()
            .id(TOKEN_2_ID)
            .token(TOKEN_2_TOKEN)
            .expires(Instant.now().minus(100000000, MILLIS))
            .build();

    public final static Long TOKEN_3_ID = 3L;
    public final static String TOKEN_3_TOKEN = "Ul0ioy8oUl0ioy8o";
    public final static String TOKEN_3_TOKEN_HASH = "131290c0f8bbb4ab9348c3d95ae3b595b625bd130f2ee6a48803a4120ce9c147";
    public final static String TOKEN_3_AUTHORIZATION = "Bearer " + TOKEN_3_TOKEN;
    public final static Instant TOKEN_3_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_3 = Token.builder()
            .id(TOKEN_3_ID)
            .token(TOKEN_3_TOKEN)
            .tokenHash(TOKEN_3_TOKEN_HASH)
            .creator(USER_3_ID)
            .expires(TOKEN_3_EXPIRES)
            .build();

    public final static Long IMAGE_1_ID = 1L;
    public final static String IMAGE_1_REPOSITORY = "mariadb";
    public final static String IMAGE_1_TAG = "10.5";
    public final static String IMAGE_1_HASH = "d6a5e003eae42397f7ee4589e9f21e231d3721ac131970d2286bd616e7f55bb4";
    public final static String IMAGE_1_DIALECT = "org.hibernate.dialect.MariaDBDialect";
    public final static String IMAGE_1_DRIVER = "org.mariadb.jdbc.Driver";
    public final static String IMAGE_1_JDBC = "mariadb";
    public final static Integer IMAGE_1_PORT = 3306;
    public final static Long IMAGE_1_SIZE = 12000L;
    public final static Instant IMAGE_1_BUILT = Instant.now().minus(40, HOURS);

    public final static List<ContainerImageEnvironmentItem> IMAGE_1_ENV = List.of(
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("UZERNAME")
                    .value("root")
                    .type(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME)
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("MARIADB_ROOT_PASSWORD")
                    .value("mariadb")
                    .type(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD)
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("MARIADB_USER")
                    .value("mariadb")
                    .type(ContainerImageEnvironmentItemType.USERNAME)
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("MARIADB_PASSWORD")
                    .value("mariadb")
                    .type(ContainerImageEnvironmentItemType.PASSWORD)
                    .build());

    public final static List<ImageEnvItemDto> IMAGE_1_ENV_DTO = List.of(ImageEnvItemDto.builder()
                    .iid(IMAGE_1_ID)
                    .key("MARIADB_USER")
                    .value("mariadb")
                    .type(ImageEnvItemTypeDto.USERNAME)
                    .build(),
            ImageEnvItemDto.builder()
                    .iid(IMAGE_1_ID)
                    .key("MARIADB_PASSWORD")
                    .value("mariadb")
                    .type(ImageEnvItemTypeDto.PASSWORD)
                    .build());

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

    public final static ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .repository(IMAGE_1_REPOSITORY)
            .tag(IMAGE_1_TAG)
            .hash(IMAGE_1_HASH)
            .compiled(IMAGE_1_BUILT)
            .dialect(IMAGE_1_DIALECT)
            .jdbcMethod(IMAGE_1_JDBC)
            .driverClass(IMAGE_1_DRIVER)
            .size(IMAGE_1_SIZE)
            .environment(IMAGE_1_ENV)
            .defaultPort(IMAGE_1_PORT)
            .dateFormats(List.of(IMAGE_DATE_1, IMAGE_DATE_2, IMAGE_DATE_3))
            .build();

    public final static Long IMAGE_2_ID = 2L;
    public final static String IMAGE_2_REPOSITORY = "mysql";
    public final static String IMAGE_2_TAG = "8.0";
    public final static String IMAGE_2_HASH = "83b40f2726e5";
    public final static Integer IMAGE_2_PORT = 3306;
    public final static String IMAGE_2_DIALECT = "org.hibernate.dialect.MySQLDialect";
    public final static String IMAGE_2_DRIVER = "com.mysql.jdbc.Driver";
    public final static String IMAGE_2_JDBC = "mysql";
    public final static Long IMAGE_2_SIZE = 12000L;
    public final static Instant IMAGE_2_BUILT = Instant.now().minus(38, HOURS);

    public final static List<ImageEnvItemDto> IMAGE_2_ENV_DTO = List.of(ImageEnvItemDto.builder()
                    .iid(IMAGE_2_ID)
                    .key("MYSQL_USER")
                    .value("mysql")
                    .type(ImageEnvItemTypeDto.USERNAME)
                    .build(),
            ImageEnvItemDto.builder()
                    .iid(IMAGE_2_ID)
                    .key("MYSQL_PASSWORD")
                    .value("mysql")
                    .type(ImageEnvItemTypeDto.PASSWORD)
                    .build());

    public final static Long IMAGE_BROKER_ID = 2L;
    public final static String IMAGE_BROKER_REPOSITORY = "rabbitmq";
    public final static String IMAGE_BROKER_TAG = "3-management-alpine";
    public final static String IMAGE_BROKER_HASH = "d6a5e003eae42397f7ee4589e9f21e231d3721ac131970d2286bd616e7f55bb4\n";
    public final static String IMAGE_BROKER_DIALECT = "org.hibernate.dialect.MariaDBDialect";
    public final static String IMAGE_BROKER_DRIVER = "org.mariadb.jdbc.Driver";
    public final static String IMAGE_BROKER_JDBC = "mariadb";
    public final static Integer IMAGE_BROKER_PORT = 15672;
    public final static Long IMAGE_BROKER_SIZE = 12000L;
    public final static Instant IMAGE_BROKER_BUILT = Instant.now().minus(40, HOURS);

    public final static ContainerImage IMAGE_BROKER = ContainerImage.builder()
            .id(IMAGE_BROKER_ID)
            .repository(IMAGE_BROKER_REPOSITORY)
            .tag(IMAGE_BROKER_TAG)
            .hash(IMAGE_BROKER_HASH)
            .compiled(IMAGE_BROKER_BUILT)
            .dialect(IMAGE_BROKER_DIALECT)
            .jdbcMethod(IMAGE_BROKER_JDBC)
            .driverClass(IMAGE_BROKER_DRIVER)
            .size(IMAGE_BROKER_SIZE)
            .defaultPort(IMAGE_BROKER_PORT)
            .build();

    public final static Long IMAGE_ELASTIC_ID = 3L;
    public final static String IMAGE_ELASTIC_REPOSITORY = "elasticsearch";
    public final static String IMAGE_ELASTIC_TAG = "7.13.4";
    public final static String[] IMAGE_ELASTIC_ENV = new String[]{"discovery.type=single-node", "ES_JAVA_OPTS=-Xms512m -Xmx512m", "logger.level=WARN"};
    public final static String IMAGE_ELASTIC_CMD = "elasticsearch";

    public final static ContainerImage IMAGE_ELASTIC = ContainerImage.builder()
            .id(IMAGE_ELASTIC_ID)
            .repository(IMAGE_ELASTIC_REPOSITORY)
            .tag(IMAGE_ELASTIC_TAG)
            .build();

    public final static Long CONTAINER_1_ID = 1L;
    public final static String CONTAINER_1_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_1_IMAGE = IMAGE_1;
    public final static String CONTAINER_1_NAME = "u01";
    public final static String CONTAINER_1_INTERNALNAME = "dbrepo-userdb-u01";
    public final static String CONTAINER_1_IP = "172.28.0.5";
    public final static Instant CONTAINER_1_CREATED = Instant.ofEpochSecond(1677399629) /* 2023-02-26 08:20:29 (UTC) */;
    public final static HealthCheck CONTAINER_1_HEALTHCHECK = new HealthCheck()
            .withTest(List.of("CMD", "mysqladmin", "ping", "--host=127.0.0.1", "--password=mariadb"));
    public final static String[] CONTAINER_1_ENV = new String[]{"MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=weather"};

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_1_IMAGE)
            .hash(CONTAINER_1_HASH)
            .created(CONTAINER_1_CREATED)
            .ipAddress(CONTAINER_1_IP)
            .creator(USER_1)
            .owner(USER_1)
            .build();

    public final static Long CONTAINER_2_ID = 2L;
    public final static String CONTAINER_2_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_2_IMAGE = IMAGE_1;
    public final static String CONTAINER_2_NAME = "u02";
    public final static String CONTAINER_2_INTERNALNAME = "dbrepo-userdb-u02";
    public final static String CONTAINER_2_IP = "172.28.0.6";
    public final static Instant CONTAINER_2_CREATED = Instant.ofEpochSecond(1677399655) /* 2023-02-26 08:20:55 (UTC) */;
    public final static HealthCheck CONTAINER_2_HEALTHCHECK = new HealthCheck()
            .withTest(List.of("CMD", "mysqladmin", "ping", "--host=127.0.0.1", "--password=mariadb"));
    public final static String[] CONTAINER_2_ENV = new String[]{"MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=zoo"};

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_2_IMAGE)
            .hash(CONTAINER_2_HASH)
            .created(CONTAINER_2_CREATED)
            .ipAddress(CONTAINER_2_IP)
            .creator(USER_2)
            .owner(USER_2)
            .build();

    public final static Long CONTAINER_3_ID = 3L;
    public final static String CONTAINER_3_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_3_IMAGE = IMAGE_1;
    public final static String CONTAINER_3_NAME = "u03";
    public final static String CONTAINER_3_INTERNALNAME = "dbrepo-userdb-u03";
    public final static String CONTAINER_3_IP = "172.28.0.7";
    public final static Instant CONTAINER_3_CREATED = Instant.ofEpochSecond(1677399672) /* 2023-02-26 08:21:12 (UTC) */;
    public final static HealthCheck CONTAINER_3_HEALTHCHECK = new HealthCheck()
            .withTest(List.of("CMD", "mysqladmin", "ping", "--host=127.0.0.1", "--password=mariadb"));
    public final static String[] CONTAINER_3_ENV = new String[]{"MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=musicology"};

    public final static Container CONTAINER_3 = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .internalName(CONTAINER_3_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_3_IMAGE)
            .hash(CONTAINER_3_HASH)
            .created(CONTAINER_3_CREATED)
            .ipAddress(CONTAINER_3_IP)
            .creator(USER_3)
            .owner(USER_3)
            .build();

    public final static Long CONTAINER_4_ID = 4L;
    public final static String CONTAINER_4_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_4_IMAGE = IMAGE_1;
    public final static String CONTAINER_4_NAME = "u04";
    public final static String CONTAINER_4_INTERNALNAME = "dbrepo-userdb-u04";
    public final static String CONTAINER_4_IP = "172.28.0.8";
    public final static Instant CONTAINER_4_CREATED = Instant.ofEpochSecond(1677399688) /* 2023-02-26 08:21:28 (UTC) */;
    public final static HealthCheck CONTAINER_4_HEALTHCHECK = new HealthCheck()
            .withTest(List.of("CMD", "mysqladmin", "ping", "--host=127.0.0.1", "--password=mariadb"));
    public final static String[] CONTAINER_4_ENV = new String[]{"MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=sensor"};

    public final static Container CONTAINER_4 = Container.builder()
            .id(CONTAINER_4_ID)
            .name(CONTAINER_4_NAME)
            .internalName(CONTAINER_4_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_4_IMAGE)
            .hash(CONTAINER_4_HASH)
            .created(CONTAINER_4_CREATED)
            .ipAddress(CONTAINER_4_IP)
            .creator(USER_4)
            .owner(USER_4)
            .build();

    public final static Long CONTAINER_BROKER_ID = 5L;
    public final static String CONTAINER_BROKER_NAME = "dbrepo-broker-service";
    public final static String CONTAINER_BROKER_INTERNAL_NAME = "dbrepo-broker-service";
    public final static String CONTAINER_BROKER_IP = "172.29.0.2";
    public final static String CONTAINER_BROKER_HASH = "deadbeef";
    public final static Instant CONTAINER_BROKER_CREATED = Instant.ofEpochSecond(1677399705) /* 2023-02-26 08:21:45 (UTC) */;
    public final static HealthCheck CONTAINER_BROKER_HEALTHCHECK = new HealthCheck()
            .withTest(List.of("CMD", "rabbitmq-diagnostics", "-q", "ping"));
    public final static String[] CONTAINER_BROKER_ENV = new String[]{};

    public final static Container CONTAINER_BROKER = Container.builder()
            .id(CONTAINER_BROKER_ID)
            .name(CONTAINER_BROKER_NAME)
            .internalName(CONTAINER_BROKER_INTERNAL_NAME)
            .imageId(IMAGE_BROKER_ID)
            .image(IMAGE_BROKER)
            .ipAddress(CONTAINER_BROKER_IP)
            .hash(CONTAINER_BROKER_HASH)
            .created(CONTAINER_BROKER_CREATED)
            .creator(USER_1)
            .build();

    public final static Long CONTAINER_ELASTIC_ID = 6L;
    public final static String CONTAINER_ELASTIC_NAME = "dbrepo-search-mock-service";
    public final static String CONTAINER_ELASTIC_INTERNAL_NAME = "dbrepo-search-mock-service";
    public final static String CONTAINER_ELASTIC_IP = "172.29.0.3";
    public final static String CONTAINER_ELASTIC_HASH = "deadbeef";
    public final static Instant CONTAINER_ELASTIC_CREATED = Instant.ofEpochSecond(1677399721) /* 2023-02-26 08:22:01 (UTC) */;
    public final static String[] CONTAINER_ELASTIC_ENV = new String[]{"discovery.type=single-node", "ES_JAVA_OPTS=-Xms512m -Xmx512m",
            "logger.level=WARN"};

    public final static Container CONTAINER_ELASTIC = Container.builder()
            .id(CONTAINER_ELASTIC_ID)
            .name(CONTAINER_ELASTIC_NAME)
            .internalName(CONTAINER_ELASTIC_INTERNAL_NAME)
            .imageId(IMAGE_ELASTIC_ID)
            .image(IMAGE_ELASTIC)
            .hash(CONTAINER_ELASTIC_HASH)
            .ipAddress(CONTAINER_ELASTIC_IP)
            .created(CONTAINER_ELASTIC_CREATED)
            .creator(USER_1)
            .build();

    public final static Long DATABASE_1_ID = 1L;
    public final static String DATABASE_1_NAME = "Weather";
    public final static String DATABASE_1_INTERNALNAME = "weather";
    public final static Boolean DATABASE_1_PUBLIC = false;
    public final static String DATABASE_1_EXCHANGE = "dbrepo." + CONTAINER_1_INTERNALNAME;
    public final static Instant DATABASE_1_CREATED = Instant.ofEpochSecond(1677399741) /* 2023-02-26 08:22:21 (UTC) */;
    public final static Instant DATABASE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399741) /* 2023-02-26 08:22:21 (UTC) */;

    public final static Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_1_PUBLIC)
            .name(DATABASE_1_NAME)
            .container(CONTAINER_1)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .created(DATABASE_1_CREATED)
            .lastModified(DATABASE_1_LAST_MODIFIED)
            .creator(USER_1)
            .owner(USER_1)
            .tables(List.of()) /* TABLE_1, TABLE_2, TABLE_3 */
            .views(List.of())
            .build();

    public final static DatabaseDto DATABASE_1_DTO = DatabaseDto.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .isPublic(DATABASE_1_PUBLIC)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .tables(List.of()) /* TABLE_1, TABLE_2, TABLE_3 */
            .views(List.of())
            .build();

    public final static DatabaseAccess DATABASE_1_OWNER_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseAccess DATABASE_1_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_1_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_1_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_1_ID)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseCreateDto DATABASE_1_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_1_NAME)
            .isPublic(DATABASE_1_PUBLIC)
            .build();

    public final static Long DATABASE_2_ID = 2L;
    public final static String DATABASE_2_NAME = "Zoo";
    public final static String DATABASE_2_INTERNALNAME = "zoo";
    public final static Boolean DATABASE_2_PUBLIC = false;
    public final static String DATABASE_2_EXCHANGE = "dbrepo." + CONTAINER_2_INTERNALNAME;
    public final static Instant DATABASE_2_CREATED = Instant.ofEpochSecond(1677399772) /* 2023-02-26 08:22:52 (UTC) */;
    public final static Instant DATABASE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677399772) /* 2023-02-26 08:22:52 (UTC) */;

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .created(DATABASE_1_CREATED)
            .lastModified(Instant.now())
            .isPublic(DATABASE_2_PUBLIC)
            .name(DATABASE_2_NAME)
            .container(CONTAINER_2)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .created(DATABASE_2_CREATED)
            .lastModified(DATABASE_2_LAST_MODIFIED)
            .creator(USER_2)
            .owner(USER_2)
            .tables(List.of()) /* TABLE_4, TABLE_5, TABLE_6 */
            .views(List.of()) /* VIEW_4 */
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

    public final static DatabaseAccess DATABASE_2_OWNER_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseAccess DATABASE_2_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_2_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseAccess DATABASE_2_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_2_ID)
            .huserid(USER_2_ID)
            .build();

    public final static DatabaseCreateDto DATABASE_2_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_2_NAME)
            .isPublic(DATABASE_2_PUBLIC)
            .build();

    public final static Long DATABASE_3_ID = 3L;
    public final static String DATABASE_3_NAME = "Musicology";
    public final static String DATABASE_3_INTERNALNAME = "musicology";
    public final static Boolean DATABASE_3_PUBLIC = true;
    public final static String DATABASE_3_EXCHANGE = "dbrepo." + CONTAINER_3_INTERNALNAME;
    public final static Instant DATABASE_3_CREATED = Instant.ofEpochSecond(1677399792) /* 2023-02-26 08:23:12 (UTC) */;
    public final static Instant DATABASE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677399792) /* 2023-02-26 08:23:12 (UTC) */;

    public final static Database DATABASE_3 = Database.builder()
            .id(DATABASE_3_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_3_PUBLIC)
            .name(DATABASE_3_NAME)
            .container(CONTAINER_3)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchangeName(DATABASE_3_EXCHANGE)
            .created(DATABASE_3_CREATED)
            .lastModified(DATABASE_3_LAST_MODIFIED)
            .creator(USER_3)
            .owner(USER_3)
            .tables(List.of()) /* TABLE_8 */
            .views(List.of())
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

    public final static DatabaseAccess DATABASE_3_OWNER_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_3_ID)
            .build();

    public final static DatabaseAccess DATABASE_3_READ_ACCESS = DatabaseAccess.builder()
            .type(AccessType.READ)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseAccess DATABASE_3_WRITE_OWN_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_OWN)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseAccess DATABASE_3_WRITE_ALL_ACCESS = DatabaseAccess.builder()
            .type(AccessType.WRITE_ALL)
            .hdbid(DATABASE_3_ID)
            .huserid(USER_1_ID)
            .build();

    public final static DatabaseCreateDto DATABASE_3_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_3_NAME)
            .isPublic(DATABASE_3_PUBLIC)
            .build();

    public final static Long DATABASE_4_ID = 4L;
    public final static String DATABASE_4_NAME = "Weather AT";
    public final static Boolean DATABASE_4_PUBLIC = false;
    public final static String DATABASE_4_INTERNALNAME = "weather_at";
    public final static String DATABASE_4_EXCHANGE = DATABASE_4_INTERNALNAME;
    public final static Instant DATABASE_4_CREATED = Instant.ofEpochSecond(1677399813) /* 2023-02-26 08:23:33 (UTC) */;
    public final static Instant DATABASE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677399813) /* 2023-02-26 08:23:33 (UTC) */;

    public final static Database DATABASE_4 = Database.builder()
            .id(DATABASE_4_ID)
            .created(Instant.now().minus(4, HOURS))
            .lastModified(Instant.now())
            .isPublic(DATABASE_4_PUBLIC)
            .name(DATABASE_4_NAME)
            .container(CONTAINER_4)
            .internalName(DATABASE_4_INTERNALNAME)
            .exchangeName(DATABASE_4_EXCHANGE)
            .created(DATABASE_4_CREATED)
            .lastModified(DATABASE_4_LAST_MODIFIED)
            .creator(USER_4)
            .owner(USER_4)
            .tables(List.of())
            .views(List.of())
            .build();

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "Weather AUS";
    public final static String TABLE_1_INTERNALNAME = "weather_aus";
    public final static String TABLE_1_DESCRIPTION = "Weather in the world";
    public final static String TABLE_1_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_1_INTERNALNAME;
    public final static String TABLE_1_ROUTING_KEY = TABLE_1_QUEUE_NAME;
    public final static Instant TABLE_1_CREATED = Instant.ofEpochSecond(1677399975) /* 2023-02-26 08:26:15 (UTC) */;
    public final static Instant TABLE_1_LAST_MODIFIED = Instant.ofEpochSecond(1677399975) /* 2023-02-26 08:26:15 (UTC) */;

    public final static Long TABLE_2_ID = 2L;
    public final static String TABLE_2_NAME = "Weather Location";
    public final static String TABLE_2_INTERNALNAME = "weather_location";
    public final static String TABLE_2_DESCRIPTION = "Weather location";
    public final static String TABLE_2_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_2_INTERNALNAME;
    public final static String TABLE_2_ROUTING_KEY = TABLE_2_QUEUE_NAME;
    public final static Instant TABLE_2_CREATED = Instant.ofEpochSecond(1677400007) /* 2023-02-26 08:26:47 (UTC) */;
    public final static Instant TABLE_2_LAST_MODIFIED = Instant.ofEpochSecond(1677400007) /* 2023-02-26 08:26:47 (UTC) */;

    public final static Long TABLE_3_ID = 3L;
    public final static String TABLE_3_NAME = "Traffic Zürich";
    public final static String TABLE_3_INTERNALNAME = "traffic_zurich";
    public final static String TABLE_3_DESCRIPTION = "https://www.kaggle.com/laa283/zurich-public-transport/version/2";
    public final static String TABLE_3_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_3_INTERNALNAME;
    public final static String TABLE_3_ROUTING_KEY = TABLE_3_QUEUE_NAME;
    public final static Instant TABLE_3_CREATED = Instant.ofEpochSecond(1677400031) /* 2023-02-26 08:27:11 (UTC) */;
    public final static Instant TABLE_3_LAST_MODIFIED = Instant.ofEpochSecond(1677400031) /* 2023-02-26 08:27:11 (UTC) */;

    public final static TableCreateDto TABLE_3_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(List.of())
            .build();

    public final static Long TABLE_4_ID = 4L;
    public final static String TABLE_4_NAME = "zoo";
    public final static String TABLE_4_INTERNALNAME = "zoo";
    public final static String TABLE_4_DESCRIPTION = "Some Kaggle dataset";
    public final static String TABLE_4_QUEUE_NAME = DATABASE_2_EXCHANGE + "." + TABLE_4_INTERNALNAME;
    public final static String TABLE_4_ROUTING_KEY = TABLE_4_QUEUE_NAME;
    public final static Instant TABLE_4_CREATED = Instant.ofEpochSecond(1677400067) /* 2023-02-26 08:27:47 (UTC) */;
    public final static Instant TABLE_4_LAST_MODIFIED = Instant.ofEpochSecond(1677400067) /* 2023-02-26 08:27:47 (UTC) */;

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

    public final static Long TABLE_7_ID = 7L;
    public final static String TABLE_7_NAME = "Sensor";
    public final static String TABLE_7_INTERNAL_NAME = "sensor";
    public final static String TABLE_7_DESCRIPTION = "Hello sensor";
    public final static String TABLE_7_QUEUE_NAME = DATABASE_1_EXCHANGE + "." + TABLE_7_INTERNAL_NAME;
    public final static String TABLE_7_ROUTING_KEY = TABLE_7_QUEUE_NAME;
    public final static Instant TABLE_7_CREATED = Instant.ofEpochSecond(1677400175) /* 2023-02-26 08:29:35 (UTC) */;
    public final static Instant TABLE_7_LAST_MODIFIED = Instant.ofEpochSecond(1677400175) /* 2023-02-26 08:29:35 (UTC) */;

    public final static Table TABLE_7 = Table.builder()
            .id(TABLE_7_ID)
            .created(Instant.now())
            .internalName(TABLE_7_INTERNAL_NAME)
            .description(TABLE_7_DESCRIPTION)
            .name(TABLE_7_NAME)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_7_QUEUE_NAME)
            .routingKey(TABLE_7_ROUTING_KEY)
            .columns(List.of(TableColumn.builder()
                    .id(1L)
                    .ordinalPosition(0)
                    .cdbid(DATABASE_1_ID)
                    .tid(TABLE_1_ID)
                    .name("Timestamp")
                    .internalName("timestamp")
                    .columnType(TableColumnType.TIMESTAMP)
                    .dfid(IMAGE_DATE_3_ID)
                    .isNullAllowed(false)
                    .isUnique(true)
                    .autoGenerated(false)
                    .isPrimaryKey(true)
                    .build()))
            .creator(USER_1)
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

    public final static Table TABLE_8 = Table.builder()
            .id(TABLE_8_ID)
            .created(Instant.now())
            .internalName(TABLE_8_INTERNAL_NAME)
            .description(TABLE_8_DESCRIPTION)
            .name(TABLE_8_NAME)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_8_QUEUE_NAME)
            .routingKey(TABLE_8_ROUTING_KEY)
            .columns(List.of(TableColumn.builder()
                            .id(1L)
                            .ordinalPosition(0)
                            .cdbid(DATABASE_3_ID)
                            .tid(TABLE_8_ID)
                            .name("ID")
                            .internalName("id")
                            .columnType(TableColumnType.NUMBER)
                            .dfid(null)
                            .isNullAllowed(false)
                            .isUnique(true)
                            .autoGenerated(true)
                            .isPrimaryKey(true)
                            .build(),
                    TableColumn.builder()
                            .id(2L)
                            .ordinalPosition(1)
                            .cdbid(DATABASE_3_ID)
                            .tid(TABLE_8_ID)
                            .name("Value")
                            .internalName("value")
                            .columnType(TableColumnType.DECIMAL)
                            .dfid(null)
                            .isNullAllowed(false)
                            .isUnique(false)
                            .autoGenerated(false)
                            .isPrimaryKey(false)
                            .build()))
            .creator(USER_1)
            .created(TABLE_8_CREATED)
            .lastModified(TABLE_8_LAST_MODIFIED)
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

    public final static Long COLUMN_4_1_ID = 9L;
    public final static Integer COLUMN_4_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_4_1_PRIMARY = false;
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

    public final static Long COLUMN_4_2_ID = 10L;
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

    public final static Long COLUMN_4_3_ID = 11L;
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

    public final static Long COLUMN_4_4_ID = 12L;
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

    public final static Long COLUMN_4_5_ID = 13L;
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

    public final static Long COLUMN_4_6_ID = 14L;
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

    public final static Long COLUMN_4_7_ID = 15L;
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

    public final static Long COLUMN_4_8_ID = 16L;
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

    public final static Long COLUMN_4_9_ID = 17L;
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

    public final static Long COLUMN_4_10_ID = 18L;
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

    public final static Long COLUMN_4_11_ID = 19L;
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

    public final static Long COLUMN_4_12_ID = 20L;
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

    public final static Long COLUMN_4_13_ID = 21L;
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

    public final static Long COLUMN_4_14_ID = 22L;
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

    public final static Long COLUMN_4_15_ID = 23L;
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

    public final static Long COLUMN_4_16_ID = 24L;
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

    public final static Long COLUMN_4_17_ID = 25L;
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

    public final static Long COLUMN_4_18_ID = 26L;
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

    public final static Long COLUMN_4_19_ID = 27L;
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

    public final static Long COLUMN_4_20_ID = 28L;
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

    public final static Long COLUMN_4_21_ID = 29L;
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

    public final static Long COLUMN_5_1_ID = 23L;
    public final static Integer COLUMN_5_1_ORDINALPOS = 0;
    public final static Boolean COLUMN_5_1_PRIMARY = true;
    public final static String COLUMN_5_1_NAME = "id";
    public final static String COLUMN_5_1_INTERNAL_NAME = "id";
    public final static TableColumnType COLUMN_5_1_TYPE = TableColumnType.NUMBER;
    public final static Long COLUMN_5_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_1_NULL = false;
    public final static Boolean COLUMN_5_1_UNIQUE = true;
    public final static Boolean COLUMN_5_1_AUTO_GENERATED = true;
    public final static String COLUMN_5_1_FOREIGN_KEY = null;
    public final static String COLUMN_5_1_CHECK = null;
    public final static List<String> COLUMN_5_1_ENUM_VALUES = null;

    public final static Long COLUMN_5_2_ID = 24L;
    public final static Integer COLUMN_5_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_5_2_PRIMARY = false;
    public final static String COLUMN_5_2_NAME = "firstname";
    public final static String COLUMN_5_2_INTERNAL_NAME = "firstname";
    public final static TableColumnType COLUMN_5_2_TYPE = TableColumnType.STRING;
    public final static Long COLUMN_5_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_2_NULL = false;
    public final static Boolean COLUMN_5_2_UNIQUE = false;
    public final static Boolean COLUMN_5_2_AUTO_GENERATED = false;
    public final static String COLUMN_5_2_FOREIGN_KEY = null;
    public final static String COLUMN_5_2_CHECK = null;
    public final static List<String> COLUMN_5_2_ENUM_VALUES = null;

    public final static Long COLUMN_5_3_ID = 25L;
    public final static Integer COLUMN_5_3_ORDINALPOS = 2;
    public final static Boolean COLUMN_5_3_PRIMARY = false;
    public final static String COLUMN_5_3_NAME = "lastname";
    public final static String COLUMN_5_3_INTERNAL_NAME = "lastname";
    public final static TableColumnType COLUMN_5_3_TYPE = TableColumnType.STRING;
    public final static Long COLUMN_5_3_DATE_FORMAT = null;
    public final static Boolean COLUMN_5_3_NULL = false;
    public final static Boolean COLUMN_5_3_UNIQUE = false;
    public final static Boolean COLUMN_5_3_AUTO_GENERATED = false;
    public final static String COLUMN_5_3_FOREIGN_KEY = null;
    public final static String COLUMN_5_3_CHECK = null;
    public final static List<String> COLUMN_5_3_ENUM_VALUES = null;

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
    public final static String QUERY_1_DOI = "1111/1";
    public final static Long QUERY_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long QUERY_1_DATABASE_ID = DATABASE_1_ID;
    public final static String QUERY_1_QUERY_HASH = "a3b8ac39e38167d14cf3a9c20a69e4b6954d049525390b973a2c23064953a992";
    public final static String QUERY_1_RESULT_HASH = "8358c8ade4849d2094ab5bb29127afdae57e6bb5acb1db7af603813d406c467a";
    public final static Instant QUERY_1_CREATED = Instant.now();
    public final static Instant QUERY_1_EXECUTION = Instant.now();
    public final static Boolean QUERY_1_PERSISTED = false;

    public final static Query QUERY_1 = Query.builder()
            .id(QUERY_1_ID)
            .query(QUERY_1_STATEMENT)
            .queryHash(QUERY_1_QUERY_HASH)
            .resultHash(QUERY_1_RESULT_HASH)
            .created(QUERY_1_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_1_PERSISTED)
            .build();

    public final static QueryDto QUERY_1_DTO = QueryDto.builder()
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
    public final static String QUERY_2_RESULT_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static Instant QUERY_2_CREATED = Instant.now().minus(2, MINUTES);
    public final static Instant QUERY_2_EXECUTION = Instant.now().minus(1, MINUTES);
    public final static Instant QUERY_2_LAST_MODIFIED = Instant.ofEpochSecond(1541588352);
    public final static Long QUERY_2_RESULT_NUMBER = 5L;
    public final static Boolean QUERY_2_PERSISTED = true;

    public final static Query QUERY_2 = Query.builder()
            .id(QUERY_2_ID)
            .query(QUERY_2_STATEMENT)
            .queryHash(QUERY_2_QUERY_HASH)
            .resultHash(QUERY_2_RESULT_HASH)
            .created(QUERY_2_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_2_PERSISTED)
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
    public final static Long QUERY_3_CONTAINER_ID = CONTAINER_3_ID;
    public final static Long QUERY_3_DATABASE_ID = DATABASE_3_ID;
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
    public final static Long QUERY_4_RESULT_NUMBER = 6L;
    public final static Boolean QUERY_4_PERSISTED = true;

    public final static Query QUERY_4 = Query.builder()
            .id(QUERY_4_ID)
            .query(QUERY_4_STATEMENT)
            .queryHash(QUERY_4_QUERY_HASH)
            .resultHash(QUERY_4_RESULT_HASH)
            .created(QUERY_4_CREATED)
            .createdBy(USER_1_USERNAME)
            .isPersisted(QUERY_4_PERSISTED)
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
                    .isUnique(COLUMN_1_1_UNIQUE)
                    .autoGenerated(COLUMN_1_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_1_PRIMARY)
                    .enumValues(COLUMN_1_1_ENUM_VALUES)
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
                    .isUnique(COLUMN_1_2_UNIQUE)
                    .autoGenerated(COLUMN_1_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_2_PRIMARY)
                    .enumValues(COLUMN_1_2_ENUM_VALUES)
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
                    .isUnique(COLUMN_1_3_UNIQUE)
                    .autoGenerated(COLUMN_1_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_3_PRIMARY)
                    .enumValues(COLUMN_1_3_ENUM_VALUES)
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
                    .isUnique(COLUMN_1_4_UNIQUE)
                    .autoGenerated(COLUMN_1_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_4_PRIMARY)
                    .enumValues(COLUMN_1_4_ENUM_VALUES)
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
                    .isUnique(COLUMN_1_5_UNIQUE)
                    .autoGenerated(COLUMN_1_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_1_5_PRIMARY)
                    .enumValues(COLUMN_1_5_ENUM_VALUES)
                    .build());

    public final static Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .created(Instant.now())
            .internalName(TABLE_1_INTERNALNAME)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .columns(TABLE_1_COLUMNS)
            .creator(USER_1)
            .created(TABLE_1_CREATED)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .build();

    public final static Table TABLE_1_NOCOLS = Table.builder()
            .id(TABLE_1_ID)
            .created(Instant.now())
            .internalName(TABLE_1_INTERNALNAME)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .columns(List.of())
            .creator(USER_1)
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
                    .isUnique(COLUMN_2_1_UNIQUE)
                    .autoGenerated(COLUMN_2_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_1_PRIMARY)
                    .enumValues(COLUMN_2_1_ENUM_VALUES)
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
                    .isUnique(COLUMN_2_2_UNIQUE)
                    .autoGenerated(COLUMN_2_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_2_PRIMARY)
                    .enumValues(COLUMN_2_2_ENUM_VALUES)
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
                    .isUnique(COLUMN_2_3_UNIQUE)
                    .autoGenerated(COLUMN_2_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_2_3_PRIMARY)
                    .enumValues(COLUMN_2_3_ENUM_VALUES)
                    .build());

    public final static Table TABLE_2 = Table.builder()
            .id(TABLE_2_ID)
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
            .created(TABLE_2_CREATED)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .build();

    public final static Table TABLE_2_NOCOLS = Table.builder()
            .id(TABLE_2_ID)
            .created(Instant.now())
            .internalName(TABLE_2_INTERNALNAME)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_2_QUEUE_NAME)
            .routingKey(TABLE_2_ROUTING_KEY)
            .columns(List.of())
            .creator(USER_1)
            .created(TABLE_2_CREATED)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .build();

    public final static List<TableColumn> TABLE_3_COLUMNS = List.of(TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(0)
                    .autoGenerated(true)
                    .columnType(TableColumnType.NUMBER)
                    .name("id")
                    .internalName("id")
                    .isNullAllowed(false)
                    .isPrimaryKey(true)
                    .isUnique(true)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(1)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("linie")
                    .internalName("linie")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(2)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("richtung")
                    .internalName("richtung")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(3)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("betriebsdatum")
                    .internalName("betriebsdatum")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dfid(IMAGE_DATE_2_ID)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(4)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fahrzeug")
                    .internalName("fahrzeug")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(5)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("kurs")
                    .internalName("kurs")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(6)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("seq_von")
                    .internalName("seq_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(7)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_diva_von")
                    .internalName("halt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(8)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_diva_von")
                    .internalName("halt_punkt_diva_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(9)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("halt_kurz_von1")
                    .internalName("halt_kurz_von1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(10)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("datum_von")
                    .internalName("datum_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dfid(IMAGE_DATE_2_ID)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(11)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_an_von")
                    .internalName("soll_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(12)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_an_von")
                    .internalName("ist_an_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(13)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_ab_von")
                    .internalName("soll_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(14)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_ab_von")
                    .internalName("ist_ab_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(15)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("seq_nach")
                    .internalName("seq_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(16)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_diva_nach")
                    .internalName("halt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(17)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_diva_nach")
                    .internalName("halt_punkt_diva_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(18)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("halt_kurz_nach1")
                    .internalName("halt_kurz_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(19)
                    .autoGenerated(false)
                    .columnType(TableColumnType.DATE)
                    .name("datum_nach")
                    .internalName("datum_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dfid(IMAGE_DATE_2_ID)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(20)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_an_nach")
                    .internalName("soll_an_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(21)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_an_nach1")
                    .internalName("ist_an_nach1")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(22)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("soll_ab_nach")
                    .internalName("soll_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(23)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("ist_ab_nach")
                    .internalName("ist_ab_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(24)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fahrt_id")
                    .internalName("fahrt_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(25)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fahrweg_id")
                    .internalName("fahrweg_id")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(26)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fw_no")
                    .internalName("fw_no")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(27)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fw_typ")
                    .internalName("fw_typ")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(28)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("fw_kurz")
                    .internalName("fw_kurz")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(29)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("fw_lang")
                    .internalName("fw_lang")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(30)
                    .autoGenerated(false)
                    .columnType(TableColumnType.STRING)
                    .name("umlauf_von")
                    .internalName("umlauf_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(31)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_id_von")
                    .internalName("halt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(32)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_id_nach")
                    .internalName("halt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(33)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_id_von")
                    .internalName("halt_punkt_id_von")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build(),
            TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_1_ID)
                    .ordinalPosition(34)
                    .autoGenerated(false)
                    .columnType(TableColumnType.NUMBER)
                    .name("halt_punkt_id_nach")
                    .internalName("halt_punkt_id_nach")
                    .isNullAllowed(true)
                    .isPrimaryKey(false)
                    .isUnique(false)
                    .dateFormat(null)
                    .checkExpression(null)
                    .enumValues(null)
                    .build());

    public final static Table TABLE_3 = Table.builder()
            .id(TABLE_3_ID)
            .created(Instant.now())
            .internalName(TABLE_3_INTERNALNAME)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_3_QUEUE_NAME)
            .routingKey(TABLE_3_ROUTING_KEY)
            .columns(TABLE_3_COLUMNS)
            .creator(USER_1)
            .created(TABLE_3_CREATED)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .build();

    public final static Table TABLE_3_NOCOLS = Table.builder()
            .id(TABLE_3_ID)
            .created(Instant.now())
            .internalName(TABLE_3_INTERNALNAME)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .queueName(TABLE_3_QUEUE_NAME)
            .routingKey(TABLE_3_ROUTING_KEY)
            .columns(List.of())
            .creator(USER_1)
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
                    .isUnique(COLUMN_4_1_UNIQUE)
                    .autoGenerated(COLUMN_4_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_1_PRIMARY)
                    .enumValues(COLUMN_4_1_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_2_UNIQUE)
                    .autoGenerated(COLUMN_4_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_2_PRIMARY)
                    .enumValues(COLUMN_4_2_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_3_UNIQUE)
                    .autoGenerated(COLUMN_4_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_3_PRIMARY)
                    .enumValues(COLUMN_4_3_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_4_UNIQUE)
                    .autoGenerated(COLUMN_4_4_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_4_PRIMARY)
                    .enumValues(COLUMN_4_4_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_5_UNIQUE)
                    .autoGenerated(COLUMN_4_5_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_5_PRIMARY)
                    .enumValues(COLUMN_4_5_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_6_UNIQUE)
                    .autoGenerated(COLUMN_4_6_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_6_PRIMARY)
                    .enumValues(COLUMN_4_6_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_7_UNIQUE)
                    .autoGenerated(COLUMN_4_7_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_7_PRIMARY)
                    .enumValues(COLUMN_4_7_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_8_UNIQUE)
                    .autoGenerated(COLUMN_4_8_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_8_PRIMARY)
                    .enumValues(COLUMN_4_8_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_9_UNIQUE)
                    .autoGenerated(COLUMN_4_9_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_9_PRIMARY)
                    .enumValues(COLUMN_4_9_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_10_UNIQUE)
                    .autoGenerated(COLUMN_4_10_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_10_PRIMARY)
                    .enumValues(COLUMN_4_10_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_11_UNIQUE)
                    .autoGenerated(COLUMN_4_11_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_11_PRIMARY)
                    .enumValues(COLUMN_4_11_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_12_UNIQUE)
                    .autoGenerated(COLUMN_4_12_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_12_PRIMARY)
                    .enumValues(COLUMN_4_12_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_13_UNIQUE)
                    .autoGenerated(COLUMN_4_13_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_13_PRIMARY)
                    .enumValues(COLUMN_4_13_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_14_UNIQUE)
                    .autoGenerated(COLUMN_4_14_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_14_PRIMARY)
                    .enumValues(COLUMN_4_14_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_15_UNIQUE)
                    .autoGenerated(COLUMN_4_15_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_15_PRIMARY)
                    .enumValues(COLUMN_4_15_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_16_UNIQUE)
                    .autoGenerated(COLUMN_4_16_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_16_PRIMARY)
                    .enumValues(COLUMN_4_16_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_17_UNIQUE)
                    .autoGenerated(COLUMN_4_17_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_17_PRIMARY)
                    .enumValues(COLUMN_4_17_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_18_UNIQUE)
                    .autoGenerated(COLUMN_4_18_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_18_PRIMARY)
                    .enumValues(COLUMN_4_18_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_19_UNIQUE)
                    .autoGenerated(COLUMN_4_19_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_19_PRIMARY)
                    .enumValues(COLUMN_4_19_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_20_UNIQUE)
                    .autoGenerated(COLUMN_4_20_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_20_PRIMARY)
                    .enumValues(COLUMN_4_20_ENUM_VALUES)
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
                    .isUnique(COLUMN_4_21_UNIQUE)
                    .autoGenerated(COLUMN_4_21_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_4_21_PRIMARY)
                    .enumValues(COLUMN_4_21_ENUM_VALUES)
                    .build());

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
            .creator(USER_1)
            .created(TABLE_4_CREATED)
            .lastModified(TABLE_4_LAST_MODIFIED)
            .build();

    public final static List<ColumnCreateDto> TABLE_4_COLUMNS_INVALID_CREATE = List.of(ColumnCreateDto.builder()
            .name(COLUMN_4_2_NAME)
            .type(COLUMN_4_2_TYPE_DTO)
            .dfid(COLUMN_4_2_DATE_FORMAT)
            .nullAllowed(COLUMN_4_2_NULL)
            .unique(COLUMN_4_2_UNIQUE)
            .primaryKey(COLUMN_4_2_PRIMARY)
            .enumValues(COLUMN_4_2_ENUM_VALUES_ARRAY)
            .foreignKey("somecolumn")
            .references("sometable")
            .build());

    public final static List<ColumnCreateDto> TABLE_4_COLUMNS_CREATE = List.of(ColumnCreateDto.builder()
            .name(COLUMN_4_2_NAME)
            .type(COLUMN_4_2_TYPE_DTO)
            .dfid(COLUMN_4_2_DATE_FORMAT)
            .nullAllowed(COLUMN_4_2_NULL)
            .unique(COLUMN_4_2_UNIQUE)
            .primaryKey(COLUMN_4_2_PRIMARY)
            .enumValues(COLUMN_4_2_ENUM_VALUES_ARRAY)
            .build());

    public final static TableCreateDto TABLE_4_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_4_NAME)
            .description(TABLE_4_DESCRIPTION)
            .columns(TABLE_4_COLUMNS_CREATE)
            .build();

    public final static TableCreateDto TABLE_4_INVALID_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_4_NAME)
            .description(TABLE_4_DESCRIPTION)
            .columns(TABLE_4_COLUMNS_INVALID_CREATE)
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
                    .isUnique(COLUMN_5_1_UNIQUE)
                    .autoGenerated(COLUMN_5_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_1_PRIMARY)
                    .enumValues(COLUMN_5_1_ENUM_VALUES)
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
                    .isUnique(COLUMN_5_2_UNIQUE)
                    .autoGenerated(COLUMN_5_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_2_PRIMARY)
                    .enumValues(COLUMN_5_2_ENUM_VALUES)
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
                    .isUnique(COLUMN_5_3_UNIQUE)
                    .autoGenerated(COLUMN_5_3_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_5_3_PRIMARY)
                    .enumValues(COLUMN_5_3_ENUM_VALUES)
                    .build());

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
            .creator(USER_1)
            .created(TABLE_5_CREATED)
            .lastModified(TABLE_5_LAST_MODIFIED)
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
                    .isUnique(COLUMN_6_1_UNIQUE)
                    .autoGenerated(COLUMN_6_1_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_6_1_PRIMARY)
                    .enumValues(COLUMN_6_1_ENUM_VALUES)
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
                    .isUnique(COLUMN_6_2_UNIQUE)
                    .autoGenerated(COLUMN_6_2_AUTO_GENERATED)
                    .isPrimaryKey(COLUMN_6_2_PRIMARY)
                    .enumValues(COLUMN_6_2_ENUM_VALUES)
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
            .created(TABLE_6_CREATED)
            .lastModified(TABLE_6_LAST_MODIFIED)
            .build();

    public final static Long VIEW_1_ID = 1L;
    public final static Boolean VIEW_1_INITIAL_VIEW = false;
    public final static String VIEW_1_NAME = "JUnit";
    public final static String VIEW_1_INTERNAL_NAME = "junit";
    public final static Long VIEW_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_1_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_1_PUBLIC = true;
    public final static String VIEW_1_QUERY = "select `location`, `lat`, `lng` from `weather_location`";

    public final static View VIEW_1 = View.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vcid(VIEW_1_CONTAINER_ID)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .query(VIEW_1_QUERY)
            .creator(USER_1)
            .build();

    public final static ViewDto VIEW_1_DTO = ViewDto.builder()
            .id(VIEW_1_ID)
            .isInitialView(VIEW_1_INITIAL_VIEW)
            .name(VIEW_1_NAME)
            .internalName(VIEW_1_INTERNAL_NAME)
            .vdbid(VIEW_1_DATABASE_ID)
            .isPublic(VIEW_1_PUBLIC)
            .query(VIEW_1_QUERY)
            .build();

    public final static Long VIEW_2_ID = 2L;
    public final static Boolean VIEW_2_INITIAL_VIEW = false;
    public final static String VIEW_2_NAME = "JUnit2";
    public final static String VIEW_2_INTERNAL_NAME = "junit2";
    public final static Long VIEW_2_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_2_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_2_PUBLIC = true;
    public final static String VIEW_2_QUERY = "select `date`, `location`, `mintemp`, `rainfall` from `weather_aus`";

    public final static View VIEW_2 = View.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vcid(VIEW_2_CONTAINER_ID)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .query(VIEW_2_QUERY)
            .creator(USER_1)
            .build();

    public final static ViewDto VIEW_2_DTO = ViewDto.builder()
            .id(VIEW_2_ID)
            .isInitialView(VIEW_2_INITIAL_VIEW)
            .name(VIEW_2_NAME)
            .internalName(VIEW_2_INTERNAL_NAME)
            .vdbid(VIEW_2_DATABASE_ID)
            .isPublic(VIEW_2_PUBLIC)
            .query(VIEW_2_QUERY)
            .build();

    public final static Long VIEW_3_ID = 3L;
    public final static Boolean VIEW_3_INITIAL_VIEW = false;
    public final static String VIEW_3_NAME = "JUnit3";
    public final static String VIEW_3_INTERNAL_NAME = "junit3";
    public final static Long VIEW_3_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long VIEW_3_DATABASE_ID = DATABASE_1_ID;
    public final static Boolean VIEW_3_PUBLIC = false;
    public final static String VIEW_3_QUERY = "select w.`mintemp`, w.`rainfall`, w.`location`, m.`lat`, m.`lng` from `weather_aus` w join `mock_view` m on m.`location` = w.`location`";

    public final static View VIEW_3 = View.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vcid(VIEW_3_CONTAINER_ID)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .query(VIEW_3_QUERY)
            .creator(USER_1)
            .build();

    public final static ViewDto VIEW_3_DTO = ViewDto.builder()
            .id(VIEW_3_ID)
            .isInitialView(VIEW_3_INITIAL_VIEW)
            .name(VIEW_3_NAME)
            .internalName(VIEW_3_INTERNAL_NAME)
            .vdbid(VIEW_3_DATABASE_ID)
            .isPublic(VIEW_3_PUBLIC)
            .query(VIEW_3_QUERY)
            .build();

    public final static Long VIEW_4_ID = 4L;
    public final static Boolean VIEW_4_INITIAL_VIEW = false;
    public final static String VIEW_4_NAME = "Mock View";
    public final static String VIEW_4_INTERNAL_NAME = "mock_view";
    public final static Long VIEW_4_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long VIEW_4_DATABASE_ID = DATABASE_2_ID;
    public final static Boolean VIEW_4_PUBLIC = true;
    public final static String VIEW_4_QUERY = "SELECT `animal_name`, `hair`, `feathers`, `eggs`, `milk`, `airborne`, `aquatic`, `predator`, `toothed`, `backbone`, `breathes`, `venomous`, `fins`, `legs`, `tail`, `domestic`, `catsize`, `class_type`FROM `zoo`WHERE `class_type` = 1";

    public final static View VIEW_4 = View.builder()
            .id(VIEW_4_ID)
            .isInitialView(VIEW_4_INITIAL_VIEW)
            .name(VIEW_4_NAME)
            .internalName(VIEW_4_INTERNAL_NAME)
            .vcid(VIEW_4_CONTAINER_ID)
            .vdbid(VIEW_4_DATABASE_ID)
            .isPublic(VIEW_4_PUBLIC)
            .query(VIEW_4_QUERY)
            .build();

    public final static Long QUERY_1_RESULT_ID = 1L;
    public final static Long QUERY_1_RESULT_NUMBER = 2L;
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
    public final static Long IDENTIFIER_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long IDENTIFIER_1_DATABASE_ID = DATABASE_1_ID;
    public final static String IDENTIFIER_1_DESCRIPTION = "Selecting all from the weather Austrian table";
    public final static String IDENTIFIER_1_DESCRIPTION_MODIFY = "Selecting some from the weather Austrian table";
    public final static String IDENTIFIER_1_TITLE = "Austrian weather data";
    public final static String IDENTIFIER_1_TITLE_MODIFY = "Austrian weather some data";
    public final static String IDENTIFIER_1_DOI = "10.1000/182";
    public final static VisibilityType IDENTIFIER_1_VISIBILITY = VisibilityType.EVERYONE;
    public final static VisibilityTypeDto IDENTIFIER_1_VISIBILITY_DTO = VisibilityTypeDto.EVERYONE;
    public final static Instant IDENTIFIER_1_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant IDENTIFIER_1_MODIFIED = Instant.ofEpochSecond(1541588352);
    public final static Instant IDENTIFIER_1_EXECUTION = Instant.ofEpochSecond(1541588352);
    public final static Integer IDENTIFIER_1_PUBLICATION_MONTH = 5;
    public final static Integer IDENTIFIER_1_PUBLICATION_YEAR = 2022;
    public final static Integer IDENTIFIER_1_PUBLICATION_DAY = null;
    public final static String IDENTIFIER_1_QUERY_HASH = "abc";
    public final static String IDENTIFIER_1_RESULT_HASH = "def";
    public final static String IDENTIFIER_1_QUERY = "SELECT `id` FROM `foobar`";
    public final static String IDENTIFIER_1_NORMALIZED = "SELECT `id` FROM `foobar`";
    public final static Long IDENTIFIER_1_RESULT_NUMBER = 2L;
    public final static String IDENTIFIER_1_PUBLISHER = "Austrian Government";
    public final static IdentifierType IDENTIFIER_1_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_1_TYPE_DTO = IdentifierTypeDto.DATABASE;

    public final static Creator IDENTIFIER_1_CREATOR_1 = Creator.builder()
            .id(CREATOR_1_ID)
            .pid(IDENTIFIER_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static CreatorDto IDENTIFIER_1_CREATOR_1_DTO = CreatorDto.builder()
            .id(CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static Creator IDENTIFIER_1_CREATOR_2 = Creator.builder()
            .id(CREATOR_2_ID)
            .pid(IDENTIFIER_1_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static CreatorDto IDENTIFIER_1_CREATOR_2_DTO = CreatorDto.builder()
            .id(CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static Identifier IDENTIFIER_1 = Identifier.builder()
            .id(IDENTIFIER_1_ID)
            .containerId(IDENTIFIER_1_CONTAINER_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
            .doi(IDENTIFIER_1_DOI)
            .visibility(IDENTIFIER_1_VISIBILITY)
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
            .creators(List.of(IDENTIFIER_1_CREATOR_1, IDENTIFIER_1_CREATOR_2))
            .build();

    public final static IdentifierDto IDENTIFIER_1_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_1_ID)
            .containerId(IDENTIFIER_1_CONTAINER_ID)
            .databaseId(IDENTIFIER_1_DATABASE_ID)
            .queryId(IDENTIFIER_1_QUERY_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
            .doi(IDENTIFIER_1_DOI)
            .visibility(IDENTIFIER_1_VISIBILITY_DTO)
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
            .creators(List.of(IDENTIFIER_1_CREATOR_1_DTO, IDENTIFIER_1_CREATOR_2_DTO))
            .build();

    public final static Long IDENTIFIER_2_ID = 2L;
    public final static Long IDENTIFIER_2_QUERY_ID = QUERY_2_ID;
    public final static Long IDENTIFIER_2_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long IDENTIFIER_2_DATABASE_ID = DATABASE_2_ID;
    public final static String IDENTIFIER_2_DESCRIPTION = "Selecting all from the weather Austria table";
    public final static String IDENTIFIER_2_TITLE = "Australian weather data";
    public final static String IDENTIFIER_2_DOI = "10.1000/183";
    public final static VisibilityType IDENTIFIER_2_VISIBILITY = VisibilityType.SELF;
    public final static VisibilityTypeDto IDENTIFIER_2_VISIBILITY_DTO = VisibilityTypeDto.SELF;
    public final static Instant IDENTIFIER_2_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant IDENTIFIER_2_MODIFIED = Instant.ofEpochSecond(1541588352);
    public final static Instant IDENTIFIER_2_EXECUTION = Instant.ofEpochSecond(1541588352);
    public final static Integer IDENTIFIER_2_PUBLICATION_DAY = 14;
    public final static Integer IDENTIFIER_2_PUBLICATION_MONTH = 7;
    public final static Integer IDENTIFIER_2_PUBLICATION_YEAR = 2022;
    public final static String IDENTIFIER_2_QUERY_HASH = "abc";
    public final static String IDENTIFIER_2_RESULT_HASH = "def";
    public final static String IDENTIFIER_2_QUERY = "SELECT `id` FROM `foobar`";
    public final static String IDENTIFIER_2_NORMALIZED = "SELECT `id` FROM `foobar`";
    public final static Long IDENTIFIER_2_RESULT_NUMBER = 2L;
    public final static String IDENTIFIER_2_PUBLISHER = "Australian Government";
    public final static IdentifierType IDENTIFIER_2_TYPE = IdentifierType.SUBSET;
    public final static IdentifierTypeDto IDENTIFIER_2_TYPE_DTO = IdentifierTypeDto.SUBSET;

    public final static Creator IDENTIFIER_2_CREATOR_1 = Creator.builder()
            .id(CREATOR_1_ID)
            .pid(IDENTIFIER_2_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static CreatorDto IDENTIFIER_2_CREATOR_1_DTO = CreatorDto.builder()
            .id(CREATOR_1_ID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
            .affiliation(CREATOR_1_AFFIL)
            .build();

    public final static Creator IDENTIFIER_2_CREATOR_2 = Creator.builder()
            .id(CREATOR_2_ID)
            .pid(IDENTIFIER_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static CreatorDto IDENTIFIER_2_CREATOR_2_DTO = CreatorDto.builder()
            .id(CREATOR_2_ID)
            .firstname(CREATOR_2_FIRSTNAME)
            .lastname(CREATOR_2_LASTNAME)
            .affiliation(CREATOR_2_AFFIL)
            .build();

    public final static Identifier IDENTIFIER_2 = Identifier.builder()
            .id(IDENTIFIER_2_ID)
            .containerId(IDENTIFIER_2_CONTAINER_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
            .doi(IDENTIFIER_2_DOI)
            .visibility(IDENTIFIER_2_VISIBILITY)
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
            .build();

    public final static IdentifierDto IDENTIFIER_2_DTO = IdentifierDto.builder()
            .id(IDENTIFIER_2_ID)
            .containerId(IDENTIFIER_2_CONTAINER_ID)
            .databaseId(IDENTIFIER_2_DATABASE_ID)
            .queryId(IDENTIFIER_2_QUERY_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
            .doi(IDENTIFIER_2_DOI)
            .visibility(IDENTIFIER_2_VISIBILITY_DTO)
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
            .build();

    public final static Creator CREATOR_1 = Creator.builder()
            .id(CREATOR_1_ID)
            .pid(IDENTIFIER_1_ID)
            .orcid(CREATOR_1_ORCID)
            .firstname(CREATOR_1_FIRSTNAME)
            .lastname(CREATOR_1_LASTNAME)
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
            .containerId(CONTAINER_1_ID)
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
            .visibility(IDENTIFIER_1_VISIBILITY_DTO)
            .created(IDENTIFIER_1_CREATED)
            .lastModified(IDENTIFIER_1_MODIFIED)
            .creators(List.of(CREATOR_1_DTO))
            .build();

    public final static IdentifierCreateDto IDENTIFIER_1_DTO_REQUEST = IdentifierCreateDto.builder()
            .cid(IDENTIFIER_1_CONTAINER_ID)
            .dbid(IDENTIFIER_1_DATABASE_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
            .doi(IDENTIFIER_1_DOI)
            .visibility(IDENTIFIER_1_VISIBILITY_DTO)
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_CREATE_DTO))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_1_DTO_TRUSTED_REQUEST = IdentifierCreateDto.builder()
            .cid(IDENTIFIER_1_CONTAINER_ID)
            .dbid(IDENTIFIER_1_DATABASE_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
            .doi(IDENTIFIER_1_DOI)
            .visibility(VisibilityTypeDto.TRUSTED)
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_CREATE_DTO))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
            .build();

    public final static IdentifierCreateDto IDENTIFIER_1_DTO_SELF_REQUEST = IdentifierCreateDto.builder()
            .cid(IDENTIFIER_1_CONTAINER_ID)
            .dbid(IDENTIFIER_1_DATABASE_ID)
            .description(IDENTIFIER_1_DESCRIPTION)
            .title(IDENTIFIER_1_TITLE)
            .doi(IDENTIFIER_1_DOI)
            .visibility(VisibilityTypeDto.SELF)
            .relatedIdentifiers(List.of())
            .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_CREATE_DTO))
            .publisher(IDENTIFIER_1_PUBLISHER)
            .type(IDENTIFIER_1_TYPE_DTO)
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
            .cid(IDENTIFIER_2_CONTAINER_ID)
            .dbid(IDENTIFIER_2_DATABASE_ID)
            .description(IDENTIFIER_2_DESCRIPTION)
            .title(IDENTIFIER_2_TITLE)
            .doi(IDENTIFIER_2_DOI)
            .visibility(IDENTIFIER_2_VISIBILITY_DTO)
            .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_2_CREATE_DTO))
            .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
            .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
            .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
            .creators(List.of(CREATOR_1_CREATE_DTO, CREATOR_2_CREATE_DTO))
            .publisher(IDENTIFIER_2_PUBLISHER)
            .type(IDENTIFIER_2_TYPE_DTO)
            .build();

    public final static Long IDENTIFIER_3_ID = 3L;
    public final static Long IDENTIFIER_3_QUERY_ID = QUERY_3_ID;
    public final static Long IDENTIFIER_3_CONTAINER_ID = CONTAINER_3_ID;
    public final static Long IDENTIFIER_3_DATABASE_ID = DATABASE_3_ID;
    public final static String IDENTIFIER_3_DESCRIPTION = "Selecting all from the weather Norwegian table";
    public final static String IDENTIFIER_3_TITLE = "Norwegian weather data";
    public final static String IDENTIFIER_3_DOI = "10.1000/183";
    public final static VisibilityType IDENTIFIER_3_VISIBILITY = VisibilityType.EVERYONE;
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

    public final static Identifier IDENTIFIER_3 = Identifier.builder()
            .id(IDENTIFIER_3_ID)
            .containerId(IDENTIFIER_3_CONTAINER_ID)
            .databaseId(IDENTIFIER_3_DATABASE_ID)
            .queryId(IDENTIFIER_3_QUERY_ID)
            .description(IDENTIFIER_3_DESCRIPTION)
            .title(IDENTIFIER_3_TITLE)
            .doi(IDENTIFIER_3_DOI)
            .visibility(IDENTIFIER_3_VISIBILITY)
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
            .creator(USER_3)
            .creators(List.of(CREATOR_1, CREATOR_2, CREATOR_3))
            .build();

    public final static Long IDENTIFIER_4_ID = 4L;
    public final static Long IDENTIFIER_4_CONTAINER_ID = CONTAINER_4_ID;
    public final static Long IDENTIFIER_4_DATABASE_ID = DATABASE_4_ID;
    public final static String IDENTIFIER_4_DESCRIPTION = "Selecting all from the weather Sweden table";
    public final static String IDENTIFIER_4_TITLE = "Sweden weather data";
    public final static String IDENTIFIER_4_DOI = "10.1000/184";
    public final static VisibilityType IDENTIFIER_4_VISIBILITY = VisibilityType.EVERYONE;
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

    public final static Identifier IDENTIFIER_4 = Identifier.builder()
            .id(IDENTIFIER_4_ID)
            .containerId(IDENTIFIER_4_CONTAINER_ID)
            .databaseId(IDENTIFIER_4_DATABASE_ID)
            .description(IDENTIFIER_4_DESCRIPTION)
            .title(IDENTIFIER_4_TITLE)
            .doi(IDENTIFIER_4_DOI)
            .visibility(IDENTIFIER_4_VISIBILITY)
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

}
