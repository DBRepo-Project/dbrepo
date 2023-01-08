package at.tuwien;

import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.database.*;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.License;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String BROKER_NAME = "fda-broker-service";
    public final static String BROKER_IP = "172.29.0.2";
    public final static String BROKER_HOSTNAME = "broker-service";
    public final static Integer BROKER_MANAGEMENT_PORT = 15672;
    public final static String BROKER_IMAGE = "rabbitmq";
    public final static String BROKER_TAG = "3-management-alpine";

    public final static String GATEWAY_NAME = "fda-gateway-service";
    public final static String GATEWAY_IP = "172.29.0.3";
    public final static String GATEWAY_HOSTNAME = "gateway-service";
    public final static Integer GATEWAY_PORT = 9095;
    public final static String GATEWAY_IMAGE = "nginx";
    public final static String GATEWAY_TAG = "alpine";

    public final static String SEARCH_NAME = "fda-search-mock-service";
    public final static String SEARCH_IP = "172.29.0.3";
    public final static String SEARCH_HOSTNAME = "fda-search-mock-service";
    public final static String SEARCH_IMAGE = "elasticsearch";
    public final static String SEARCH_TAG = "7.13.4";

    public final static Long USER_1_ID = 1L;
    public final static String USER_1_USERNAME = "junit";
    public final static String USER_1_PASSWORD = "junit";
    public final static String USER_1_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static String USER_1_EMAIL = "junit@ossdip.at";
    public final static Boolean USER_1_VERIFIED = true;
    public final static Boolean USER_1_THEME = false;

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(USER_1_VERIFIED)
            .themeDark(USER_1_THEME)
            .password(USER_1_PASSWORD)
            .databasePassword(USER_1_DATABASE_PASSWORD)
            .build();

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static Principal USER_1_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_1_DETAILS,
            USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());

    public final static Long USER_2_ID = 2L;
    public final static String USER_2_USERNAME = "junit2";
    public final static String USER_2_PASSWORD = "junit2";
    public final static String USER_2_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static String USER_2_EMAIL = "junit2@ossdip.at";
    public final static Boolean USER_2_VERIFIED = true;
    public final static Boolean USER_2_THEME = false;

    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(USER_2_VERIFIED)
            .themeDark(USER_2_THEME)
            .password(USER_2_PASSWORD)
            .databasePassword(USER_2_DATABASE_PASSWORD)
            .build();

    public final static UserDetails USER_2_DETAILS = UserDetailsDto.builder()
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static Principal USER_2_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_2_DETAILS,
            USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());

    public final static Long USER_3_ID = 2L;
    public final static String USER_3_USERNAME = "junit3";
    public final static String USER_3_PASSWORD = "junit3";
    public final static String USER_3_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static String USER_3_EMAIL = "junit3@ossdip.at";
    public final static Boolean USER_3_VERIFIED = true;
    public final static Boolean USER_3_THEME = false;

    public final static User USER_3 = User.builder()
            .id(USER_3_ID)
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .emailVerified(USER_3_VERIFIED)
            .themeDark(USER_3_THEME)
            .password(USER_3_PASSWORD)
            .databasePassword(USER_3_DATABASE_PASSWORD)
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

    public final static Long CONTAINER_3_ID = 3L;
    public final static String CONTAINER_3_HASH = "deadbeef";
    public final static String CONTAINER_3_IP = "172.28.0.7";
    public final static String CONTAINER_3_NAME = "fda-userdb-u03";
    public final static String CONTAINER_3_INTERNALNAME = "fda-userdb-u03";
    public final static Instant CONTAINER_3_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant CONTAINER_3_UPDATED = Instant.now();

    public final static Long CONTAINER_4_ID = 4L;
    public final static String CONTAINER_4_HASH = "deadbeef";
    public final static String CONTAINER_4_IP = "172.28.0.8";
    public final static String CONTAINER_4_NAME = "fda-userdb-u04";
    public final static String CONTAINER_4_INTERNALNAME = "fda-userdb-u04";
    public final static Instant CONTAINER_4_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant CONTAINER_4_UPDATED = Instant.now();

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

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .hash(CONTAINER_1_HASH)
            .internalName(CONTAINER_1_INTERNALNAME)
            .created(CONTAINER_1_CREATED)
            .lastModified(CONTAINER_1_UPDATED)
            .ipAddress(CONTAINER_1_IP)
            .imageId(IMAGE_1_ID)
            .image(IMAGE_1)
            .build();

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .hash(CONTAINER_2_HASH)
            .internalName(CONTAINER_2_INTERNALNAME)
            .created(CONTAINER_2_CREATED)
            .lastModified(CONTAINER_2_UPDATED)
            .ipAddress(CONTAINER_2_IP)
            .imageId(IMAGE_1_ID)
            .image(IMAGE_1)
            .build();

    public final static Container CONTAINER_3 = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .hash(CONTAINER_3_HASH)
            .internalName(CONTAINER_3_INTERNALNAME)
            .created(CONTAINER_3_CREATED)
            .lastModified(CONTAINER_3_UPDATED)
            .ipAddress(CONTAINER_3_IP)
            .imageId(IMAGE_1_ID)
            .image(IMAGE_1)
            .build();

    public final static Container CONTAINER_4 = Container.builder()
            .id(CONTAINER_4_ID)
            .name(CONTAINER_4_NAME)
            .hash(CONTAINER_4_HASH)
            .internalName(CONTAINER_4_INTERNALNAME)
            .created(CONTAINER_4_CREATED)
            .lastModified(CONTAINER_4_UPDATED)
            .ipAddress(CONTAINER_4_IP)
            .imageId(IMAGE_1_ID)
            .image(IMAGE_1)
            .build();

    public final static Long DATABASE_1_ID = 1L;
    public final static String DATABASE_1_NAME = "Weather";
    public final static String DATABASE_1_DESCRIPTION = "Weather somewhere in the world";
    public final static String DATABASE_1_PUBLISHER = "TU Wien";
    public final static Integer DATABASE_1_PUBLICATION_YEAR = 2022;
    public final static Boolean DATABASE_1_PUBLIC = false;
    public final static String DATABASE_1_INTERNALNAME = "weather";
    public final static String DATABASE_1_EXCHANGE = "dbrepo/" + CONTAINER_1_ID + "/" + DATABASE_1_ID;
    public final static Instant DATABASE_1_CREATED = Instant.now().minus(1, HOURS);
    public final static Instant DATABASE_1_UPDATED = Instant.now();

    public final static ExchangeDto DATABASE_EXCHANGE_1 = ExchangeDto.builder()
            .durable(false)
            .autoDelete(false)
            .type("direct")
            .internal(false)
            .vhost("/")
            .name(DATABASE_1_EXCHANGE)
            .build();

    public final static DatabaseCreateDto DATABASE_1_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_1_NAME)
            .isPublic(DATABASE_1_PUBLIC)
            .build();

    public final static Long DATABASE_2_ID = 2L;
    public final static String DATABASE_2_NAME = "Weather AT";
    public final static Boolean DATABASE_2_PUBLIC = false;
    public final static String DATABASE_2_INTERNALNAME = "weather_at";
    public final static String DATABASE_2_EXCHANGE = "fda." + DATABASE_2_INTERNALNAME;
    public final static Instant DATABASE_2_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant DATABASE_2_UPDATED = Instant.now();

    public final static DatabaseCreateDto DATABASE_2_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_2_NAME)
            .isPublic(DATABASE_2_PUBLIC)
            .build();

    public final static Long DATABASE_3_ID = 3L;
    public final static String DATABASE_3_NAME = "Weather AT";
    public final static Boolean DATABASE_3_PUBLIC = false;
    public final static String DATABASE_3_INTERNALNAME = "weather_at";
    public final static String DATABASE_3_EXCHANGE = DATABASE_3_INTERNALNAME;
    public final static Instant DATABASE_3_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant DATABASE_3_UPDATED = Instant.now();

    public final static DatabaseCreateDto DATABASE_3_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_3_NAME)
            .isPublic(DATABASE_3_PUBLIC)
            .build();

    public final static Long DATABASE_4_ID = 4L;
    public final static String DATABASE_4_NAME = "Weather AT";
    public final static Boolean DATABASE_4_PUBLIC = false;
    public final static String DATABASE_4_INTERNALNAME = "weather_at";
    public final static String DATABASE_4_EXCHANGE = DATABASE_4_INTERNALNAME;
    public final static Instant DATABASE_4_CREATED = Instant.now().minus(2, HOURS);
    public final static Instant DATABASE_4_UPDATED = Instant.now();

    public final static DatabaseCreateDto DATABASE_4_CREATE = DatabaseCreateDto.builder()
            .name(DATABASE_4_NAME)
            .isPublic(DATABASE_4_PUBLIC)
            .build();

    public final static Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNALNAME)
            .isPublic(DATABASE_1_PUBLIC)
            .container(CONTAINER_1)
            .created(DATABASE_1_CREATED)
            .creator(USER_1)
            .tables(List.of())
            .lastModified(DATABASE_1_UPDATED)
            .container(CONTAINER_1)
            .exchangeName(DATABASE_1_EXCHANGE)
            .build();

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .name(DATABASE_2_NAME)
            .internalName(DATABASE_2_INTERNALNAME)
            .isPublic(DATABASE_2_PUBLIC)
            .container(CONTAINER_2)
            .created(DATABASE_2_CREATED)
            .creator(USER_1)
            .tables(List.of())
            .lastModified(DATABASE_2_UPDATED)
            .container(CONTAINER_2)
            .exchangeName(DATABASE_2_EXCHANGE)
            .build();

    public final static Database DATABASE_3 = Database.builder()
            .id(DATABASE_3_ID)
            .name(DATABASE_3_NAME)
            .internalName(DATABASE_3_INTERNALNAME)
            .isPublic(DATABASE_3_PUBLIC)
            .container(CONTAINER_3)
            .created(DATABASE_3_CREATED)
            .creator(USER_1)
            .tables(List.of())
            .lastModified(DATABASE_3_UPDATED)
            .container(CONTAINER_3)
            .exchangeName(DATABASE_3_EXCHANGE)
            .build();

    public final static Database DATABASE_4 = Database.builder()
            .id(DATABASE_4_ID)
            .name(DATABASE_4_NAME)
            .internalName(DATABASE_4_INTERNALNAME)
            .isPublic(DATABASE_4_PUBLIC)
            .container(CONTAINER_4)
            .created(DATABASE_4_CREATED)
            .creator(USER_1)
            .tables(List.of())
            .lastModified(DATABASE_4_UPDATED)
            .container(CONTAINER_4)
            .exchangeName(DATABASE_4_EXCHANGE)
            .build();

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "NYSE";
    public final static String TABLE_1_INTERNALNAME = "nyse";
    public final static String TABLE_1_QUEUE_NAME = "dbrepo/" + CONTAINER_1_ID + "/" + DATABASE_1_ID + "/" + TABLE_1_ID;
    public final static String TABLE_1_ROUTING_KEY = "dbrepo/" + CONTAINER_1_ID + "/" + DATABASE_1_ID + "/" + TABLE_1_ID + "/1";

    public final static Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .name(TABLE_1_NAME)
            .internalName(TABLE_1_INTERNALNAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .tdbid(DATABASE_1_ID)
            .database(DATABASE_1)
            .build();

    public final static List<String> IMAGE_1_ENV = List.of("MARIADB_ROOT_PASSWORD=mariadb");

    public final static List<String> IMAGE_2_ENV = List.of("MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=weather_at");

    public final static AccessType DATABASE_1_READ_ACCESS_TYPE = AccessType.READ;

    public final static AccessTypeDto DATABASE_1_READ_ACCESS_TYPE_DTO = AccessTypeDto.READ;

    public final static AccessType DATABASE_2_WRITE_OWN_ACCESS_TYPE = AccessType.WRITE_OWN;

    public final static AccessTypeDto DATABASE_2_WRITE_OWN_ACCESS_TYPE_DTO = AccessTypeDto.WRITE_OWN;

    public final static AccessType DATABASE_3_WRITE_ALL_ACCESS_TYPE = AccessType.WRITE_ALL;

    public final static AccessTypeDto DATABASE_3_WRITE_ALL_ACCESS_TYPE_DTO = AccessTypeDto.WRITE_ALL;

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

}
