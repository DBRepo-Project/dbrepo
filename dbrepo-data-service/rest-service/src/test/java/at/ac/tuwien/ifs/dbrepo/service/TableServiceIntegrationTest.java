package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.SparkConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class TableServiceIntegrationTest extends BaseTest {

    @Autowired
    private TableService tableService;

    @Autowired
    private SparkConfig sparkConfig;

    @MockitoBean
    private StorageService storageService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNAL_NAME);
        MariaDbUtil.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_3_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
        MariaDbUtil.createInitDatabase(DATABASE_3_PRIVILEGED_DTO);
    }

    @Test
    public void updateTuple_succeeds() throws SQLException, TableMalformedException, QueryMalformedException {
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

        /* test */
        tableService.updateTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_modifyPrimaryKey_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException {
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

        /* test */
        tableService.updateTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_missingPrimaryKey_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException {
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

        /* test */
        tableService.updateTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_notInOrder_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException {
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

        /* test */
        tableService.updateTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void createTuple_succeeds() throws SQLException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException {
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

        /* test */
        tableService.createTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date"));
        assertEquals("Vienna", result.get(0).get("location"));
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void createTuple_autogeneratedBlob_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException, StorageUnavailableException, StorageNotFoundException, IOException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put("value", "24.3");
                    put("raw", "s3key");
                }})
                .build();

        /* mock */
        when(storageService.getBytes("s3key"))
                .thenReturn(Files.toByteArray(new File("src/test/resources/csv/keyboard.csv")));

        /* test */
        tableService.createTuple(DATABASE_3_PRIVILEGED_DTO, TABLE_8_DTO, request);
        final List<Map<String, byte[]>> result = MariaDbUtil.selectQueryByteArr(DATABASE_3_PRIVILEGED_DTO, "SELECT raw FROM mfcc WHERE raw IS NOT NULL", Set.of("raw"));
        assertNotNull(result.get(0).get("raw"));
        assertArrayEquals(Files.toByteArray(new File("src/test/resources/csv/keyboard.csv")), result.get(0).get("raw"));
    }

    @Test
    public void createTuple_notInOrder_succeeds() throws SQLException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException {
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

        /* test */
        tableService.createTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date"));
        assertEquals("Vienna", result.get(0).get("location"));
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void deleteTuple_succeeds() throws SQLException, TableMalformedException, QueryMalformedException {
        /* delete row based on primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("id", 1L);
                }})
                .build();

        /* test */
        tableService.deleteTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
        assertEquals(0, result.size());
    }

    @Test
    public void deleteTuple_withoutPrimaryKey_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException {
        /* remove row based on non-primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("date", "2008-12-01");
                    put("location", "Albury");
                }})
                .build();

        /* test */
        tableService.deleteTuple(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
        assertEquals(0, result.size());
    }

    @Test
    public void delete_succeeds() throws SQLException, QueryMalformedException, TableNotFoundException {

        /* test */
        tableService.delete(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO);
    }

    @Test
    public void delete_notFound_fails() throws SQLException {

        /* mock */
        MariaDbUtil.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNAL_NAME);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.delete(DATABASE_2_PRIVILEGED_DTO, TABLE_5_DTO);
        });
    }

    @Test
    public void getCount_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(DATABASE_1_PRIVILEGED_DTO, TABLE_1_INTERNAL_NAME, null);
        assertEquals(3, response);
    }

    @Test
    public void getCount_timestamp_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(DATABASE_1_PRIVILEGED_DTO, TABLE_1_INTERNAL_NAME, Instant.ofEpochSecond(0));
        assertEquals(0, response);
    }

    @Test
    public void getCount_notFound_fails() throws SQLException {

        /* mock */
        MariaDbUtil.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNAL_NAME);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.getCount(DATABASE_2_PRIVILEGED_DTO, TABLE_5_INTERNAL_NAME, null);
        });
    }

    @Test
    public void history_succeeds() throws SQLException, TableNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.history(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, 1000L);
        assertEquals(1, response.size());
        final TableHistoryDto history0 = response.get(0);
        assertNotNull(history0.getTimestamp());
        assertEquals(HistoryEventTypeDto.INSERT, history0.getEvent());
        assertEquals(3, history0.getTotal());
    }

    @Test
    public void history_notFound_fails() throws SQLException {

        /* mock */
        MariaDbUtil.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNAL_NAME);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.history(DATABASE_2_PRIVILEGED_DTO, TABLE_5_DTO, null);
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
        when(storageService.loadDataset(anyList(), anyString(), anyString(), anyBoolean()))
                .thenReturn(sparkConfig.loadDataset("src/test/resources/csv/weather_aus.csv", ";", false, "id", "Date", "Location", "MinTemp", "Rainfall"));

        /* test */
        tableService.importDataset(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
    }

    @Test
    public void importDataset_wrongSeparator_fails() throws MalformedException, StorageUnavailableException,
            TableMalformedException, StorageNotFoundException {
        final Character separator = ',';
        final ImportDto request = ImportDto.builder()
                .header(false)
                .lineTermination("\n")
                .quote('"')
                .separator(separator)
                .location("s3key") /* irrelevant */
                .build();

        /* mock */
        when(storageService.loadDataset(anyList(), anyString(), anyString(), anyBoolean()))
                .thenReturn(sparkConfig.loadDataset("src/test/resources/csv/weather_aus.csv", "" + separator, false, "id;Date;Location;MinTemp;Rainfall"));

        /* test */
        assertThrows(MalformedException.class, () -> {
            tableService.importDataset(DATABASE_1_PRIVILEGED_DTO, TABLE_1_DTO, request);
        });
    }

}
