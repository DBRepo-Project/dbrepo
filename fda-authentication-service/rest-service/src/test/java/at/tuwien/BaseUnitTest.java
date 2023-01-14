package at.tuwien;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserThemeSetDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static Long USER_1_ID = 1L;
    public final static String USER_1_EMAIL = "john.doe@example.com";
    public final static String USER_1_USERNAME = "jdoe";
    public final static String USER_1_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_1_PASSWORD_ENCODED = "$2a$10$0dtdedA/RLTrFbUsvpbUw.I73AXOKeQP3t5UXj96OvnDEaDb3d3M6";
    public final static String USER_1_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static String USER_1_FIRSTNAME = "John";
    public final static String USER_1_LASTNAME = "Doe";
    public final static String USER_1_AFFILIATION = "TU Graz";
    public final static String USER_1_ORCID = "000000034216302X";
    public final static String USER_1_ORCID_UNCOMPRESSED = "0000-0003-4216-302X";
    public final static String USER_1_TITLES_BEFORE = "Dr.";
    public final static String USER_1_TITLES_AFTER = "MSc BSc";
    public final static Boolean USER_1_VERIFIED = false;
    public final static Boolean USER_1_THEME_DARK = false;
    public final static Instant USER_1_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
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

    public final static SignupRequestDto USER_1_SIGNUP_REQUEST_DTO = SignupRequestDto.builder()
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .email(USER_1_EMAIL)
            .build();

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static at.tuwien.api.amqp.UserDetailsDto USER_1_DETAILS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_1_USERNAME)
            .tags(new String[]{})
            .build();

    public final static at.tuwien.api.amqp.UserDetailsDto USER_1_DETAILS_WITH_TAGS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_1_USERNAME)
            .tags(new String[]{"administrator"})
            .build();

    public final static Principal USER_1_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_1_DETAILS,
            USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());

    public final static Long USER_2_ID = 2L;
    public final static String USER_2_EMAIL = "jane.doe@example.com";
    public final static String USER_2_USERNAME = "jdoe2";
    public final static String USER_2_FIRSTNAME = "Jane";
    public final static String USER_2_LASTNAME = "Doe";
    public final static String USER_2_AFFILIATION = "TU Wien";
    public final static String USER_2_ORCID = "0000000292726225";
    public final static String USER_2_ORCID_UNCOMPRESSED = "0000-0002-9272-6225";
    public final static String USER_2_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_2_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static Boolean USER_2_VERIFIED = true;
    public final static Boolean USER_2_THEME_DARK = false;
    public final static Instant USER_2_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
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
    public final static String USER_3_EMAIL = "jonas.doe@example.com";
    public final static String USER_3_USERNAME = "jdoe3";
    public final static String USER_3_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_3_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static Boolean USER_3_VERIFIED = true;
    public final static Boolean USER_3_THEME_DARK = false;
    public final static Instant USER_3_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
    public final static Instant USER_3_LAST_MODIFIED = USER_1_CREATED;

    public final static User USER_3 = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .databasePassword(USER_3_DATABASE_PASSWORD)
            .emailVerified(USER_3_VERIFIED)
            .themeDark(USER_3_THEME_DARK)
            .created(USER_3_CREATED)
            .roles(List.of())
            .lastModified(USER_3_LAST_MODIFIED)
            .build();

    public final static SignupRequestDto USER_3_SIGNUP_REQUEST_DTO = SignupRequestDto.builder()
            .username(USER_3_USERNAME)
            .password(USER_3_PASSWORD)
            .email(USER_3_EMAIL)
            .build();

    public final static UserDetails USER_3_DETAILS = UserDetailsDto.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .authorities(List.of())
            .build();

    public final static at.tuwien.api.amqp.UserDetailsDto USER_3_DETAILS_DTO = at.tuwien.api.amqp.UserDetailsDto.builder()
            .name(USER_3_USERNAME)
            .tags(new String[]{})
            .build();

    public final static Principal USER_3_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_3_DETAILS,
            USER_3_PASSWORD, USER_3_DETAILS.getAuthorities());

    public final static UserThemeSetDto USER_THEME_DARK_DTO = UserThemeSetDto.builder()
            .themeDark(true)
            .build();

    public final static UserThemeSetDto USER_THEME_LIGHT_DTO = UserThemeSetDto.builder()
            .themeDark(false)
            .build();

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
    public final static String TOKEN_1_AUTHORIZATION = "Bearer " + TOKEN_1_TOKEN;
    public final static Instant TOKEN_1_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_1 = Token.builder()
            .id(TOKEN_1_ID)
            .token(TOKEN_1_TOKEN)
            .expires(TOKEN_1_EXPIRES)
            .build();

    public final static Long TOKEN_2_ID = 2L;
    public final static String TOKEN_2_TOKEN = "Ul0ioy8oUl0ioy8o";
    public final static String TOKEN_2_AUTHORIZATION = "Bearer " + TOKEN_2_TOKEN;
    public final static Instant TOKEN_2_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_2 = Token.builder()
            .id(TOKEN_2_ID)
            .token(TOKEN_2_TOKEN)
            .expires(TOKEN_2_EXPIRES)
            .build();

    public final static Token TOKEN_2_EXPIRED = Token.builder()
            .id(TOKEN_2_ID)
            .token(TOKEN_2_TOKEN)
            .expires(Instant.now().minus(100000000, MILLIS))
            .build();

    public final static Long TOKEN_3_ID = 3L;
    public final static String TOKEN_3_TOKEN = "Ul0ioy8oUl0ioy8o";
    public final static String TOKEN_3_AUTHORIZATION = "Bearer " + TOKEN_3_TOKEN;
    public final static Instant TOKEN_3_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_3 = Token.builder()
            .id(TOKEN_3_ID)
            .token(TOKEN_3_TOKEN)
            .expires(TOKEN_3_EXPIRES)
            .build();

    public final static String IMAGE_BROKER_IMAGE = "rabbitmq";
    public final static String IMAGE_BROKER_TAG = "3-management-alpine";

    public final static Long CONTAINER_BROKER_ID = 1L;
    public final static String CONTAINER_BROKER_NAME = "broker-service";
    public final static String CONTAINER_BROKER_INTERNAL_NAME = "broker-service";
    public final static String CONTAINER_BROKER_IP = "172.29.0.2";

    public final static Container CONTAINER_BROKER = Container.builder()
            .id(CONTAINER_BROKER_ID)
            .name(CONTAINER_BROKER_NAME)
            .internalName(CONTAINER_BROKER_INTERNAL_NAME)
            .ipAddress(CONTAINER_BROKER_IP)
            .build();

    public final static Long IMAGE_1_ID = 1L;
    public final static String IMAGE_1_REPOSITORY = "mariadb";
    public final static String IMAGE_1_TAG = "10.5";
    public final static String IMAGE_1_HASH = "d6a5e003eae42397f7ee4589e9f21e231d3721ac131970d2286bd616e7f55bb4\n";
    public final static String IMAGE_1_DIALECT = "org.hibernate.dialect.MariaDBDialect";
    public final static String IMAGE_1_DRIVER = "org.mariadb.jdbc.Driver";
    public final static String IMAGE_1_JDBC = "mariadb";
    public final static String IMAGE_1_LOGO = "AAAA";
    public final static Integer IMAGE_1_PORT = 3306;
    public final static Long IMAGE_1_SIZE = 12000L;
    public final static Instant IMAGE_1_CREATED = Instant.now().minus(40, HOURS);
    public final static Instant IMAGE_1_UPDATED = Instant.now().minus(39, HOURS);
    public final static List<ContainerImageEnvironmentItem> IMAGE_1_ENVIRONMENT = List.of(ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .type(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD)
                    .key("MARIADB_ROOT_PASSWORD")
                    .value("mariadb")
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .type(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME)
                    .key("UZERNAME")
                    .value("root")
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .type(ContainerImageEnvironmentItemType.USERNAME)
                    .key("MARIADB_USER")
                    .value("mariadb")
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .type(ContainerImageEnvironmentItemType.PASSWORD)
                    .key("MARIADB_PASSWORD")
                    .value("mariadb")
                    .build());

    public final static ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .repository(IMAGE_1_REPOSITORY)
            .tag(IMAGE_1_TAG)
            .hash(IMAGE_1_HASH)
            .size(IMAGE_1_SIZE)
            .environment(IMAGE_1_ENVIRONMENT)
            .dialect(IMAGE_1_DIALECT)
            .driverClass(IMAGE_1_DRIVER)
            .jdbcMethod(IMAGE_1_JDBC)
            .created(IMAGE_1_CREATED)
            .defaultPort(IMAGE_1_PORT)
            .compiled(IMAGE_1_UPDATED)
            .build();

}
