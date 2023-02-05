package at.tuwien;

import at.tuwien.api.container.image.ImageEnvItemDto;
import at.tuwien.api.container.image.ImageEnvItemTypeDto;
import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String JWT_1 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtd2Vpc2UiLCJybmQiOjk2NjIyNzAwMCwiZXhwIjoxNjczODg2MDk5LCJpYXQiOjE2NzM3OTk2OTl9.y1jqokCfZE7c_Ztt_nLQlf73jCYXPH5TZpCvo3RwS0C5azyrqLh03bphl6R8A24g6Kv_3qjzvnubNIwmO7y7pA";

    public final static GrantedAuthorityDto RESEARCHER_AUTHORITY_DTO = GrantedAuthorityDto.builder()
            .authority("ROLE_RESEARCHER")
            .build();

    public final static Long USER_1_ID = 1L;
    public final static String USER_1_USERNAME = "junit";
    public final static String USER_1_EMAIL = "junit@gmail.com";
    public final static String USER_1_AFFILIATION = "TU Wien";
    public final static Boolean USER_1_EMAIL_VERIFIED = false;
    public final static Boolean USER_1_THEME_DARK = false;
    public final static String USER_1_PASSWORD = "p455w0rdh45h";
    public final static String USER_1_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static GrantedAuthority USER_1_AUTHORITY = new SimpleGrantedAuthority("ROLE_RESEARCHER");

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(USER_1_EMAIL_VERIFIED)
            .affiliation(USER_1_AFFILIATION)
            .themeDark(USER_1_THEME_DARK)
            .password(USER_1_PASSWORD)
            .databasePassword(USER_1_DATABASE_PASSWORD)
            .roles(List.of(RoleType.ROLE_RESEARCHER))
            .build();

    public final static UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(USER_1_EMAIL_VERIFIED)
            .affiliation(USER_1_AFFILIATION)
            .themeDark(USER_1_THEME_DARK)
            .password(USER_1_PASSWORD)
            .roles(List.of("ROLE_RESEARCHER"))
            .authorities(List.of(RESEARCHER_AUTHORITY_DTO))
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
    public final static String USER_2_USERNAME = "dev";
    public final static String USER_2_EMAIL = "dev@gmail.com";
    public final static Boolean USER_2_EMAIL_VERIFIED = false;
    public final static Boolean USER_2_THEME_DARK = false;
    public final static String USER_2_PASSWORD = "p455w0rdh45";
    public final static String USER_2_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static RoleType USER_2_ROLE_TYPE = RoleType.ROLE_DEVELOPER;
    public final static GrantedAuthority USER_2_AUTHORITY = new SimpleGrantedAuthority("ROLE_DEVELOPER");

    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(USER_2_EMAIL_VERIFIED)
            .themeDark(USER_2_THEME_DARK)
            .password(USER_2_PASSWORD)
            .databasePassword(USER_2_DATABASE_PASSWORD)
            .roles(List.of(USER_2_ROLE_TYPE))
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
    public final static String USER_3_USERNAME = "steward";
    public final static String USER_3_EMAIL = "steward@gmail.com";
    public final static Boolean USER_3_EMAIL_VERIFIED = false;
    public final static Boolean USER_3_THEME_DARK = false;
    public final static String USER_3_PASSWORD = "p455w0rdh45";
    public final static String USER_3_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static RoleType USER_3_ROLE_TYPE = RoleType.ROLE_DATA_STEWARD;
    public final static GrantedAuthority USER_3_AUTHORITY = new SimpleGrantedAuthority("ROLE_DATA_STEWARD");

    public final static User USER_3 = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .emailVerified(USER_3_EMAIL_VERIFIED)
            .themeDark(USER_3_THEME_DARK)
            .password(USER_3_PASSWORD)
            .databasePassword(USER_3_DATABASE_PASSWORD)
            .roles(List.of(USER_3_ROLE_TYPE))
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

    public final static String USER_4_USERNAME = "nobody";
    public final static String USER_4_EMAIL = "nobody@gmail.com";
    public final static Boolean USER_4_EMAIL_VERIFIED = false;
    public final static Boolean USER_4_THEME_DARK = false;
    public final static String USER_4_PASSWORD = "p455w0rdh45";
    public final static String USER_4_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";

    public final static User USER_4 = User.builder()
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .emailVerified(USER_4_EMAIL_VERIFIED)
            .themeDark(USER_4_THEME_DARK)
            .password(USER_4_PASSWORD)
            .databasePassword(USER_4_DATABASE_PASSWORD)
            .roles(List.of())
            .build();

    public final static Long USER_5_ID = 5L;
    public final static String USER_5_USERNAME = "mweise";
    public final static String USER_5_EMAIL = "mweise@gmail.com";
    public final static Boolean USER_5_EMAIL_VERIFIED = false;
    public final static Boolean USER_5_THEME_DARK = false;
    public final static String USER_5_PASSWORD = "p455w0rdh45";
    public final static String USER_5_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";

    public final static UserDetailsDto USER_5_DETAILS_DTO = UserDetailsDto.builder()
            .id(USER_5_ID)
            .username(USER_5_USERNAME)
            .email(USER_5_EMAIL)
            .password(USER_5_PASSWORD)
            .authorities(List.of())
            .build();

    public final static Long IMAGE_1_ID = 1L;
    public final static String IMAGE_1_REPOSITORY = "mariadb";
    public final static String IMAGE_1_TAG = "10.5";
    public final static String IMAGE_1_HASH = "83b40f2726e5";
    public final static Integer IMAGE_1_PORT = 3306;
    public final static String IMAGE_1_DIALECT = "org.hibernate.dialect.MariaDBDialect";
    public final static String IMAGE_1_DRIVER = "org.mariadb.jdbc.Driver";
    public final static String IMAGE_1_JDBC = "mariadb";
    public final static Long IMAGE_1_SIZE = 12000L;
    public final static Instant IMAGE_1_BUILT = Instant.now().minus(38, HOURS);

    public final static List<ContainerImageEnvironmentItem> IMAGE_1_ENV = List.of(ContainerImageEnvironmentItem.builder()
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
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("MARIADB_ROOT_PASSWORD")
                    .value("mariadb")
                    .type(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD)
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("UZERNAME")
                    .value("root")
                    .type(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME)
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

    public final static ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .repository(IMAGE_1_REPOSITORY)
            .tag(IMAGE_1_TAG)
            .hash(IMAGE_1_HASH)
            .jdbcMethod(IMAGE_1_JDBC)
            .dialect(IMAGE_1_DIALECT)
            .driverClass(IMAGE_1_DRIVER)
            .compiled(IMAGE_1_BUILT)
            .size(IMAGE_1_SIZE)
            .environment(IMAGE_1_ENV)
            .defaultPort(IMAGE_1_PORT)
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

    public final static Long CONTAINER_1_ID = 1L;
    public final static String CONTAINER_1_HASH = "deadbeef";
    public final static String CONTAINER_1_NAME = "fda-userdb-u01";
    public final static String CONTAINER_1_INTERNALNAME = "dbrepo-userdb-fda-userdb-u01";
    public final static String CONTAINER_1_DATABASE = "univie";
    public final static String CONTAINER_1_IP = "172.28.0.5";
    public final static Instant CONTAINER_1_CREATED = Instant.now().minus(1, HOURS);

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(IMAGE_1)
            .hash(CONTAINER_1_HASH)
            .ipAddress(CONTAINER_1_IP)
            .created(CONTAINER_1_CREATED)
            .creator(USER_1)
            .build();

    public final static Long CONTAINER_2_ID = 2L;
    public final static String CONTAINER_2_HASH = "deadbeef";
    public final static String CONTAINER_2_NAME = "fda-userdb-u02";
    public final static String CONTAINER_2_INTERNALNAME = "dbrepo-userdb-fda-userdb-u02";
    public final static String CONTAINER_2_DATABASE = "univie";
    public final static String CONTAINER_2_IP = "172.28.0.6";
    public final static Instant CONTAINER_2_CREATED = Instant.now().minus(2, HOURS);

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(IMAGE_1)
            .hash(CONTAINER_2_HASH)
            .ipAddress(CONTAINER_2_IP)
            .created(CONTAINER_2_CREATED)
            .creator(USER_2)
            .build();

    public final static Long CONTAINER_3_ID = 3L;
    public final static String CONTAINER_3_HASH = "deadbeef";
    public final static String CONTAINER_3_NAME = "fda-userdb-u03";
    public final static String CONTAINER_3_INTERNALNAME = "dbrepo-userdb-fda-userdb-u03";
    public final static String CONTAINER_3_DATABASE = "u03";
    public final static String CONTAINER_3_IP = "173.38.0.7";
    public final static Instant CONTAINER_3_CREATED = Instant.now().minus(2, HOURS);

    public final static Container CONTAINER_3 = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .internalName(CONTAINER_3_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(IMAGE_1)
            .hash(CONTAINER_3_HASH)
            .ipAddress(CONTAINER_3_IP)
            .created(CONTAINER_3_CREATED)
            .build();

}
