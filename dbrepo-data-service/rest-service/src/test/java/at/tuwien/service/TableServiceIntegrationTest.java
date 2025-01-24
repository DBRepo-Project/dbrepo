package at.tuwien.service;

import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.config.S3Config;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.test.AbstractUnitTest;
import com.google.common.io.Files;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class TableServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private TableService tableService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Config s3Config;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Container
    private static final MinIOContainer minIOContainer = new MinIOContainer(MINIO_IMAGE);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.endpoints.storageService", minIOContainer::getS3URL);
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_3_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_3_DTO);
        /* s3 */
        if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(s3Config.getS3Bucket()))) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(s3Config.getS3Bucket())
                    .build());
        }
        if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(s3Config.getS3Bucket()))) {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(s3Config.getS3Bucket())
                    .build());
        }
    }

    @Test
    public void updateTuple_succeeds() throws SQLException, RemoteUnavailableException, ContainerNotFoundException,
            TableNotFoundException, TableMalformedException, QueryMalformedException, MetadataServiceException {
        /* modify row based on primary key */
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .data(new HashMap<>() {{
                    put("date", "2023-10-03");
                    put("location", "Vienna");
                    put("mintemp", 15.0);
                    put("rainfall", 0.2);
                }})
                .keys(new HashMap<>() {{
                    put("id", 1L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_modifyPrimaryKey_succeeds() throws SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            MetadataServiceException {
        /* modify row primary key based on primary key */
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .data(new HashMap<>() {{
                    put("id", 4L);
                    put("date", "2023-10-03");
                    put("location", "Vienna");
                    put("mintemp", 15.0);
                    put("rainfall", 0.2);
                }})
                .keys(new HashMap<>() {{
                    put("id", 1L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_missingPrimaryKey_succeeds() throws SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            MetadataServiceException {
        /* modify row based on non-primary key column */
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .data(new HashMap<>() {{
                    put("date", "2023-10-03");
                    put("location", "Vienna");
                    put("mintemp", 15.0);
                    put("rainfall", 0.2);
                }})
                .keys(new HashMap<>() {{
                    put("date", "2008-12-01");
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_notInOrder_succeeds() throws SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            MetadataServiceException {
        /* modify row based on non-primary key column */
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .data(new HashMap<>() {{
                    put("mintemp", 15.0);
                    put("location", "Vienna");
                    put("rainfall", 0.2);
                    put("date", "2023-10-03");
                }})
                .keys(new HashMap<>() {{
                    put("date", "2008-12-01");
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void createTuple_succeeds() throws SQLException, RemoteUnavailableException, ContainerNotFoundException,
            TableNotFoundException, TableMalformedException, QueryMalformedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException {
        /* add row with primary key */
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put("id", 4L);
                    put("date", "2023-10-03");
                    put("location", "Vienna");
                    put("mintemp", 15.0);
                    put("rainfall", 0.2);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.createTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date"));
        assertEquals("Vienna", result.get(0).get("location"));
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void createTuple_autogeneratedBlob_succeeds() throws SQLException, RemoteUnavailableException, ContainerNotFoundException,
            TableNotFoundException, TableMalformedException, QueryMalformedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException, IOException {
        /* add row with primary key */
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put("value", "24.3");
                    put("raw", "s3key");
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_DTO);
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/keyboard.csv")));

        /* test */
        tableService.createTuple(TABLE_8_DTO, request);
        final List<Map<String, byte[]>> result = MariaDbConfig.selectQueryByteArr(DATABASE_3_DTO, "SELECT raw FROM mfcc WHERE raw IS NOT NULL", Set.of("raw"));
        assertNotNull(result.get(0).get("raw"));
        assertArrayEquals(Files.toByteArray(new File("src/test/resources/csv/keyboard.csv")), result.get(0).get("raw"));
    }

    @Test
    public void createTuple_notInOrder_succeeds() throws SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException, MetadataServiceException {
        /* add row with primary key */
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put("location", "Vienna");
                    put("id", 4L);
                    put("date", "2023-10-03");
                    put("rainfall", 0.2);
                    put("mintemp", 15.0);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.createTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date"));
        assertEquals("Vienna", result.get(0).get("location"));
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void deleteTuple_succeeds() throws SQLException, RemoteUnavailableException, ContainerNotFoundException,
            TableNotFoundException, TableMalformedException, QueryMalformedException, MetadataServiceException {
        /* delete row based on primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("id", 1L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.deleteTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
        assertEquals(0, result.size());
    }

    @Test
    public void deleteTuple_withoutPrimaryKey_succeeds() throws SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            MetadataServiceException {
        /* remove row based on non-primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("date", "2008-12-01");
                    put("location", "Albury");
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        tableService.deleteTuple(TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_DTO, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
        assertEquals(0, result.size());
    }

    @Test
    @Disabled("Not stable CI/CD")
    public void getStatistics_succeeds() throws TableMalformedException, SQLException, TableNotFoundException {

        /* test */
        final TableStatisticDto response = tableService.getStatistics(TABLE_2_DTO);
        assertEquals(TABLE_2_COLUMNS.size(), response.getColumns().size());
        log.trace("response rows: {}", response.getRows());
        assertEquals(3L, response.getRows());
        assertEquals(Set.of("location", "lat", "lng"), response.getColumns().keySet());
        final ColumnStatisticDto column0 = response.getColumns().get("location");
        assertNull(column0.getMin());
        assertNull(column0.getMax());
        assertNull(column0.getMean());
        assertNull(column0.getMedian());
        assertNull(column0.getStdDev());
        final ColumnStatisticDto column3 = response.getColumns().get("lat");
        assertEquals(BigDecimal.valueOf(-36.0653583), column3.getMin());
        assertEquals(BigDecimal.valueOf(-33.847927), column3.getMax());
        assertNotNull(column3.getMean());
        assertNotNull(column3.getMedian());
        assertNotNull(column3.getStdDev());
        final ColumnStatisticDto column4 = response.getColumns().get("lng");
        assertEquals(BigDecimal.valueOf(146.9112214), column4.getMin());
        assertEquals(BigDecimal.valueOf(150.6517942), column4.getMax());
        assertNotNull(column4.getMean());
        assertNotNull(column4.getMedian());
        assertNotNull(column4.getStdDev());
    }

    @Test
    public void delete_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        tableService.delete(TABLE_1_DTO);
    }

    @Test
    public void delete_notFound_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.delete(TABLE_5_DTO);
        });
    }

    @Test
    public void getCount_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(TABLE_1_DTO, null);
        assertEquals(3, response);
    }

    @Test
    public void getCount_timestamp_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(TABLE_1_DTO, Instant.ofEpochSecond(0));
        assertEquals(0, response);
    }

    @Test
    public void getCount_notFound_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.getCount(TABLE_5_DTO, null);
        });
    }

    @Test
    public void history_succeeds() throws SQLException, TableNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.history(TABLE_1_DTO, 1000L);
        assertEquals(1, response.size());
        final TableHistoryDto history0 = response.get(0);
        assertNotNull(history0.getTimestamp());
        assertEquals("INSERT", history0.getEvent());
        assertEquals(3, history0.getTotal());
    }

    @Test
    public void history_notFound_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.history(TABLE_5_DTO, null);
        });
    }

    @Test
    public void importDataset_succeeds() throws SQLException, MalformedException, StorageUnavailableException,
            StorageNotFoundException, QueryMalformedException, TableMalformedException {
        final ImportDto request = ImportDto.builder()
                .header(false)
                .lineTermination("\n")
                .quote('"')
                .separator(';')
                .location("s3key") /* irrelevant */
                .build();

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        tableService.importDataset(TABLE_1_DTO, request);
    }

    @Test
    public void importDataset_withHeader_fails() {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\n")
                .quote('"')
                .separator(';')
                .location("s3key") /* irrelevant */
                .build();

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableService.importDataset(TABLE_1_DTO, request);
        });
    }

    @Test
    public void importDataset_wrongSeparator_fails() {
        final ImportDto request = ImportDto.builder()
                .header(false)
                .lineTermination("\n")
                .quote('"')
                .separator(',')
                .location("s3key") /* irrelevant */
                .build();

        /* mock */
        s3Client.putObject(PutObjectRequest.builder()
                .key("s3key")
                .bucket(s3Config.getS3Bucket())
                .build(), RequestBody.fromFile(new File("src/test/resources/csv/weather_aus.csv")));

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.importDataset(TABLE_1_DTO, request);
        });
    }

}
