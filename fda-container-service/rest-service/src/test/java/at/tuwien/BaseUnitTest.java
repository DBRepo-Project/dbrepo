package at.tuwien;

import at.tuwien.api.container.image.ImageEnvItemDto;
import at.tuwien.api.container.image.ImageEnvItemTypeDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String USER_1_USERNAME = "junit";
    public final static String USER_1_EMAIL = "junit@gmail.com";
    public final static Boolean USER_1_EMAIL_VERIFIED = false;
    public final static Boolean USER_1_THEME_DARK = false;
    public final static String USER_1_PASSWORD = "p455w0rdh45h";
    public final static RoleType USER_1_ROLE_TYPE = RoleType.ROLE_RESEARCHER;
    public final static GrantedAuthority USER_1_AUTHORITY = new SimpleGrantedAuthority("ROLE_RESEARCHER");

    public final static User USER_1 = User.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(USER_1_EMAIL_VERIFIED)
            .themeDark(USER_1_THEME_DARK)
            .password(USER_1_PASSWORD)
            .roles(List.of(USER_1_ROLE_TYPE))
            .build();

    public final static String USER_2_USERNAME = "dev";
    public final static String USER_2_EMAIL = "dev@gmail.com";
    public final static Boolean USER_2_EMAIL_VERIFIED = false;
    public final static Boolean USER_2_THEME_DARK = false;
    public final static String USER_2_PASSWORD = "p455w0rdh45";
    public final static RoleType USER_2_ROLE_TYPE = RoleType.ROLE_DEVELOPER;
    public final static GrantedAuthority USER_2_AUTHORITY = new SimpleGrantedAuthority("ROLE_DEVELOPER");

    public final static User USER_2 = User.builder()
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(USER_2_EMAIL_VERIFIED)
            .themeDark(USER_2_THEME_DARK)
            .password(USER_2_PASSWORD)
            .roles(List.of(USER_2_ROLE_TYPE))
            .build();

    public final static String USER_3_USERNAME = "steward";
    public final static String USER_3_EMAIL = "steward@gmail.com";
    public final static Boolean USER_3_EMAIL_VERIFIED = false;
    public final static Boolean USER_3_THEME_DARK = false;
    public final static String USER_3_PASSWORD = "p455w0rdh45";
    public final static RoleType USER_3_ROLE_TYPE = RoleType.ROLE_DATA_STEWARD;
    public final static GrantedAuthority USER_3_AUTHORITY = new SimpleGrantedAuthority("ROLE_DATA_STEWARD");

    public final static User USER_3 = User.builder()
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .emailVerified(USER_3_EMAIL_VERIFIED)
            .themeDark(USER_3_THEME_DARK)
            .password(USER_3_PASSWORD)
            .roles(List.of(USER_3_ROLE_TYPE))
            .build();

    public final static String USER_4_USERNAME = "nobody";
    public final static String USER_4_EMAIL = "nobody@gmail.com";
    public final static Boolean USER_4_EMAIL_VERIFIED = false;
    public final static Boolean USER_4_THEME_DARK = false;
    public final static String USER_4_PASSWORD = "p455w0rdh45";

    public final static User USER_4 = User.builder()
            .username(USER_4_USERNAME)
            .email(USER_4_EMAIL)
            .emailVerified(USER_4_EMAIL_VERIFIED)
            .themeDark(USER_4_THEME_DARK)
            .password(USER_4_PASSWORD)
            .roles(List.of())
            .build();

    public final static Long IMAGE_1_ID = 2L;
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
            .image(IMAGE_1)
            .hash(CONTAINER_1_HASH)
            .ipAddress(CONTAINER_1_IP)
            .created(CONTAINER_1_CREATED)
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
            .image(IMAGE_1)
            .hash(CONTAINER_2_HASH)
            .ipAddress(CONTAINER_2_IP)
            .created(CONTAINER_2_CREATED)
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
            .image(IMAGE_1)
            .hash(CONTAINER_3_HASH)
            .ipAddress(CONTAINER_3_IP)
            .created(CONTAINER_3_CREATED)
            .build();

}
