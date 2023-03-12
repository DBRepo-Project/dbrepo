package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.MessageQueueListener;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.*;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    /* keep */
    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    /* keep */
    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private QueryService queryService;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    private final static String BIND_WEATHER = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    private final static String BIND_ZOO = new File("./src/test/resources/zoo").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        /* create network */
        DockerConfig.createAllNetworks();
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* create network */
        DockerConfig.createAllNetworks();
        /* metadata database */
        DATABASE_1.setTables(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_7));
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_3.setDatabase(DATABASE_1);
        TABLE_7.setDatabase(DATABASE_1);
        DATABASE_2.setTables(List.of(TABLE_4, TABLE_5, TABLE_6));
        DATABASE_2.setViews(List.of(VIEW_4));
        TABLE_4.setDatabase(DATABASE_2);
        TABLE_5.setDatabase(DATABASE_2);
        TABLE_6.setDatabase(DATABASE_2);
        VIEW_4.setDatabase(DATABASE_2);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, TableNotFoundException, DatabaseConnectionException,
            PaginationException, ContainerNotFoundException, QueryMalformedException, UserNotFoundException,
            InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final QueryResultDto result = queryService.tableFindAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(),
                null, null, USER_1_PRINCIPAL);
        assertEquals(3, result.getResult().size());
        assertEquals(BigInteger.valueOf(1L), result.getResult().get(0).get(COLUMN_1_1_INTERNAL_NAME));
        assertEquals(toInstant("2008-12-01"), result.getResult().get(0).get(COLUMN_1_2_INTERNAL_NAME));
        assertEquals("Albury", result.getResult().get(0).get(COLUMN_1_3_INTERNAL_NAME));
        assertEquals(13.4, result.getResult().get(0).get(COLUMN_1_4_INTERNAL_NAME));
        assertEquals(0.6, result.getResult().get(0).get(COLUMN_1_5_INTERNAL_NAME));
        assertEquals(BigInteger.valueOf(2L), result.getResult().get(1).get(COLUMN_1_1_INTERNAL_NAME));
        assertEquals(toInstant("2008-12-02"), result.getResult().get(1).get(COLUMN_1_2_INTERNAL_NAME));
        assertEquals("Albury", result.getResult().get(1).get(COLUMN_1_3_INTERNAL_NAME));
        assertEquals(7.4, result.getResult().get(1).get(COLUMN_1_4_INTERNAL_NAME));
        assertEquals(0.0, result.getResult().get(1).get(COLUMN_1_5_INTERNAL_NAME));
        assertEquals(BigInteger.valueOf(3L), result.getResult().get(2).get(COLUMN_1_1_INTERNAL_NAME));
        assertEquals(toInstant("2008-12-03"), result.getResult().get(2).get(COLUMN_1_2_INTERNAL_NAME));
        assertEquals("Albury", result.getResult().get(2).get(COLUMN_1_3_INTERNAL_NAME));
        assertEquals(12.9, result.getResult().get(2).get(COLUMN_1_4_INTERNAL_NAME));
        assertEquals(0.0, result.getResult().get(2).get(COLUMN_1_5_INTERNAL_NAME));
    }

    @Test
    public void selectAll_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException, InterruptedException {
        final Long page = 0L;
        final Long size = 10L;

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        queryService.tableFindAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(), page, size, USER_1_PRINCIPAL);
    }

    @Test
    public void selectAll_noTable_fails() throws InterruptedException {
        final Long page = 0L;
        final Long size = 10L;

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            queryService.tableFindAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(), page, size, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_columns_fails() throws InterruptedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("key", "some_value"))
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_timestamp_succeeds() throws UserNotFoundException, TableNotFoundException, TableMalformedException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, InterruptedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("timestamp", "2023-02-10 12:15:20"))
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_7_ID))
                .thenReturn(Optional.of(TABLE_7));

        /* test */
        queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_7_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_timestampMillis_succeeds() throws UserNotFoundException, TableNotFoundException, TableMalformedException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, InterruptedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("timestamp", "2023-02-10 12:15:20.613405"))
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_7_ID))
                .thenReturn(Optional.of(TABLE_7));

        /* test */
        queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_7_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_withConstraints_succeeds() throws UserNotFoundException, TableNotFoundException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, InterruptedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-04",
                        "location", "Melbourne",
                        "mintemp", 5,
                        "rainfall", 0))
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_violatingForeignKey_fails() throws InterruptedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-04",
                        "location", "Mexico City", // not in referenced table
                        "mintemp", 5,
                        "rainfall", 0))
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_violatingUnique_fails() throws InterruptedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-03", // entry with date already exists
                        "location", "Melbourne",
                        "mintemp", 5,
                        "rainfall", 0))
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_violatingCheck_fails() throws InterruptedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-04",
                        "location", "Melbourne",
                        "mintemp", -1, // mintemp is smaller than 0, which is not allowed
                        "rainfall", 0))
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findAll_timestampMissing_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        queryService.tableFindAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, null, null, null, USER_1_PRINCIPAL);
    }

    @Test
    public void findAll_timestampBeforeCreation_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException, InterruptedException {
        final Instant timestamp = DATABASE_1_CREATED.minus(1, ChronoUnit.SECONDS);

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        queryService.tableFindAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, timestamp, null, null, USER_1_PRINCIPAL);
        queryService.tableFindAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, timestamp, null, null, USER_1_PRINCIPAL);
    }

    @Test
    public void execute_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT n.`firstname`, n.`lastname`, z.`animal_name`, z.`legs` FROM `likes` l JOIN `names` n ON l.`name_id` = n.`id` JOIN `mock_view` z ON z.`id` = l.`zoo_id`")
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_ZOO, CONTAINER_2, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_2_ID, DATABASE_2_ID))
                .thenReturn(Optional.of(DATABASE_2));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_2_ID, DATABASE_2_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(4L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals(BigInteger.valueOf(4L), result.get(0).get("legs"));
        assertEquals("boar", result.get(0).get("animal_name"));
        assertEquals("Moritz", result.get(0).get("firstname"));
        assertEquals("Staudinger", result.get(0).get("lastname"));
        assertEquals(BigInteger.valueOf(4L), result.get(1).get("legs"));
        assertEquals("cavy", result.get(1).get("animal_name"));
        assertEquals("Moritz", result.get(1).get("firstname"));
        assertEquals("Staudinger", result.get(1).get("lastname"));
        assertEquals(BigInteger.valueOf(4L), result.get(2).get("legs"));
        assertEquals("bear", result.get(2).get("animal_name"));
        assertEquals("Eva", result.get(2).get("firstname"));
        assertEquals("Gergely", result.get(2).get("lastname"));
        assertEquals(BigInteger.valueOf(4L), result.get(3).get("legs"));
        assertEquals("bear", result.get(3).get("animal_name"));
        assertEquals("Cornelia", result.get(3).get("firstname"));
        assertEquals("Michlits", result.get(3).get("lastname"));
    }

    @Test
    public void execute_withoutNullField_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `location`, `lng` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(1L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals("Melbourne", result.get(0).get("location"));
        assertNull(result.get(0).get("lat"));
        assertNull(result.get(0).get("lng"));
    }

    @Test
    public void execute_withoutNullField2_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `location` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(1L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals("Melbourne", result.get(0).get("location"));
        assertNull(result.get(0).get("lat"));
        assertNull(result.get(0).get("lng"));
    }

    @Test
    public void execute_withNullField_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `lat`, `lng` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(1L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertNull(result.get(0).get("lat"));
        assertNull(result.get(0).get("lng"));
    }

    @Test
    public void count_emptySet_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, InterruptedException, QueryNotFoundException,
            FileStorageException, SQLException {
        final Query query = Query.builder()
                .id(QUERY_1_ID)
                .query("SELECT `location`, `lat`, `lng` FROM `weather_location` WHERE `location` = \"Vienna\"")
                .queryHash(QUERY_1_QUERY_HASH)
                .resultHash(null)
                .resultNumber(0L)
                .created(QUERY_1_CREATED)
                .executed(QUERY_1_EXECUTION)
                .createdBy(USER_1_USERNAME)
                .isPersisted(true)
                .build();


        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, query, USER_1_USERNAME);

        /* test */
        final ExportResource response = queryService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
        assertNotNull(response.getFilename());
        assertNotNull(response.getResource());
    }

    @SneakyThrows
    private static Instant toInstant(String str) {
        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive() /* case insensitive to parse JAN and FEB */
                .appendPattern("yyyy-MM-dd")
                .toFormatter(Locale.ENGLISH);
        final LocalDate date = LocalDate.parse(str, formatter);
        return date.atStartOfDay(ZoneId.of("UTC"))
                .toInstant();
    }

}
