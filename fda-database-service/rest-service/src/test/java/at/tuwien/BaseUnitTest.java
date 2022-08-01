package at.tuwien;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyDto;
import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.License;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String BROKER_NAME = "fda-broker-service";
    public final static String BROKER_IP = "172.29.0.2";
    public final static String BROKER_HOSTNAME = "fda-broker-service";
    public final static String BROKER_IMAGE = "rabbitmq";
    public final static String BROKER_TAG = "3-alpine";

    public final static String SEARCH_NAME = "fda-search-mock-service";
    public final static String SEARCH_IP = "172.29.0.3";
    public final static String SEARCH_HOSTNAME = "fda-search-mock-service";
    public final static String SEARCH_IMAGE = "elasticsearch";
    public final static String SEARCH_TAG = "7.13.4";

    public final static Long USER_1_ID = 1L;
    public final static String USER_1_USERNAME = "junit";
    public final static String USER_1_PASSWORD = "junit";
    public final static String USER_1_EMAIL = "junit@ossdip.at";
    public final static Boolean USER_1_VERIFIED = true;
    public final static Boolean USER_1_THEME = false;

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(USER_1_VERIFIED)
            .themeDark(USER_1_THEME)
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

    public final static Long DATABASE_1_ID = 1L;
    public final static String DATABASE_1_NAME = "Weather";
    public final static String DATABASE_1_DESCRIPTION = "Weather somewhere in the world";
    public final static String DATABASE_1_PUBLISHER = "TU Wien";
    public final static Boolean DATABASE_1_PUBLIC = false;
    public final static String DATABASE_1_INTERNALNAME = "weather";
    public final static String DATABASE_1_EXCHANGE = "fda." + DATABASE_1_INTERNALNAME;
    public final static Instant DATABASE_1_CREATED = Instant.now().minus(1, HOURS);
    public final static Instant DATABASE_1_UPDATED = Instant.now();

    public final static DatabaseCreateDto DATABASE_1_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_1_NAME)
            .isPublic(DATABASE_1_PUBLIC)
            .description(DATABASE_1_DESCRIPTION)
            .build();

    public final static DatabaseModifyDto DATABASE_1_UPDATE1 = DatabaseModifyDto.builder()
            .isPublic(DATABASE_1_PUBLIC)
            .description(DATABASE_1_DESCRIPTION)
            .language(LanguageTypeDto.EN)
            .build();

    public final static DatabaseModifyDto DATABASE_1_UPDATE2 = DatabaseModifyDto.builder()
            .isPublic(DATABASE_1_PUBLIC)
            .description(DATABASE_1_DESCRIPTION)
            .language(LanguageTypeDto.EN)
            .license(LICENSE_1_DTO)
            .publication("2022-08-01")
            .publisher(DATABASE_1_PUBLISHER)
            .build();

    public final static Long DATABASE_2_ID = 2L;
    public final static String DATABASE_2_NAME = "Weather AT";
    public final static Boolean DATABASE_2_PUBLIC = false;
    public final static String DATABASE_2_INTERNALNAME = "weather_at";
    public final static String DATABASE_2_EXCHANGE = "fda." + DATABASE_2_INTERNALNAME;
    public final static Instant DATABASE_2_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant DATABASE_2_UPDATED = Instant.now();

    public final static Long CONTAINER_1_ID = 1L;
    public final static String CONTAINER_1_HASH = "deadbeef";
    public final static String CONTAINER_1_IP = "172.28.0.5";
    public final static String CONTAINER_1_NAME = "fda-userdb-u01";
    public final static String CONTAINER_1_INTERNALNAME = "fda-userdb-u01";
    public final static Instant CONTAINER_1_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant CONTAINER_1_UPDATED = Instant.now();

    public final static Long CONTAINER_2_ID = 2L;
    public final static String CONTAINER_2_HASH = "deadbeef";
    public final static String CONTAINER_2_IP = "172.28.0.6";
    public final static String CONTAINER_2_NAME = "fda-userdb-u02";
    public final static String CONTAINER_2_INTERNALNAME = "fda-userdb-u02";
    public final static Instant CONTAINER_2_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant CONTAINER_2_UPDATED = Instant.now();

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
            .logo(IMAGE_1_LOGO)
            .build();

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .hash(CONTAINER_1_HASH)
            .internalName(CONTAINER_1_INTERNALNAME)
            .created(CONTAINER_1_CREATED)
            .lastModified(CONTAINER_1_UPDATED)
            .image(IMAGE_1)
            .build();

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .hash(CONTAINER_2_HASH)
            .internalName(CONTAINER_2_INTERNALNAME)
            .created(CONTAINER_2_CREATED)
            .lastModified(CONTAINER_2_UPDATED)
            .image(IMAGE_1)
            .build();

    public final static Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNALNAME)
            .isPublic(DATABASE_1_PUBLIC)
            .container(CONTAINER_1)
            .created(DATABASE_1_CREATED)
            .tables(List.of())
            .lastModified(DATABASE_1_UPDATED)
            .container(CONTAINER_1)
            .exchange(DATABASE_1_EXCHANGE)
            .build();

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "NYSE";
    public final static String TABLE_1_INTERNALNAME = "nyse";
    public final static String TABLE_1_TOPIC = DATABASE_1_EXCHANGE + "." + TABLE_1_INTERNALNAME;

    public final static Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .name(TABLE_1_NAME)
            .internalName(TABLE_1_INTERNALNAME)
            .topic(TABLE_1_TOPIC)
            .tdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .build();

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .name(DATABASE_2_NAME)
            .internalName(DATABASE_2_INTERNALNAME)
            .isPublic(DATABASE_2_PUBLIC)
            .container(CONTAINER_2)
            .created(DATABASE_2_CREATED)
            .tables(List.of())
            .lastModified(DATABASE_2_UPDATED)
            .container(CONTAINER_2)
            .exchange(DATABASE_2_EXCHANGE)
            .build();

    public final static List<String> IMAGE_1_ENV = List.of("MARIADB_ROOT_PASSWORD=mariadb");

    public final static List<String> IMAGE_2_ENV = List.of("MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=weather_at");
}
