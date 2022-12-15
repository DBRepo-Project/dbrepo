package at.tuwien;

import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.TableCreateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.container.image.*;
import at.tuwien.entities.database.table.columns.concepts.Concept;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import at.tuwien.querystore.Query;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static java.time.temporal.ChronoUnit.*;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static Long USER_1_ID = 1L;
    public final static String USER_1_USERNAME = "junit";
    public final static String USER_1_EMAIL = "junit@example.com";
    public final static String USER_1_PASSWORD = "password";
    public final static String USER_1_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static Instant USER_1_CREATED = Instant.now().minus(1, HOURS);
    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(true)
            .themeDark(false)
            .password(USER_1_PASSWORD)
            .databasePassword(USER_1_DATABASE_PASSWORD)
            .roles(Collections.singletonList(RoleType.ROLE_RESEARCHER))
            .created(USER_1_CREATED)
            .lastModified(USER_1_CREATED)
            .build();
    public final static UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .emailVerified(true)
            .themeDark(false)
            .password("password")
            .build();

    public final static Long USER_2_ID = 2L;
    public final static String USER_2_USERNAME = "junit2";
    public final static String USER_2_EMAIL = "junit2@example.com";
    public final static String USER_2_PASSWORD = "password";
    public final static String USER_2_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static Instant USER_2_CREATED = Instant.now().minus(1, HOURS);
    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(true)
            .themeDark(false)
            .password(USER_2_PASSWORD)
            .databasePassword(USER_2_DATABASE_PASSWORD)
            .roles(Collections.singletonList(RoleType.ROLE_RESEARCHER))
            .created(USER_2_CREATED)
            .lastModified(USER_2_CREATED)
            .build();
    public final static UserDto USER_2_DTO = UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .emailVerified(true)
            .themeDark(false)
            .password("password")
            .build();

    public final static String DATABASE_NET = "fda-userdb";

    public final static String BROKER_IMAGE = "fda-broker-service:latest";
    public final static String BROKER_INTERNALNAME = "fda-broker-service";
    public final static String BROKER_NET = "fda-public";
    public final static String BROKER_IP = "172.29.0.2";

    public final static Long DATABASE_1_ID = 1L;
    public final static String DATABASE_1_NAME = "Weather";
    public final static String DATABASE_1_INTERNALNAME = "weather";
    public final static String DATABASE_1_EXCHANGE = "fda." + DATABASE_1_INTERNALNAME;
    public final static Instant DATABASE_1_CREATED = Instant.now().minus(2, SECONDS);

    public final static Long DATABASE_2_ID = 2L;
    public final static String DATABASE_2_NAME = "Zoo";
    public final static String DATABASE_2_INTERNALNAME = "zoo";
    public final static String DATABASE_2_EXCHANGE = "fda." + DATABASE_2_INTERNALNAME;

    public final static Long DATABASE_3_ID = 3L;
    public final static String DATABASE_3_NAME = "traffic";
    public final static String DATABASE_3_INTERNALNAME = "traffic";
    public final static String DATABASE_3_EXCHANGE = "fda." + DATABASE_3_INTERNALNAME;

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "Weather AUS";
    public final static String TABLE_1_INTERNALNAME = "weather_aus";
    public final static String TABLE_1_DESCRIPTION = "Weather in the world";
    public final static String TABLE_1_TOPIC = DATABASE_1_EXCHANGE + "." + TABLE_1_INTERNALNAME;
    public final static Instant TABLE_1_LAST_MODIFIED = Instant.now();
    public final static Long TABLE_1_SKIP_HEADERS = 1L;
    public final static String TABLE_1_NULL_ELEMENT = "NA";
    public final static Character TABLE_1_SEPARATOR = ',';
    public final static String TABLE_1_TRUE_ELEMENT = null;
    public final static String TABLE_1_FALSE_ELEMENT = null;

    public final static Long TABLE_2_ID = 2L;
    public final static String TABLE_2_NAME = "Weather Location";
    public final static String TABLE_2_INTERNALNAME = "weather_location";
    public final static String TABLE_2_DESCRIPTION = "Weather location";
    public final static String TABLE_2_TOPIC = DATABASE_2_EXCHANGE + "." + TABLE_2_INTERNALNAME;
    public final static Instant TABLE_2_LAST_MODIFIED = Instant.now();
    public final static Long TABLE_2_SKIP_HEADERS = 1L;
    public final static String TABLE_2_NULL_ELEMENT = null;
    public final static Character TABLE_2_SEPARATOR = ';';
    public final static String TABLE_2_TRUE_ELEMENT = null;
    public final static String TABLE_2_FALSE_ELEMENT = null;

    public final static Long TABLE_3_ID = 3L;
    public final static String TABLE_3_NAME = "Traffic Zürich";
    public final static String TABLE_3_INTERNALNAME = "traffic_zurich";
    public final static String TABLE_3_DESCRIPTION = "https://www.kaggle.com/laa283/zurich-public-transport/version/2";
    public final static String TABLE_3_TOPIC = DATABASE_3_EXCHANGE + "." + TABLE_3_INTERNALNAME;
    public final static Instant TABLE_3_LAST_MODIFIED = Instant.now();
    public final static Long TABLE_3_SKIP_HEADERS = 1L;
    public final static String TABLE_3_NULL_ELEMENT = null;
    public final static Character TABLE_3_SEPARATOR = ',';
    public final static String TABLE_3_TRUE_ELEMENT = null;
    public final static String TABLE_3_FALSE_ELEMENT = null;

    public final static Long TABLE_4_ID = 4L;
    public final static String TABLE_4_NAME = "zoo";
    public final static String TABLE_4_INTERNALNAME = "zoo";
    public final static String TABLE_4_DESCRIPTION = "Some Kaggle dataset";
    public final static String TABLE_4_TOPIC = DATABASE_1_EXCHANGE + "." + TABLE_4_INTERNALNAME;
    public final static Instant TABLE_4_LAST_MODIFIED = Instant.now();
    public final static Long TABLE_4_SKIP_HEADERS = 1L;
    public final static String TABLE_4_NULL_ELEMENT = null;
    public final static Character TABLE_4_SEPARATOR = '\t';
    public final static String TABLE_4_TRUE_ELEMENT = null;
    public final static String TABLE_4_FALSE_ELEMENT = null;

    public final static Long TABLE_5_ID = 5L;
    public final static String TABLE_5_NAME = "names";
    public final static String TABLE_5_INTERNALNAME = "names";
    public final static String TABLE_5_DESCRIPTION = "Some names dataset";
    public final static String TABLE_5_TOPIC = DATABASE_1_EXCHANGE + "." + TABLE_5_INTERNALNAME;
    public final static Instant TABLE_5_LAST_MODIFIED = Instant.now();
    public final static Long TABLE_5_SKIP_HEADERS = 1L;
    public final static String TABLE_5_NULL_ELEMENT = null;
    public final static Character TABLE_5_SEPARATOR = ',';
    public final static String TABLE_5_TRUE_ELEMENT = null;
    public final static String TABLE_5_FALSE_ELEMENT = null;

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
    public final static Instant IMAGE_1_BUILT = Instant.now().minus(40, HOURS);

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
    public final static ColumnTypeDto COLUMN_4_1_TYPE_DTO = ColumnTypeDto.NUMBER;
    public final static Long COLUMN_4_1_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_1_NULL = false;
    public final static Boolean COLUMN_4_1_UNIQUE = true;
    public final static Boolean COLUMN_4_1_AUTO_GENERATED = true;
    public final static String COLUMN_4_1_FOREIGN_KEY = null;
    public final static String COLUMN_4_1_REFERENCES = null;
    public final static List<String> COLUMN_4_1_ENUM_VALUES = null;
    public final static String[] COLUMN_4_1_ENUM_VALUES_ARRAY = null;

    public final static Long COLUMN_4_2_ID = 10L;
    public final static Integer COLUMN_4_2_ORDINALPOS = 1;
    public final static Boolean COLUMN_4_2_PRIMARY = false;
    public final static String COLUMN_4_2_NAME = "Animal Name";
    public final static String COLUMN_4_2_INTERNAL_NAME = "animal_name";
    public final static TableColumnType COLUMN_4_2_TYPE = TableColumnType.STRING;
    public final static ColumnTypeDto COLUMN_4_2_TYPE_DTO = ColumnTypeDto.STRING;
    public final static Long COLUMN_4_2_DATE_FORMAT = null;
    public final static Boolean COLUMN_4_2_NULL = true;
    public final static Boolean COLUMN_4_2_UNIQUE = false;
    public final static Boolean COLUMN_4_2_AUTO_GENERATED = false;
    public final static String COLUMN_4_2_FOREIGN_KEY = null;
    public final static String COLUMN_4_2_REFERENCES = null;
    public final static String COLUMN_4_2_CHECK = null;
    public final static List<String> COLUMN_4_2_ENUM_VALUES = null;
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
    public final static TableColumnType COLUMN_4_17_TYPE = TableColumnType.DECIMAL;
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
            .build();

    public final static Long CONTAINER_1_ID = 1L;
    public final static String CONTAINER_1_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_1_IMAGE = IMAGE_1;
    public final static String CONTAINER_1_NAME = "u01";
    public final static String CONTAINER_1_INTERNALNAME = "fda-userdb-u01";
    public final static String CONTAINER_1_IP = "172.28.0.5";
    public final static Instant CONTAINER_1_CREATED = Instant.now().minus(1, HOURS);

    public final static Long CONTAINER_2_ID = 2L;
    public final static String CONTAINER_2_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_2_IMAGE = IMAGE_1;
    public final static String CONTAINER_2_NAME = "u02";
    public final static String CONTAINER_2_INTERNALNAME = "fda-userdb-u02";
    public final static String CONTAINER_2_IP = "172.28.0.6";
    public final static Instant CONTAINER_2_CREATED = Instant.now().minus(1, HOURS);

    public final static Long CONTAINER_3_ID = 3L;
    public final static String CONTAINER_3_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_3_IMAGE = IMAGE_1;
    public final static String CONTAINER_3_NAME = "u03";
    public final static String CONTAINER_3_INTERNALNAME = "fda-userdb-u03";
    public final static String CONTAINER_3_IP = "172.28.0.7";
    public final static Instant CONTAINER_3_CREATED = Instant.now().minus(1, HOURS);

    public final static Long CONTAINER_NGINX_ID = 4L;
    public final static String CONTAINER_NGINX_HASH = "deadbeef";
    public final static String CONTAINER_NGINX_IMAGE = "nginx";
    public final static String CONTAINER_NGINX_TAG = "1.20-alpine";
    public final static String CONTAINER_NGINX_NET = "fda-public";
    public final static String CONTAINER_NGINX_NAME = "file-service";
    public final static String CONTAINER_NGINX_INTERNALNAME = "fda-test-file-service";
    public final static String CONTAINER_NGINX_IP = "172.29.0.3";
    public final static Instant CONTAINER_NGINX_CREATED = Instant.now().minus(3, HOURS);

    public final static Long CONCEPT_1_ID = 1L;
    public final static String CONCEPT_1_NAME = "Temperature";
    public final static Instant CONCEPT_1_CREATED = Instant.now().minus(1, HOURS);

    public final static Concept CONCEPT_1 = Concept.builder()
            .name(CONCEPT_1_NAME)
            .created(CONCEPT_1_CREATED)
            .uri("http://www.ontology-of-units-of-measure.org/resource/om-2/")
            .build();

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_1_IMAGE)
            .hash(CONTAINER_1_HASH)
            .created(CONTAINER_1_CREATED)
            .creator(USER_1)
            .build();

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_2_IMAGE)
            .hash(CONTAINER_2_HASH)
            .created(CONTAINER_2_CREATED)
            .creator(USER_2)
            .build();

    public final static Container CONTAINER_3 = Container.builder()
            .id(CONTAINER_3_ID)
            .name(CONTAINER_3_NAME)
            .internalName(CONTAINER_3_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_3_IMAGE)
            .hash(CONTAINER_3_HASH)
            .created(CONTAINER_3_CREATED)
            .build();

    public final static Long QUERY_1_ID = 1L;
    public final static String QUERY_1_STATEMENT = "SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM " +
            "`weather_aus`";
    public final static String QUERY_1_DOI = "1111/1";
    public final static Long QUERY_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long QUERY_1_DATABASE_ID = DATABASE_1_ID;
    public final static String QUERY_1_RESULT_HASH = "5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03";
    public final static Instant QUERY_1_CREATED = Instant.now();
    public final static Instant QUERY_1_EXECUTION = Instant.now();

    public final static Long QUERY_2_ID = 2L;
    public final static String QUERY_2_STATEMENT = "SELECT * FROM `weather`;";
    public final static Long QUERY_2_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long QUERY_2_DATABASE_ID = DATABASE_2_ID;
    public final static String QUERY_2_RESULT_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static Instant QUERY_2_CREATED = Instant.now().minus(2, MINUTES);
    public final static Instant QUERY_2_EXECUTION = Instant.now().minus(1, MINUTES);

    public final static Query QUERY_1 = Query.builder()
            .id(QUERY_1_ID)
            .cid(QUERY_1_CONTAINER_ID)
            .dbid(QUERY_1_DATABASE_ID)
            .query(QUERY_1_STATEMENT)
            .resultHash(QUERY_1_RESULT_HASH)
            .created(QUERY_1_CREATED)
            .execution(QUERY_1_EXECUTION)
            .createdBy(USER_1_ID)
            .build();

    public final static QueryDto QUERY_1_DTO = QueryDto.builder()
            .id(QUERY_1_ID)
            .cid(QUERY_1_CONTAINER_ID)
            .dbid(QUERY_1_DATABASE_ID)
            .query(QUERY_1_STATEMENT)
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
            .resultHash(QUERY_1_RESULT_HASH)
            .created(QUERY_1_CREATED)
            .execution(QUERY_1_EXECUTION)
            .createdBy(USER_1_ID)
            .creator(USER_1_DTO)
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

    public final static List<TableColumn> TABLE_3_COLUMNS = List.of(TableColumn.builder()
                    .tid(TABLE_3_ID)
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .cdbid(DATABASE_3_ID)
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
                    .references(COLUMN_4_1_REFERENCES)
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

    public final static Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .created(Instant.now())
            .internalName(TABLE_1_INTERNALNAME)
            .description(TABLE_1_DESCRIPTION)
            .name(TABLE_1_NAME)
            .lastModified(TABLE_1_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .topic(TABLE_1_TOPIC)
            .creator(USER_1)
            .build();

    public final static Table TABLE_2 = Table.builder()
            .id(TABLE_2_ID)
            .created(Instant.now())
            .internalName(TABLE_2_INTERNALNAME)
            .description(TABLE_2_DESCRIPTION)
            .name(TABLE_2_NAME)
            .lastModified(TABLE_2_LAST_MODIFIED)
            .tdbid(DATABASE_1_ID)
            .topic(TABLE_2_TOPIC)
            .creator(USER_2)
            .build();

    public final static Table TABLE_3 = Table.builder()
            .id(TABLE_3_ID)
            .created(Instant.now())
            .internalName(TABLE_3_INTERNALNAME)
            .description(TABLE_3_DESCRIPTION)
            .name(TABLE_3_NAME)
            .lastModified(TABLE_3_LAST_MODIFIED)
            .tdbid(DATABASE_3_ID)
            .topic(TABLE_3_TOPIC)
            .creator(USER_2)
            .build();

    public final static TableCreateDto TABLE_3_CREATE_DTO = TableCreateDto.builder()
            .name(TABLE_3_NAME)
            .description(TABLE_3_DESCRIPTION)
            .columns(List.of())
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

    public final static Table TABLE_4 = Table.builder()
            .id(TABLE_4_ID)
            .created(Instant.now())
            .internalName(TABLE_4_INTERNALNAME)
            .description(TABLE_4_DESCRIPTION)
            .name(TABLE_4_NAME)
            .lastModified(TABLE_4_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .topic(TABLE_4_TOPIC)
            .creator(USER_2)
            .build();

    public final static Table TABLE_5 = Table.builder()
            .id(TABLE_5_ID)
            .created(Instant.now())
            .internalName(TABLE_5_INTERNALNAME)
            .description(TABLE_5_DESCRIPTION)
            .name(TABLE_5_NAME)
            .lastModified(TABLE_5_LAST_MODIFIED)
            .tdbid(DATABASE_2_ID)
            .topic(TABLE_5_TOPIC)
            .creator(USER_2)
            .build();

    public final static Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(true)
            .name(DATABASE_1_NAME)
            .container(CONTAINER_1)
            .internalName(DATABASE_1_INTERNALNAME)
            .exchange(DATABASE_1_EXCHANGE)
            .creator(USER_1)
            .build();

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(false)
            .name(DATABASE_2_NAME)
            .container(CONTAINER_2)
            .internalName(DATABASE_2_INTERNALNAME)
            .exchange(DATABASE_2_EXCHANGE)
            .creator(USER_2)
            .build();

    public final static Database DATABASE_3 = Database.builder()
            .id(DATABASE_3_ID)
            .created(Instant.now().minus(1, HOURS))
            .lastModified(Instant.now())
            .isPublic(false)
            .name(DATABASE_3_NAME)
            .container(CONTAINER_3)
            .internalName(DATABASE_3_INTERNALNAME)
            .exchange(DATABASE_3_EXCHANGE)
            .creator(USER_2)
            .build();

}
