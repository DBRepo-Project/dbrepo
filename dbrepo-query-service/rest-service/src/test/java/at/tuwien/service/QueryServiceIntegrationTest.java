package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.ViewIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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


@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private ViewIdxRepository viewIdxRepository;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private QueryService queryService;

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_2);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        userRepository.saveAll(List.of(USER_1, USER_2));
        imageRepository.save(IMAGE_1);
        containerRepository.saveAll(List.of(CONTAINER_1_SIMPLE, CONTAINER_2_SIMPLE));
        databaseRepository.saveAll(List.of(DATABASE_1_SIMPLE, DATABASE_2_SIMPLE));
        tableRepository.saveAll(List.of(TABLE_1_SIMPLE, TABLE_2_SIMPLE, TABLE_3_SIMPLE, TABLE_4_SIMPLE, TABLE_5_SIMPLE, TABLE_6_SIMPLE, TABLE_7_SIMPLE));
        tableColumnRepository.saveAll(TABLE_1_COLUMNS);
        tableColumnRepository.saveAll(TABLE_2_COLUMNS);
        tableColumnRepository.saveAll(TABLE_3_COLUMNS);
        tableColumnRepository.saveAll(TABLE_4_COLUMNS);
        tableColumnRepository.saveAll(TABLE_5_COLUMNS);
        tableColumnRepository.saveAll(TABLE_6_COLUMNS);
        tableColumnRepository.saveAll(TABLE_7_COLUMNS);
        viewRepository.saveAll(List.of(VIEW_1, VIEW_2, VIEW_3, VIEW_4));
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, TableNotFoundException, DatabaseConnectionException,
            PaginationException, QueryMalformedException, UserNotFoundException {

        /* test */
        final QueryResultDto result = queryService.tableFindAll(DATABASE_1_ID, TABLE_1_ID, Instant.now(),
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
            QueryMalformedException, UserNotFoundException {
        final Long page = 0L;
        final Long size = 10L;

        /* test */
        queryService.tableFindAll(DATABASE_1_ID, TABLE_1_ID, Instant.now(), page, size, USER_1_PRINCIPAL);
    }

    @Test
    public void selectAll_noTable_fails() {
        final Long page = 0L;
        final Long size = 10L;

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            queryService.tableFindAll(DATABASE_1_ID, 9999L, Instant.now(), page, size, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_columns_fails() {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("key", "some_value"))
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_date_succeeds() throws UserNotFoundException, TableNotFoundException, TableMalformedException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(new HashMap<>() {{
                    put("id", 4L);
                    put("date", "2022-10-30");
                    put("location", "Sydney");
                    put("mintemp", 10L);
                    put("rainfall", 23.1);
                }}).build();

        /* test */
        queryService.insert(DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        final List<Map<String, String>> response = MariaDbConfig.selectQuery(DATABASE_1, "SELECT `id`, `date`, `location` FROM `weather_aus` WHERE `id` = 4", "id", "date", "location");
        final Map<String, String> row1 = response.get(0);
        assertEquals("4", row1.get("id"));
        assertEquals("2022-10-30", row1.get("date"));
        assertEquals("Sydney", row1.get("location"));
    }

    @Test
    public void insert_timestamp_succeeds() throws UserNotFoundException, TableNotFoundException, TableMalformedException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(new HashMap<>() {{
                    put("timestamp", "2023-02-10 12:15:20");
                    put("value", 12.3);
                }}).build();

        /* test */
        queryService.insert(DATABASE_1_ID, TABLE_7_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_timestampMillis_succeeds() throws UserNotFoundException, TableNotFoundException, TableMalformedException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(new HashMap<>() {{
                    put("timestamp", "2023-02-10 12:15:20.613405");
                    put("value", null);
                }}).build();

        /* test */
        queryService.insert(DATABASE_1_ID, TABLE_7_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_withConstraints_succeeds() throws UserNotFoundException, TableNotFoundException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-04",
                        "location", "Albury" /* the constraint -> weather_location (location) */,
                        "mintemp", 5,
                        "rainfall", 0))
                .build();

        /* test */
        queryService.insert(DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_violatingForeignKey_fails() {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-04",
                        "location", "Mexico City", // not in referenced table
                        "mintemp", 5,
                        "rainfall", 0))
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_violatingUnique_fails() {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-03", // entry with date already exists
                        "location", "Melbourne",
                        "mintemp", 5,
                        "rainfall", 0))
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_violatingCheck_fails() {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("id", 4L,
                        "date", "2008-12-04",
                        "location", "Melbourne",
                        "mintemp", -1, // mintemp is smaller than 0, which is not allowed
                        "rainfall", 0))
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findAll_timestampMissing_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            QueryMalformedException, UserNotFoundException {

        /* test */
        queryService.tableFindAll(DATABASE_1_ID, TABLE_1_ID, null, null, null, USER_1_PRINCIPAL);
    }

    @Test
    public void findAll_timestampBeforeCreation_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            QueryMalformedException, UserNotFoundException {
        final Instant timestamp = DATABASE_1_CREATED.minus(1, ChronoUnit.SECONDS);

        /* test */
        queryService.tableFindAll(DATABASE_1_ID, TABLE_1_ID, timestamp, null, null, USER_1_PRINCIPAL);
        queryService.tableFindAll(DATABASE_1_ID, TABLE_1_ID, timestamp, null, null, USER_1_PRINCIPAL);
    }

    @Test
    public void execute_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, UserNotFoundException,
            QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT n.`firstname`, n.`lastname`, n.`birth`, n.`reminder`, z.`animal_name`, z.`legs` FROM `likes` l JOIN `names` n ON l.`name_id` = n.`id` JOIN `mock_view` z ON z.`id` = l.`zoo_id`")
                .build();

        /* test */
        Thread.sleep(1000) /* wait for test container some more */;
        final QueryResultDto response = queryService.execute(DATABASE_2_ID, request, USER_1_PRINCIPAL, 0L, 100L, null, null);
        assertEquals(4L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals(4, result.get(0).get("legs"));
        assertEquals("boar", result.get(0).get("animal_name"));
        assertEquals("Moritz", result.get(0).get("firstname"));
        assertEquals("Staudinger", result.get(0).get("lastname"));
        assertEquals("1990", result.get(0).get("birth"));
        assertEquals("11:22:33", result.get(0).get("reminder"));
        assertEquals(4, result.get(1).get("legs"));
        assertEquals("cavy", result.get(1).get("animal_name"));
        assertEquals("Moritz", result.get(1).get("firstname"));
        assertEquals("Staudinger", result.get(1).get("lastname"));
        assertEquals("1990", result.get(1).get("birth"));
        assertEquals("11:22:33", result.get(1).get("reminder"));
        assertEquals(4, result.get(2).get("legs"));
        assertEquals("bear", result.get(2).get("animal_name"));
        assertEquals("Eva", result.get(2).get("firstname"));
        assertEquals("Gergely", result.get(2).get("lastname"));
        assertEquals(4, result.get(3).get("legs"));
        assertEquals("bear", result.get(3).get("animal_name"));
        assertEquals("Cornelia", result.get(3).get("firstname"));
        assertEquals("Michlits", result.get(3).get("lastname"));
    }

    @Test
    public void execute_withoutNullField_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `location`, `lng` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* test */
        Thread.sleep(1000) /* wait for test container some more */;
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(1L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals("Vienna", result.get(0).get("location"));
        assertNull(result.get(0).get("lat"));
        assertNull(result.get(0).get("lng"));
    }

    @Test
    public void execute_withoutNullField2_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `location` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* test */
        Thread.sleep(1000) /* wait for test container some more */;
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(1L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals("Vienna", result.get(0).get("location"));
        assertNull(result.get(0).get("lat"));
        assertNull(result.get(0).get("lng"));
    }

    @Test
    public void execute_withNullField_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `lat`, `lng` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* test */
        Thread.sleep(1000) /* wait for test container some more */;
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(1L, response.getResultNumber());
        assertNotNull(response.getResult());
    }

    @Test
    public void execute_aliases_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT aus.location as a, loc.location from weather_aus aus, weather_location loc")
                .build();

        /* test */
        Thread.sleep(1000) /* wait for test container some more */;
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 100L, null, null);
        assertEquals(9L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals("Albury", result.get(0).get("a"));
        assertEquals("Albury", result.get(0).get("location"));
        assertEquals("Albury", result.get(1).get("a"));
        assertEquals("Albury", result.get(1).get("location"));
        assertEquals("Albury", result.get(2).get("a"));
        assertEquals("Albury", result.get(2).get("location"));
        assertEquals("Albury", result.get(3).get("a"));
        assertEquals("Sydney", result.get(3).get("location"));
        assertEquals("Albury", result.get(4).get("a"));
        assertEquals("Sydney", result.get(4).get("location"));
        assertEquals("Albury", result.get(5).get("a"));
        assertEquals("Sydney", result.get(5).get("location"));
        assertEquals("Albury", result.get(6).get("a"));
        assertEquals("Vienna", result.get(6).get("location"));
        assertEquals("Albury", result.get(7).get("a"));
        assertEquals("Vienna", result.get(7).get("location"));
        assertEquals("Albury", result.get(8).get("a"));
        assertEquals("Vienna", result.get(8).get("location"));
    }

    @Test
    public void execute_aliasesWithDatabaseName_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException, InterruptedException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT aus.location as a, loc.location from weather.weather_aus aus, weather.weather_location loc")
                .build();

        /* mock */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(9L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals("Albury", result.get(0).get("a"));
        assertEquals("Albury", result.get(0).get("location"));
        assertEquals("Albury", result.get(1).get("a"));
        assertEquals("Albury", result.get(1).get("location"));
        assertEquals("Albury", result.get(2).get("a"));
        assertEquals("Albury", result.get(2).get("location"));
        assertEquals("Albury", result.get(3).get("a"));
        assertEquals("Sydney", result.get(3).get("location"));
        assertEquals("Albury", result.get(4).get("a"));
        assertEquals("Sydney", result.get(4).get("location"));
        assertEquals("Albury", result.get(5).get("a"));
        assertEquals("Sydney", result.get(5).get("location"));
        assertEquals("Albury", result.get(6).get("a"));
        assertEquals("Vienna", result.get(6).get("location"));
        assertEquals("Albury", result.get(7).get("a"));
        assertEquals("Vienna", result.get(7).get("location"));
        assertEquals("Albury", result.get(8).get("a"));
        assertEquals("Vienna", result.get(8).get("location"));
    }

    @Test
    public void count_emptySet_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, UserNotFoundException,
            QueryStoreException, QueryNotFoundException, FileStorageException, SQLException {
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
        MariaDbConfig.insertQueryStore(DATABASE_1, query, USER_1_USERNAME);

        /* test */
        final ExportResource response = queryService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
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
