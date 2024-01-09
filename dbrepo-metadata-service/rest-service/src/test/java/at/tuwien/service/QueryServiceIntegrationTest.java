package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.config.S3Config;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDbSidecarGateway;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.*;
import at.tuwien.service.impl.QueryServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class QueryServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private QueryServiceImpl queryService;

    @Autowired
    private S3Config s3Config;

    @Autowired
    private LicenseRepository licenseRepository;

    @MockBean
    private DataDbSidecarGateway dataDbSidecarGateway;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Container
    private static MinIOContainer minIOContainer = new MinIOContainer("minio/minio")
            .withUserName("seaweedfsadmin")
            .withPassword("seaweedfsadmin");

    @DynamicPropertySource
    static void openSearchProperties(DynamicPropertyRegistry registry) {
        registry.add("fda.s3.endpoint", () -> minIOContainer.getS3URL());
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        DATABASE_1.setAccesses(List.of());
        DATABASE_2.setAccesses(List.of());
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2));
        /* mock */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_2);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, TableNotFoundException, DatabaseConnectionException,
            PaginationException, QueryMalformedException, UserNotFoundException {

        /* test */
        final QueryResultDto result = queryService.tableFindAll(DATABASE_1_ID, TABLE_1_ID, Instant.now(),
                null, null, USER_1_PRINCIPAL);
        assertEquals(3, result.getResult().size());
        assertEquals(BigInteger.valueOf(1L), result.getResult().get(0).get(TABLE_1_COLUMNS.get(0).getInternalName()));
        assertEquals(toInstant("2008-12-01"), result.getResult().get(0).get(TABLE_1_COLUMNS.get(1).getInternalName()));
        assertEquals("Albury", result.getResult().get(0).get(TABLE_1_COLUMNS.get(2).getInternalName()));
        assertEquals(13.4, result.getResult().get(0).get(TABLE_1_COLUMNS.get(3).getInternalName()));
        assertEquals(0.6, result.getResult().get(0).get(TABLE_1_COLUMNS.get(4).getInternalName()));
        assertEquals(BigInteger.valueOf(2L), result.getResult().get(1).get(TABLE_1_COLUMNS.get(0).getInternalName()));
        assertEquals(toInstant("2008-12-02"), result.getResult().get(1).get(TABLE_1_COLUMNS.get(1).getInternalName()));
        assertEquals("Albury", result.getResult().get(1).get(TABLE_1_COLUMNS.get(2).getInternalName()));
        assertEquals(7.4, result.getResult().get(1).get(TABLE_1_COLUMNS.get(3).getInternalName()));
        assertEquals(0.0, result.getResult().get(1).get(TABLE_1_COLUMNS.get(4).getInternalName()));
        assertEquals(BigInteger.valueOf(3L), result.getResult().get(2).get(TABLE_1_COLUMNS.get(0).getInternalName()));
        assertEquals(toInstant("2008-12-03"), result.getResult().get(2).get(TABLE_1_COLUMNS.get(1).getInternalName()));
        assertEquals("Albury", result.getResult().get(2).get(TABLE_1_COLUMNS.get(2).getInternalName()));
        assertEquals(12.9, result.getResult().get(2).get(TABLE_1_COLUMNS.get(3).getInternalName()));
        assertEquals(0.0, result.getResult().get(2).get(TABLE_1_COLUMNS.get(4).getInternalName()));
    }

    @Test
    public void selectAll_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, TableMalformedException, QueryMalformedException {
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
    public void insert_date_succeeds() throws TableNotFoundException, TableMalformedException,
            DatabaseNotFoundException, SQLException {
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
    public void insert_timestamp_succeeds() throws TableNotFoundException, TableMalformedException,
            DatabaseNotFoundException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(new HashMap<>() {{
                    put("timestamp", "2023-02-10 12:15:20");
                    put("value", 12.3);
                }}).build();

        /* test */
        queryService.insert(DATABASE_1_ID, TABLE_4_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_timestampMillis_succeeds() throws TableNotFoundException, TableMalformedException,
            DatabaseNotFoundException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(new HashMap<>() {{
                    put("timestamp", "2023-02-10 12:15:20.613405");
                    put("value", null);
                }}).build();

        /* test */
        queryService.insert(DATABASE_1_ID, TABLE_4_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_withConstraints_succeeds() throws TableNotFoundException, TableMalformedException,
            DatabaseNotFoundException {
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
            QueryStoreException, ColumnParseException, InterruptedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT n.`firstname`, n.`lastname`, n.`birth`, n.`reminder`, z.`animal_name`, z.`legs` FROM `likes` l JOIN `names` n ON l.`name_id` = n.`id` JOIN `mock_view` z ON z.`id` = l.`zoo_id` ORDER BY animal_name ASC")
                .build();

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
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
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, UserNotFoundException,
            QueryStoreException, ColumnParseException, InterruptedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `location`, `lng` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
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
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, UserNotFoundException,
            QueryStoreException, ColumnParseException, InterruptedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `location` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
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
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, UserNotFoundException,
            QueryStoreException, ColumnParseException, InterruptedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `lat`, `lng` FROM `weather_location` WHERE `lat` IS NULL")
                .build();

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL,
                0L, 100L, null, null);
        assertEquals(1L, response.getResultNumber());
        assertNotNull(response.getResult());
    }

    @Test
    public void execute_aliases_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, UserNotFoundException,
            QueryStoreException, ColumnParseException, InterruptedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT aus.location as a, loc.location from weather_aus aus, weather_location loc")
                .build();

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
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
            DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException, UserNotFoundException,
            QueryStoreException, ColumnParseException, InterruptedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT aus.location as a, loc.location from weather.weather_aus aus, weather.weather_location loc")
                .build();

        /* pre-condition */
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
    public void viewFindAll_succeeds() throws TableMalformedException, DatabaseNotFoundException,
            QueryMalformedException, InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        final QueryResultDto response = queryService.viewFindAll(DATABASE_1_ID, VIEW_2, 0L, 10L, USER_1_PRINCIPAL);
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        /* ordering */
        final String[] keys = result.get(0).keySet().toArray(new String[0]);
        assertEquals("date", keys[0]);
        assertEquals("rainfall", keys[1]);
        assertEquals("location", keys[2]);
        assertEquals("mintemp", keys[3]);
        /* values */
        assertEquals(0.6, result.get(0).get("rainfall"));
        assertEquals("Albury", result.get(0).get("location"));
        assertEquals(13.4, result.get(0).get("mintemp"));
        assertEquals(0.0, result.get(1).get("rainfall"));
        assertEquals("Albury", result.get(1).get("location"));
        assertEquals(7.4, result.get(1).get("mintemp"));
        assertEquals(0.0, result.get(2).get("rainfall"));
        assertEquals("Albury", result.get(2).get("location"));
        assertEquals(12.9, result.get(2).get("mintemp"));
    }

    @Test
    public void findOne_emptySet_succeeds() throws DatabaseConnectionException, DatabaseNotFoundException,
            ImageNotSupportedException, QueryMalformedException, UserNotFoundException, QueryStoreException,
            QueryNotFoundException, FileStorageException, SQLException, IOException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
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
        doNothing()
                .when(dataDbSidecarGateway)
                .exportFile(anyString(), anyInt(), anyString());
        s3Config.makeBuckets("dbrepo-upload", "dbrepo-download");
        s3Config.uploadFile("dbrepo-download", "./src/test/resources/csv/testdata.csv", filename);

        /* test */
        final ExportResource response = queryService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, filename);
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
