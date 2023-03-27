package at.tuwien;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.identifier.*;
import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.*;
import at.tuwien.entities.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String JWT_1 = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtd2Vpc2UiLCJybmQiOjk2NjIyNzAwMCwiZXhwIjoxNjczODg2MDk5LCJpYXQiOjE2NzM3OTk2OTl9.y1jqokCfZE7c_Ztt_nLQlf73jCYXPH5TZpCvo3RwS0C5azyrqLh03bphl6R8A24g6Kv_3qjzvnubNIwmO7y7pA";

    public final static String USER_1_ID = "090dc12a-a46a-4515-b1f0-cff697d5f985";
    public final static String USER_1_USERNAME = "junit";
    public final static String USER_1_PASSWORD = "junit";
    public final static String USER_1_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static String USER_1_EMAIL = "junit@example.com";
    public final static Boolean USER_1_EMAIL_VERIFIED = true;
    public final static Boolean USER_1_THEME_DARK = false;
    public final static Instant USER_1_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
    public final static Instant USER_1_LAST_MODIFIED = USER_1_CREATED;

    public final static GrantedAuthorityDto AUTHORITY_RESEARCHER_DTO = GrantedAuthorityDto.builder()
            .authority("ROLE_RESEARCHER")
            .build();

    public final static GrantedAuthorityDto AUTHORITY_DEVELOPER_DTO = GrantedAuthorityDto.builder()
            .authority("ROLE_DEVELOPER")
            .build();

    public final static GrantedAuthorityDto AUTHORITY_DATA_STEWARD_DTO = GrantedAuthorityDto.builder()
            .authority("ROLE_DATA_STEWARD")
            .build();

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .databasePassword(USER_1_DATABASE_PASSWORD)
            .email(USER_1_EMAIL)
            .emailVerified(USER_1_EMAIL_VERIFIED)
            .themeDark(USER_1_THEME_DARK)
            .created(USER_1_CREATED)
            .lastModified(USER_1_LAST_MODIFIED)
            .build();

    public final static UserDto USER_1_DTO = UserDto.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .password(USER_1_PASSWORD)
            .email(USER_1_EMAIL)
            .authorities(List.of(AUTHORITY_RESEARCHER_DTO))
            .roles(List.of("ROLE_RESEARCHER"))
            .emailVerified(USER_1_EMAIL_VERIFIED)
            .themeDark(USER_1_THEME_DARK)
            .build();

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static Principal USER_1_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_1_DETAILS,
            USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());

    public final static String USER_2_ID = "0153f998-bd4c-4154-993e-75c355499044";
    public final static String USER_2_USERNAME = "junit2";
    public final static String USER_2_PASSWORD = "junit2";
    public final static String USER_2_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static String USER_2_EMAIL = "junit2@example.com";
    public final static Boolean USER_2_EMAIL_VERIFIED = true;
    public final static Boolean USER_2_THEME_DARK = false;
    public final static Instant USER_2_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
    public final static Instant USER_2_LAST_MODIFIED = USER_2_CREATED;

    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .password(USER_2_PASSWORD)
            .databasePassword(USER_2_DATABASE_PASSWORD)
            .email(USER_2_EMAIL)
            .emailVerified(USER_2_EMAIL_VERIFIED)
            .themeDark(USER_2_THEME_DARK)
            .created(USER_2_CREATED)
            .lastModified(USER_2_LAST_MODIFIED)
            .build();

    public final static UserDto USER_2_DTO = UserDto.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .password(USER_2_PASSWORD)
            .email(USER_2_EMAIL)
            .authorities(List.of(AUTHORITY_RESEARCHER_DTO))
            .roles(List.of("ROLE_RESEARCHER"))
            .emailVerified(USER_2_EMAIL_VERIFIED)
            .themeDark(USER_2_THEME_DARK)
            .build();

    public final static UserDetails USER_2_DETAILS = UserDetailsDto.builder()
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static Principal USER_2_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_2_DETAILS,
            USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());

    public final static String USER_3_ID = "fea123c7-1851-4e01-969a-53407fa6a451";
    public final static String USER_3_USERNAME = "steward";
    public final static String USER_3_EMAIL = "steward@gmail.com";
    public final static Boolean USER_3_EMAIL_VERIFIED = false;
    public final static Boolean USER_3_THEME_DARK = false;
    public final static String USER_3_PASSWORD = "p455w0rdh45";
    public final static String USER_3_DATABASE_PASSWORD = "*A8C67ABBEAE837AABCF49680A157D85D44A117E9";
    public final static Instant USER_3_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
    public final static Instant USER_3_LAST_MODIFIED = USER_3_CREATED;
    public final static GrantedAuthority USER_3_AUTHORITY = new SimpleGrantedAuthority("ROLE_DATA_STEWARD");

    public final static User USER_3 = User.builder()
            .username(USER_3_USERNAME)
            .password(USER_3_PASSWORD)
            .databasePassword(USER_3_DATABASE_PASSWORD)
            .email(USER_3_EMAIL)
            .emailVerified(USER_3_EMAIL_VERIFIED)
            .themeDark(USER_3_THEME_DARK)
            .created(USER_3_CREATED)
            .lastModified(USER_3_LAST_MODIFIED)
            .build();

    public final static UserDetails USER_3_DETAILS = UserDetailsDto.builder()
            .username(USER_3_USERNAME)
            .email(USER_3_EMAIL)
            .password(USER_3_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_DATA_STEWARD")))
            .build();

    public final static Principal USER_3_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_3_DETAILS,
            USER_3_PASSWORD, USER_3_DETAILS.getAuthorities());

    public final static Long IMAGE_1_ID = 1L;
    public final static String IMAGE_1_REPOSITORY = "postgres";
    public final static String IMAGE_1_TAG = "13-alpine";
    public final static String IMAGE_1_HASH = "83b40f2726e5";
    public final static Integer IMAGE_1_PORT = 5432;
    public final static String IMAGE_1_DIALECT = "org.mariadb.jdbc.Driver";
    public final static String IMAGE_1_DRIVER = "org.postgresql.Driver";
    public final static String IMAGE_1_JDBC = "postgresql";
    public final static Long IMAGE_1_SIZE = 12000L;
    public final static String IMAGE_1_LOGO = "AAAA";
    public final static Instant IMAGE_1_BUILT = Instant.ofEpochSecond(1441588352);

    public final static List<ContainerImageEnvironmentItem> IMAGE_1_ENV = List.of(
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("POSTGRES_USER")
                    .value("postgres")
                    .type(ContainerImageEnvironmentItemType.USERNAME)
                    .build(),
            ContainerImageEnvironmentItem.builder()
                    .iid(IMAGE_1_ID)
                    .key("POSTGRES_PASSWORD")
                    .value("postgres")
                    .type(ContainerImageEnvironmentItemType.PASSWORD)
                    .build());

    public final static ContainerImage IMAGE_1 = ContainerImage.builder()
            .id(IMAGE_1_ID)
            .repository(IMAGE_1_REPOSITORY)
            .tag(IMAGE_1_TAG)
            .hash(IMAGE_1_HASH)
            .jdbcMethod(IMAGE_1_JDBC)
            .dialect(IMAGE_1_DIALECT)
            .driverClass(IMAGE_1_DRIVER)
            .containers(List.of())
            .compiled(IMAGE_1_BUILT)
            .size(IMAGE_1_SIZE)
            .environment(IMAGE_1_ENV)
            .defaultPort(IMAGE_1_PORT)
            .build();

    public final static Long CONTAINER_1_ID = 1L;
    public final static String CONTAINER_1_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_1_IMAGE = IMAGE_1;
    public final static String CONTAINER_1_NAME = "fda-userdb-u01";
    public final static String CONTAINER_1_INTERNALNAME = "fda-userdb-u01";
    public final static String CONTAINER_1_DATABASE = "univie";
    public final static String CONTAINER_1_IP = "172.28.0.5";
    public final static Instant CONTAINER_1_CREATED = Instant.ofEpochSecond(1641588352);

    public final static Container CONTAINER_1 = Container.builder()
            .id(CONTAINER_1_ID)
            .name(CONTAINER_1_NAME)
            .internalName(CONTAINER_1_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_1_IMAGE)
            .hash(CONTAINER_1_HASH)
            .build();

    public final static Long CONTAINER_2_ID = 2L;
    public final static String CONTAINER_2_HASH = "deadbeef";
    public final static ContainerImage CONTAINER_2_IMAGE = IMAGE_1;
    public final static String CONTAINER_2_NAME = "fda-userdb-u02";
    public final static String CONTAINER_2_INTERNALNAME = "fda-userdb-u02";
    public final static String CONTAINER_2_DATABASE = "univie";
    public final static String CONTAINER_2_IP = "172.28.0.6";
    public final static Instant CONTAINER_2_CREATED = Instant.ofEpochSecond(1641588352);

    public final static Container CONTAINER_2 = Container.builder()
            .id(CONTAINER_2_ID)
            .name(CONTAINER_2_NAME)
            .internalName(CONTAINER_2_INTERNALNAME)
            .imageId(IMAGE_1_ID)
            .image(CONTAINER_2_IMAGE)
            .hash(CONTAINER_2_HASH)
            .build();

    public final static Long CONTAINER_3_ID = 3L;

    public final static Long CONTAINER_4_ID = 4L;

    public final static Long DATABASE_1_ID = 1L;
    public final static String DATABASE_1_NAME = "Test Database";
    public final static String DATABASE_1_INTERNAL_NAME = "test_database";
    public final static String DATABASE_1_EXCHANGE = "fda." + DATABASE_1_INTERNAL_NAME;
    public final static Boolean DATABASE_1_PUBLIC = true;
    public final static User DATABASE_1_CREATOR = USER_1;

    public final static Long DATABASE_2_ID = 2L;
    public final static String DATABASE_2_NAME = "Test Database 2";
    public final static String DATABASE_2_INTERNAL_NAME = "test_database_2";
    public final static String DATABASE_2_EXCHANGE = "fda." + DATABASE_2_INTERNAL_NAME;
    public final static Boolean DATABASE_2_PUBLIC = false;
    public final static User DATABASE_2_CREATOR = USER_2;

    public final static Long DATABASE_3_ID = 3L;

    public final static Long DATABASE_4_ID = 4L;

    public final static Long TABLE_1_ID = 1L;
    public final static String TABLE_1_NAME = "Rainfall";
    public final static String TABLE_1_INTERNAL_NAME = "rainfall";
    public final static String TABLE_1_QUEUE_NAME = "dbrepo/" + CONTAINER_1_ID + "/" + DATABASE_1_ID + "/" + TABLE_1_ID;
    public final static String TABLE_1_ROUTING_KEY = TABLE_1_QUEUE_NAME + "/1";

    public final static Database DATABASE_1 = Database.builder()
            .id(DATABASE_1_ID)
            .name(DATABASE_1_NAME)
            .internalName(DATABASE_1_INTERNAL_NAME)
            .exchangeName(DATABASE_1_EXCHANGE)
            .tables(List.of())
            .isPublic(DATABASE_1_PUBLIC)
            .creator(DATABASE_1_CREATOR)
            .build();

    public final static Database DATABASE_2 = Database.builder()
            .id(DATABASE_2_ID)
            .name(DATABASE_2_NAME)
            .internalName(DATABASE_2_INTERNAL_NAME)
            .exchangeName(DATABASE_2_EXCHANGE)
            .tables(List.of())
            .isPublic(DATABASE_2_PUBLIC)
            .creator(DATABASE_2_CREATOR)
            .build();

    public final static Table TABLE_1 = Table.builder()
            .id(TABLE_1_ID)
            .name(TABLE_1_NAME)
            .internalName(TABLE_1_INTERNAL_NAME)
            .queueName(TABLE_1_QUEUE_NAME)
            .routingKey(TABLE_1_ROUTING_KEY)
            .tdbid(DATABASE_1_ID)
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

    public final static Long QUERY_1_ID = 1L;
    public final static Long QUERY_1_CONTAINER_ID = CONTAINER_1_ID;
    public final static Long QUERY_1_DATABASE_ID = DATABASE_1_ID;
    public final static String QUERY_1_STATEMENT = "SELECT * FROM `weather`;";
    public final static String QUERY_1_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static String QUERY_1_RESULT_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static Long QUERY_1_RESULT_NUMBER = 9L;
    public final static Instant QUERY_1_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant QUERY_1_EXECUTED = Instant.ofEpochSecond(1641588352);
    public final static Instant QUERY_1_LAST_MODIFIED = Instant.ofEpochSecond(1541588352);

    public final static QueryDto QUERY_1_DTO = QueryDto.builder()
            .id(QUERY_1_ID)
            .cid(QUERY_1_CONTAINER_ID)
            .dbid(QUERY_1_DATABASE_ID)
            .query(QUERY_1_STATEMENT)
            .queryNormalized(QUERY_1_STATEMENT)
            .resultNumber(QUERY_1_RESULT_NUMBER)
            .resultHash(QUERY_1_RESULT_HASH)
            .lastModified(QUERY_1_LAST_MODIFIED)
            .created(QUERY_1_CREATED)
            .queryHash(QUERY_1_HASH)
            .execution(QUERY_1_EXECUTED)
            .build();

    public final static Long QUERY_2_ID = 2L;
    public final static Long QUERY_2_CONTAINER_ID = CONTAINER_2_ID;
    public final static Long QUERY_2_DATABASE_ID = DATABASE_2_ID;
    public final static String QUERY_2_STATEMENT = "SELECT * FROM `weather`;";
    public final static String QUERY_2_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static String QUERY_2_RESULT_HASH = "ff3f7cbe1b96d296957f6e39e55b8b1b577fa3d205d4795af99594cfd20cb80d";
    public final static Long QUERY_2_RESULT_NUMBER = 5L;
    public final static Instant QUERY_2_CREATED = Instant.ofEpochSecond(1641588352);
    public final static Instant QUERY_2_EXECUTED = Instant.ofEpochSecond(1641588352);
    public final static Instant QUERY_2_LAST_MODIFIED = Instant.ofEpochSecond(1541588352);

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
            .queryHash(QUERY_2_HASH)
            .execution(QUERY_2_EXECUTED)
            .build();

    public final static Long QUERY_3_ID = 3L;

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
    public final static String IDENTIFIER_3_QUERY_HASH = "abc";
    public final static String IDENTIFIER_3_RESULT_HASH = "def";
    public final static String IDENTIFIER_3_QUERY = "SELECT `id` FROM `foobar`";
    public final static String IDENTIFIER_3_NORMALIZED = "SELECT `id` FROM `foobar`";
    public final static Long IDENTIFIER_3_RESULT_NUMBER = 2L;
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

    public final static String COLUMN_1_INTERNAL_NAME = "id";
    public final static String COLUMN_2_INTERNAL_NAME = "name";

    public final static Map<String, Object> ROW_1 = new LinkedHashMap<>() {{
        put(COLUMN_1_INTERNAL_NAME, 1L);
        put(COLUMN_2_INTERNAL_NAME, "Foo");
    }};
    public final static Map<String, Object> ROW_2 = new LinkedHashMap<>() {{
        put(COLUMN_1_INTERNAL_NAME, 2L);
        put(COLUMN_2_INTERNAL_NAME, "Bar");
    }};
    public final static Map<String, Object> ROW_3 = new LinkedHashMap<>() {{
        put(COLUMN_1_INTERNAL_NAME, 3L);
        put(COLUMN_2_INTERNAL_NAME, "Baz");
    }};

    public final static QueryResultDto QUERY_1_RESULT = QueryResultDto.builder()
            .id(QUERY_1_ID)
            .result(List.of(ROW_1, ROW_2, ROW_3))
            .build();

}
