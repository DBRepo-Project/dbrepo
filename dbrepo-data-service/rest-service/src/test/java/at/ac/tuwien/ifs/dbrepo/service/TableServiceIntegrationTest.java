package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.SparkConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.CreateForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary.PrimaryKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
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
import java.util.*;

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
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_2_INTERNAL_NAME);
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_3_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        MariaDbUtil.createInitDatabase(DATABASE_2_CACHE);
        MariaDbUtil.createInitDatabase(DATABASE_3_CACHE);
    }

    @Test
    public void updateTuple_succeeds() throws SQLException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException {
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
        tableService.updateTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_modifyPrimaryKey_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
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
        tableService.updateTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_missingPrimaryKey_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
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
        tableService.updateTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("1", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date")); // <<<
        assertEquals("Vienna", result.get(0).get("location")); // <<<
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void updateTuple_notInOrder_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
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
        tableService.updateTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
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
        tableService.createTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
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
        tableService.createTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        final List<Map<String, byte[]>> result = MariaDbUtil.selectQueryByteArr(DATABASE_3_CACHE, "SELECT raw FROM mfcc WHERE raw IS NOT NULL", Set.of("raw"));
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
        tableService.createTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date"));
        assertEquals("Vienna", result.get(0).get("location"));
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void deleteTuple_succeeds() throws SQLException, TableMalformedException, QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        /* delete row based on primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("id", 1L);
                }})
                .build();

        /* test */
        tableService.deleteTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
        assertEquals(0, result.size());
    }

    @Test
    public void deleteTuple_withoutPrimaryKey_succeeds() throws SQLException, TableMalformedException,
            QueryMalformedException, StorageUnavailableException, StorageNotFoundException {
        /* remove row based on non-primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("date", "2008-12-01");
                    put("location", "Albury");
                }})
                .build();

        /* test */
        tableService.deleteTuple(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        final List<Map<String, String>> result = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
        assertEquals(0, result.size());
    }

    @Test
    public void delete_succeeds() throws SQLException, QueryMalformedException, TableNotFoundException {

        /* test */
        tableService.delete(DATABASE_1_CACHE, TABLE_1_CACHE);
        assertFalse(MariaDbUtil.tableExists(DATABASE_1_CACHE, TABLE_1_INTERNAL_NAME));
    }

    @Test
    public void delete_notFound_fails() throws SQLException {

        /* mock */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_2_INTERNAL_NAME);
        MariaDbUtil.createDatabase(CONTAINER_1_CACHE, DATABASE_2_INTERNAL_NAME);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.delete(DATABASE_2_CACHE, TABLE_5_CACHE);
        });
    }

    @Test
    public void getCount_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(DATABASE_1_CACHE, TABLE_1_INTERNAL_NAME, null);
        assertEquals(3, response);
    }

    @Test
    public void getCount_timestamp_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(DATABASE_1_CACHE, TABLE_1_INTERNAL_NAME, Instant.ofEpochSecond(0));
        assertEquals(0, response);
    }

    @Test
    public void getCount_notFound_fails() throws SQLException {

        /* mock */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_2_INTERNAL_NAME);
        MariaDbUtil.createDatabase(CONTAINER_1_CACHE, DATABASE_2_INTERNAL_NAME);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.getCount(DATABASE_2_CACHE, TABLE_5_INTERNAL_NAME, null);
        });
    }

    @Test
    public void history_succeeds() throws SQLException, TableNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.history(DATABASE_1_CACHE, TABLE_1_CACHE, 1000L);
        assertEquals(1, response.size());
        final TableHistoryDto history0 = response.get(0);
        assertNotNull(history0.getTimestamp());
        assertEquals(HistoryEventTypeDto.INSERT, history0.getEvent());
        assertEquals(3, history0.getTotal());
    }

    @Test
    public void updateTable_succeeds() throws SQLException, TableNotFoundException, TableMalformedException {

        /* test */
        tableService.update(DATABASE_3_CACHE, TABLE_8_CACHE, TABLE_8_UPDATE_DTO);
        assertEquals("", MariaDbUtil.tableDescription(DATABASE_3_CACHE, TABLE_8_INTERNAL_NAME));
    }

    @Test
    public void updateTable_notExists_fails() {
        final Table request = Table.builder()
                .internalName("i_do_not_exist")
                .build();

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.update(DATABASE_3_CACHE, request, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void history_notFound_fails() throws SQLException {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.history(DATABASE_2_CACHE, TABLE_5_CACHE, null);
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
        tableService.importDataset(DATABASE_1_CACHE, TABLE_1_CACHE, request);
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
            tableService.importDataset(DATABASE_1_CACHE, TABLE_1_CACHE, request);
        });
    }

    @Test
    public void inspectTable_sameNameDifferentDb_succeeds() throws TableNotFoundException, SQLException {

        /* mock */
        MariaDbUtil.execute(DATABASE_2_CACHE, "CREATE TABLE not_in_metadata_db (id BIGINT NOT NULL, given_name VARCHAR(255) NOT NULL, middle_name VARCHAR(255), family_name VARCHAR(255) NOT NULL, age INT NOT NULL, PRIMARY KEY (id), CHECK (`age` > 0 and `age` < 120)) WITH SYSTEM VERSIONING;");

        /* test */
        final TableDto response = tableService.inspect(DATABASE_1_CACHE, "not_in_metadata_db");
        assertEquals("not_in_metadata_db", response.getInternalName());
        assertEquals("not_in_metadata_db", response.getName());
        assertEquals(DATABASE_1_ID, response.getDatabaseId());
        assertTrue(response.getIsVersioned());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        final List<ColumnDto> columns = response.getColumns();
        assertNotNull(columns);
        assertEquals(5, columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        assertColumn(columns.get(1), null, null, DATABASE_1_ID, "given_name", "given_name", ColumnTypeDto.VARCHAR, 255L, null, false, null);
        assertColumn(columns.get(2), null, null, DATABASE_1_ID, "middle_name", "middle_name", ColumnTypeDto.VARCHAR, 255L, null, true, null);
        assertColumn(columns.get(3), null, null, DATABASE_1_ID, "family_name", "family_name", ColumnTypeDto.VARCHAR, 255L, null, false, null);
        assertColumn(columns.get(4), null, null, DATABASE_1_ID, "age", "age", ColumnTypeDto.INT, 10L, 0L, false, null);
        final ConstraintsDto constraints = response.getConstraints();
        assertNotNull(constraints);
        final Set<PrimaryKeyDto> primaryKey = constraints.getPrimaryKey();
        assertEquals(1, primaryKey.size());
        final Set<String> checks = constraints.getChecks();
        assertEquals(1, checks.size());
        assertEquals(Set.of("`age` > 0 and `age` < 120"), checks);
        final List<UniqueDto> uniques = constraints.getUniques();
        assertEquals(1, uniques.size());
        assertEquals(2, uniques.get(0).getColumns().size());
        assertEquals("not_in_metadata_db", uniques.get(0).getTable().getName());
        assertEquals("not_in_metadata_db", uniques.get(0).getTable().getInternalName());
        assertEquals("given_name", uniques.get(0).getColumns().get(0).getInternalName());
        assertEquals("family_name", uniques.get(0).getColumns().get(1).getInternalName());
        final List<ForeignKeyDto> foreignKeys = constraints.getForeignKeys();
        assertEquals(0, foreignKeys.size());
    }

    @Test
    public void inspectTableEnum_succeeds() throws TableNotFoundException, SQLException {

        /* test */
        final TableDto response = tableService.inspect(DATABASE_2_CACHE, "experiments");
        assertEquals("experiments", response.getInternalName());
        assertEquals("experiments", response.getName());
        assertEquals(DATABASE_2_ID, response.getDatabaseId());
        assertTrue(response.getIsVersioned());
        assertEquals(DATABASE_2_PUBLIC, response.getIsPublic());
        assertNotNull(response.getOwner());
        assertEquals(USER_2_USERNAME, response.getOwner().getUsername());
        final List<IdentifierDto> identifiers = response.getIdentifiers();
        assertNotNull(identifiers);
        assertEquals(0, identifiers.size());
        final List<ColumnDto> columns = response.getColumns();
        assertNotNull(columns);
        assertEquals(3, columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_2_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        assertColumn(columns.get(1), null, null, DATABASE_2_ID, "mode", "mode", ColumnTypeDto.ENUM, 3L, null, false, null);
        assertEquals(2, columns.get(1).getEnums().size());
        assertEquals(List.of("ABC", "DEF"), columns.get(1).getEnums().stream().map(EnumDto::getValue).toList());
        assertColumn(columns.get(2), null, null, DATABASE_2_ID, "seq", "seq", ColumnTypeDto.SET, 5L, null, true, null);
        assertEquals(3, columns.get(2).getSets().size());
        assertEquals(List.of("1", "2", "3"), columns.get(2).getSets().stream().map(SetDto::getValue).toList());
        /* ignore rest (constraints) */
    }

    @Test
    public void inspectTableFullConstraints_succeeds() throws TableNotFoundException, SQLException {

        /* test */
        final TableDto response = tableService.inspect(DATABASE_1_CACHE, "weather_aus");
        assertEquals("weather_aus", response.getInternalName());
        assertEquals("weather_aus", response.getName());
        assertEquals(DATABASE_1_ID, response.getDatabaseId());
        assertTrue(response.getIsVersioned());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        assertNotNull(response.getOwner());
        assertEquals(USER_1_USERNAME, response.getOwner().getUsername());
        final List<IdentifierDto> identifiers = response.getIdentifiers();
        assertNotNull(identifiers);
        assertEquals(0, identifiers.size());
        final List<ColumnDto> columns = response.getColumns();
        assertNotNull(columns);
        assertEquals(5, columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 20L, 0L, false, null);
        assertColumn(columns.get(1), null, null, DATABASE_1_ID, "date", "date", ColumnTypeDto.DATE, null, null, false, null);
        assertColumn(columns.get(2), null, null, DATABASE_1_ID, "location", "location", ColumnTypeDto.VARCHAR, 255L, null, true, "Closest city");
        assertColumn(columns.get(3), null, null, DATABASE_1_ID, "mintemp", "mintemp", ColumnTypeDto.DOUBLE, 22L, null, true, null);
        assertColumn(columns.get(4), null, null, DATABASE_1_ID, "rainfall", "rainfall", ColumnTypeDto.DOUBLE, 22L, null, true, null);
        final ConstraintsDto constraints = response.getConstraints();
        final List<PrimaryKeyDto> primaryKey = new LinkedList<>(constraints.getPrimaryKey());
        assertEquals(1, primaryKey.size());
        final PrimaryKeyDto pk0 = primaryKey.get(0);
        assertNull(pk0.getId());
        assertNotNull(pk0.getTable());
        assertNull(pk0.getTable().getId());
        assertEquals("weather_aus", pk0.getTable().getName());
        assertEquals("weather_aus", pk0.getTable().getInternalName());
        assertEquals("Weather in Australia", pk0.getTable().getDescription());
        assertNotNull(pk0.getColumn());
        assertNull(pk0.getColumn().getId());
        assertNull(pk0.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, pk0.getColumn().getDatabaseId());
        assertNull(pk0.getColumn().getAlias());
        assertEquals("id", pk0.getColumn().getName());
        assertEquals("id", pk0.getColumn().getInternalName());
        assertEquals(ColumnTypeDto.BIGINT, pk0.getColumn().getColumnType());
        final List<UniqueDto> uniques = constraints.getUniques();
        assertEquals(1, uniques.size());
        final UniqueDto unique0 = uniques.get(0);
        assertNotNull(unique0.getTable());
        assertEquals("some_constraint", unique0.getName());
        assertNull(unique0.getTable().getId());
        assertEquals(TABLE_1_INTERNAL_NAME, unique0.getTable().getName());
        assertEquals(TABLE_1_INTERNAL_NAME, unique0.getTable().getInternalName());
        assertEquals(TABLE_1_DESCRIPTION, unique0.getTable().getDescription());
        assertTrue(unique0.getTable().getIsVersioned());
        assertNotNull(unique0.getColumns());
        assertEquals(1, unique0.getColumns().size());
        assertNull(unique0.getColumns().get(0).getId());
        assertNull(unique0.getColumns().get(0).getTableId());
        assertEquals("date", unique0.getColumns().get(0).getName());
        assertEquals("date", unique0.getColumns().get(0).getInternalName());
        final List<String> checks = new LinkedList<>(constraints.getChecks());
        assertEquals("`mintemp` > 0", checks.get(0));
        final List<ForeignKeyDto> foreignKeys = constraints.getForeignKeys();
        assertEquals(1, foreignKeys.size());
        final ForeignKeyDto fk0 = foreignKeys.get(0);
        assertNotNull(fk0.getName());
        assertNotNull(fk0.getReferences());
        final ForeignKeyReferenceDto fk0ref0 = fk0.getReferences().get(0);
        assertNull(fk0ref0.getId());
        assertNotNull(fk0ref0.getColumn());
        assertNotNull(fk0ref0.getReferencedColumn());
        assertNotNull(fk0ref0.getForeignKey());
        assertEquals(DATABASE_1_ID, fk0ref0.getColumn().getDatabaseId());
        assertNull(fk0ref0.getColumn().getId());
        assertNull(fk0ref0.getColumn().getTableId());
        assertEquals("location", fk0ref0.getColumn().getName());
        assertEquals("location", fk0ref0.getColumn().getInternalName());
        assertEquals(DATABASE_1_ID, fk0ref0.getReferencedColumn().getDatabaseId());
        assertNull(fk0ref0.getReferencedColumn().getId());
        assertNull(fk0ref0.getReferencedColumn().getTableId());
        assertEquals("location", fk0ref0.getReferencedColumn().getName());
        assertEquals("location", fk0ref0.getReferencedColumn().getInternalName());
        assertNotNull(fk0.getOnUpdate());
        assertEquals(ReferenceTypeDto.RESTRICT, fk0.getOnUpdate());
        assertNotNull(fk0.getOnDelete());
        assertEquals(ReferenceTypeDto.SET_NULL, fk0.getOnDelete());
        final TableBriefDto fk0table = fk0.getTable();
        assertNull(fk0table.getId());
        assertEquals(DATABASE_1_ID, fk0table.getDatabaseId());
        assertEquals(TABLE_1_INTERNAL_NAME, fk0table.getName());
        assertEquals(TABLE_1_INTERNAL_NAME, fk0table.getInternalName());
        assertNotNull(fk0.getOnDelete());
        assertNotNull(fk0.getOnUpdate());
        assertNotNull(fk0.getReferencedTable());
        assertEquals(TABLE_2_INTERNAL_NAME, fk0.getReferencedTable().getName());
        assertEquals(TABLE_2_INTERNAL_NAME, fk0.getReferencedTable().getInternalName());
    }

    @Test
    public void inspectTable_multipleForeignKeyReferences_succeeds() throws TableNotFoundException, SQLException {

        /* test */
        final TableDto response = tableService.inspect(DATABASE_1_CACHE, "complex_foreign_keys");
        final ConstraintsDto constraints = response.getConstraints();
        final List<ForeignKeyDto> foreignKeys = constraints.getForeignKeys();
        assertEquals(1, foreignKeys.size());
        final ForeignKeyDto fk0 = foreignKeys.get(0);
        assertNotNull(fk0.getName());
        assertNotNull(fk0.getReferences());
        final ForeignKeyReferenceDto fk0ref0 = fk0.getReferences().get(0);
        assertNull(fk0ref0.getId());
        assertNotNull(fk0ref0.getColumn());
        assertNotNull(fk0ref0.getReferencedColumn());
        assertNotNull(fk0ref0.getForeignKey());
        assertEquals(DATABASE_1_ID, fk0ref0.getColumn().getDatabaseId());
        assertNull(fk0ref0.getColumn().getId());
        assertNull(fk0ref0.getColumn().getTableId());
        assertEquals("weather_id", fk0ref0.getColumn().getName());
        assertEquals("weather_id", fk0ref0.getColumn().getInternalName());
        assertEquals(DATABASE_1_ID, fk0ref0.getReferencedColumn().getDatabaseId());
        assertNull(fk0ref0.getReferencedColumn().getId());
        assertNull(fk0ref0.getReferencedColumn().getTableId());
        assertEquals("id", fk0ref0.getReferencedColumn().getName());
        assertEquals("id", fk0ref0.getReferencedColumn().getInternalName());
        final ForeignKeyReferenceDto fk0ref1 = fk0.getReferences().get(1);
        assertNull(fk0ref1.getId());
        assertNotNull(fk0ref1.getColumn());
        assertNotNull(fk0ref1.getReferencedColumn());
        assertNotNull(fk0ref1.getForeignKey());
        assertEquals(DATABASE_1_ID, fk0ref1.getColumn().getDatabaseId());
        assertNull(fk0ref1.getColumn().getId());
        assertNull(fk0ref1.getColumn().getTableId());
        assertEquals("other_id", fk0ref1.getColumn().getName());
        assertEquals("other_id", fk0ref1.getColumn().getInternalName());
        assertEquals(DATABASE_1_ID, fk0ref1.getReferencedColumn().getDatabaseId());
        assertNull(fk0ref1.getReferencedColumn().getId());
        assertNull(fk0ref1.getReferencedColumn().getTableId());
        assertEquals("other_id", fk0ref1.getReferencedColumn().getName());
        assertEquals("other_id", fk0ref1.getReferencedColumn().getInternalName());
        final TableBriefDto fk0refT0 = fk0.getTable();
        assertNull(fk0refT0.getId());
        assertEquals(DATABASE_1_ID, fk0refT0.getDatabaseId());
        assertEquals("complex_foreign_keys", fk0refT0.getName());
        assertEquals("complex_foreign_keys", fk0refT0.getInternalName());
        assertNotNull(fk0.getReferencedTable());
        assertEquals("complex_primary_key", fk0.getReferencedTable().getName());
        assertEquals("complex_primary_key", fk0.getReferencedTable().getInternalName());
        assertNotNull(fk0.getOnDelete());
        assertNotNull(fk0.getOnUpdate());
    }

    @Test
    public void inspectTable_multiplePrimaryKey_succeeds() throws TableNotFoundException, SQLException {

        /* test */
        final TableDto response = tableService.inspect(DATABASE_1_CACHE, "complex_primary_key");
        final ConstraintsDto constraints = response.getConstraints();
        final List<PrimaryKeyDto> primaryKey = new LinkedList<>(constraints.getPrimaryKey());
        assertEquals(2, primaryKey.size());
        final PrimaryKeyDto pk0 = primaryKey.get(0);
        assertNull(pk0.getId());
        assertNotNull(pk0.getTable());
        assertNull(pk0.getTable().getId());
        assertEquals("complex_primary_key", pk0.getTable().getName());
        assertEquals("complex_primary_key", pk0.getTable().getInternalName());
        assertNotNull(pk0.getColumn());
        assertNull(pk0.getColumn().getId());
        assertNull(pk0.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, pk0.getColumn().getDatabaseId());
        assertNull(pk0.getColumn().getAlias());
        assertEquals("id", pk0.getColumn().getName());
        assertEquals("id", pk0.getColumn().getInternalName());
        assertEquals(ColumnTypeDto.BIGINT, pk0.getColumn().getColumnType());
        final PrimaryKeyDto pk1 = primaryKey.get(1);
        assertNull(pk1.getId());
        assertNotNull(pk1.getTable());
        assertNull(pk1.getTable().getId());
        assertEquals("complex_primary_key", pk1.getTable().getName());
        assertEquals("complex_primary_key", pk1.getTable().getInternalName());
        assertNotNull(pk1.getColumn());
        assertNull(pk1.getColumn().getId());
        assertNull(pk1.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, pk1.getColumn().getDatabaseId());
        assertNull(pk1.getColumn().getAlias());
        assertEquals("other_id", pk1.getColumn().getName());
        assertEquals("other_id", pk1.getColumn().getInternalName());
        assertEquals(ColumnTypeDto.BIGINT, pk1.getColumn().getColumnType());
    }

    @Test
    public void inspectTable_exoticBoolean_succeeds() throws TableNotFoundException, SQLException {

        /* test */
        final TableDto response = tableService.inspect(DATABASE_1_CACHE, "exotic_boolean");
        final ConstraintsDto constraints = response.getConstraints();
        final List<PrimaryKeyDto> primaryKey = new LinkedList<>(constraints.getPrimaryKey());
        assertEquals(1, primaryKey.size());
        final PrimaryKeyDto pk0 = primaryKey.get(0);
        assertNull(pk0.getId());
        assertNotNull(pk0.getTable());
        assertNull(pk0.getTable().getId());
        assertEquals("exotic_boolean", pk0.getTable().getName());
        assertEquals("exotic_boolean", pk0.getTable().getInternalName());
        assertNotNull(pk0.getColumn());
        assertNull(pk0.getColumn().getId());
        assertNull(pk0.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, pk0.getColumn().getDatabaseId());
        assertNull(pk0.getColumn().getAlias());
        assertEquals("bool_default", pk0.getColumn().getName());
        assertEquals("bool_default", pk0.getColumn().getInternalName());
        assertEquals(ColumnTypeDto.BOOL, pk0.getColumn().getColumnType());
        final List<ColumnDto> columns = response.getColumns();
        assertEquals(3, columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_1_ID, "bool_default", "bool_default", ColumnTypeDto.BOOL, null, 0L, false, null);
        assertColumn(columns.get(1), null, null, DATABASE_1_ID, "bool_tinyint", "bool_tinyint", ColumnTypeDto.BOOL, null, 0L, false, null);
        assertColumn(columns.get(2), null, null, DATABASE_1_ID, "bool_tinyint_unsigned", "bool_tinyint_unsigned", ColumnTypeDto.BOOL, null, 0L, false, null);
    }

    @Test
    public void explore_succeeds() throws TableNotFoundException, SQLException, DatabaseMalformedException {

        /* test */
        final List<TableDto> response = tableService.explore(DATABASE_1_CACHE);
        assertEquals(4, response.size());
        final TableDto table0 = response.get(0);
        Assertions.assertEquals("complex_foreign_keys", table0.getInternalName());
        Assertions.assertEquals("complex_foreign_keys", table0.getName());
        Assertions.assertEquals(DATABASE_1_ID, table0.getDatabaseId());
        assertTrue(table0.getIsVersioned());
        Assertions.assertEquals(DATABASE_1_PUBLIC, table0.getIsPublic());
        final List<ColumnDto> columns0 = table0.getColumns();
        assertNotNull(columns0);
        Assertions.assertEquals(3, columns0.size());
        assertColumn(columns0.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        assertColumn(columns0.get(1), null, null, DATABASE_1_ID, "weather_id", "weather_id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        assertColumn(columns0.get(2), null, null, DATABASE_1_ID, "other_id", "other_id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        final ConstraintsDto constraints0 = table0.getConstraints();
        assertNotNull(constraints0);
        assertEquals(1, constraints0.getPrimaryKey().size());
        final PrimaryKeyDto pk0 = new LinkedList<>(constraints0.getPrimaryKey()).get(0);
        assertNull(pk0.getId());
        assertNull(pk0.getColumn().getId());
        assertEquals("id", pk0.getColumn().getName());
        assertEquals("id", pk0.getColumn().getInternalName());
        assertEquals(1, constraints0.getForeignKeys().size());
        final ForeignKeyDto fk0 = constraints0.getForeignKeys().get(0);
        assertNotNull(fk0.getName());
        assertNull(fk0.getTable().getId());
        assertEquals("complex_foreign_keys", fk0.getTable().getName());
        assertEquals("complex_foreign_keys", fk0.getTable().getInternalName());
        assertNull(fk0.getReferencedTable().getId());
        assertEquals("complex_primary_key", fk0.getReferencedTable().getName());
        assertEquals("complex_primary_key", fk0.getReferencedTable().getInternalName());
        assertEquals(2, fk0.getReferences().size());
        final ForeignKeyReferenceDto fk0r0 = fk0.getReferences().get(0);
        assertEquals("weather_id", fk0r0.getColumn().getName());
        assertEquals("weather_id", fk0r0.getColumn().getInternalName());
        assertNotNull(fk0r0.getColumn().getName());
        assertNotNull(fk0r0.getForeignKey());
        assertEquals("id", fk0r0.getReferencedColumn().getName());
        assertEquals("id", fk0r0.getReferencedColumn().getInternalName());
        final ForeignKeyReferenceDto fk0r1 = fk0.getReferences().get(1);
        assertEquals("other_id", fk0r1.getColumn().getName());
        assertEquals("other_id", fk0r1.getColumn().getInternalName());
        assertNotNull(fk0r1.getColumn().getName());
        assertNotNull(fk0r1.getForeignKey());
        assertEquals("other_id", fk0r1.getReferencedColumn().getName());
        assertEquals("other_id", fk0r1.getReferencedColumn().getInternalName());
        assertEquals(0, constraints0.getChecks().size());
        assertEquals(0, constraints0.getUniques().size());
        /* table 1 */
        final TableDto table1 = response.get(1);
        Assertions.assertEquals("complex_primary_key", table1.getInternalName());
        Assertions.assertEquals("complex_primary_key", table1.getName());
        Assertions.assertEquals(DATABASE_1_ID, table1.getDatabaseId());
        assertTrue(table1.getIsVersioned());
        Assertions.assertEquals(DATABASE_1_PUBLIC, table1.getIsPublic());
        final List<ColumnDto> columns1 = table1.getColumns();
        assertNotNull(columns1);
        Assertions.assertEquals(2, columns1.size());
        assertColumn(columns1.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        assertColumn(columns1.get(1), null, null, DATABASE_1_ID, "other_id", "other_id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        final ConstraintsDto constraints1 = table1.getConstraints();
        assertNotNull(constraints1);
        assertEquals(2, constraints1.getPrimaryKey().size());
        final PrimaryKeyDto pk10 = new LinkedList<>(constraints1.getPrimaryKey()).get(0);
        assertNull(pk10.getId());
        assertNull(pk10.getColumn().getId());
        assertEquals("id", pk10.getColumn().getName());
        assertEquals("id", pk10.getColumn().getInternalName());
        final PrimaryKeyDto pk11 = new LinkedList<>(constraints1.getPrimaryKey()).get(1);
        assertNull(pk11.getId());
        assertNull(pk11.getColumn().getId());
        assertEquals("other_id", pk11.getColumn().getName());
        assertEquals("other_id", pk11.getColumn().getInternalName());
        assertEquals(0, constraints1.getForeignKeys().size());
        assertEquals(0, constraints1.getChecks().size());
        assertEquals(0, constraints1.getUniques().size());
        /* table 2 */
        final TableDto table2 = response.get(2);
        Assertions.assertEquals("exotic_boolean", table2.getInternalName());
        Assertions.assertEquals("exotic_boolean", table2.getName());
        Assertions.assertEquals(DATABASE_1_ID, table2.getDatabaseId());
        assertTrue(table2.getIsVersioned());
        Assertions.assertEquals(DATABASE_1_PUBLIC, table2.getIsPublic());
        final List<ColumnDto> columns2 = table2.getColumns();
        assertNotNull(columns2);
        Assertions.assertEquals(3, columns2.size());
        assertColumn(columns2.get(0), null, null, DATABASE_1_ID, "bool_default", "bool_default", ColumnTypeDto.BOOL, null, 0L, false, null);
        assertColumn(columns2.get(1), null, null, DATABASE_1_ID, "bool_tinyint", "bool_tinyint", ColumnTypeDto.BOOL, null, 0L, false, null);
        assertColumn(columns2.get(2), null, null, DATABASE_1_ID, "bool_tinyint_unsigned", "bool_tinyint_unsigned", ColumnTypeDto.BOOL, null, 0L, false, null);
        final ConstraintsDto constraints2 = table2.getConstraints();
        assertNotNull(constraints2);
        final Set<PrimaryKeyDto> primaryKey2 = constraints2.getPrimaryKey();
        Assertions.assertEquals(1, primaryKey2.size());
        final Set<String> checks2 = constraints2.getChecks();
        Assertions.assertEquals(0, checks2.size());
        final List<UniqueDto> uniques2 = constraints2.getUniques();
        Assertions.assertEquals(0, uniques2.size());
        /* table 3 */
        final TableDto table3 = response.get(3);
        Assertions.assertEquals("not_in_metadata_db", table3.getInternalName());
        Assertions.assertEquals("not_in_metadata_db", table3.getName());
        Assertions.assertEquals(DATABASE_1_ID, table3.getDatabaseId());
        assertTrue(table3.getIsVersioned());
        Assertions.assertEquals(DATABASE_1_PUBLIC, table3.getIsPublic());
        final List<ColumnDto> columns3 = table3.getColumns();
        assertNotNull(columns3);
        Assertions.assertEquals(5, columns3.size());
        assertColumn(columns3.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        assertColumn(columns3.get(1), null, null, DATABASE_1_ID, "given_name", "given_name", ColumnTypeDto.VARCHAR, 255L, null, false, null);
        assertColumn(columns3.get(2), null, null, DATABASE_1_ID, "middle_name", "middle_name", ColumnTypeDto.VARCHAR, 255L, null, true, null);
        assertColumn(columns3.get(3), null, null, DATABASE_1_ID, "family_name", "family_name", ColumnTypeDto.VARCHAR, 255L, null, false, null);
        assertColumn(columns3.get(4), null, null, DATABASE_1_ID, "age", "age", ColumnTypeDto.INT, 10L, 0L, false, null);
        final ConstraintsDto constraints3 = table3.getConstraints();
        assertNotNull(constraints3);
        final Set<PrimaryKeyDto> primaryKey3 = constraints3.getPrimaryKey();
        Assertions.assertEquals(1, primaryKey3.size());
        final Set<String> checks3 = constraints3.getChecks();
        Assertions.assertEquals(1, checks3.size());
        Assertions.assertEquals(Set.of("`age` > 0 and `age` < 120"), checks3);
        final List<UniqueDto> uniques3 = constraints3.getUniques();
        Assertions.assertEquals(1, uniques3.size());
        Assertions.assertEquals(2, uniques3.get(0).getColumns().size());
        Assertions.assertEquals("not_in_metadata_db", uniques3.get(0).getTable().getInternalName());
        Assertions.assertEquals("given_name", uniques3.get(0).getColumns().get(0).getInternalName());
        Assertions.assertEquals("family_name", uniques3.get(0).getColumns().get(1).getInternalName());
    }

    @Test
    public void createTable_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {

        /* test */
        final TableDto response = tableService.create(DATABASE_1_CACHE, TABLE_4_CREATE_DTO);
        assertEquals(TABLE_4_INTERNAL_NAME, response.getName());
        assertEquals(TABLE_4_INTERNAL_NAME, response.getInternalName());
        final List<ColumnDto> columns = response.getColumns();
        assertEquals(TABLE_4_COLUMNS.size(), columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_1_ID, "timestamp", "timestamp", ColumnTypeDto.TIMESTAMP, null, null, false, null);
        assertColumn(columns.get(1), null, null, DATABASE_1_ID, "value", "value", ColumnTypeDto.DECIMAL, 10L, 10L, true, null);
        final ConstraintsDto constraints = response.getConstraints();
        assertNotNull(constraints);
        final Set<PrimaryKeyDto> primaryKey = constraints.getPrimaryKey();
        Assertions.assertEquals(1, primaryKey.size());
        final Set<String> checks = constraints.getChecks();
        Assertions.assertEquals(0, checks.size());
    }

    @Test
    public void createTable_malformed_fails() {
        final CreateTableDto request = CreateTableDto.builder()
                .name("missing_foreign_key")
                .columns(List.of())
                .constraints(CreateTableConstraintsDto.builder()
                        .foreignKeys(List.of(CreateForeignKeyDto.builder()
                                .columns(List.of("i_do_not_exist"))
                                .referencedTable("neither_do_i")
                                .referencedColumns(List.of("behold"))
                                .build()))
                        .build())
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableService.create(DATABASE_1_CACHE, request);
        });
    }

    @Test
    public void createTable_compositePrimaryKey_fails() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {
        final CreateTableDto request = CreateTableDto.builder()
                .name("composite_primary_key")
                .columns(List.of(CreateTableColumnDto.builder()
                                .name("name")
                                .type(ColumnTypeDto.VARCHAR)
                                .size(255L)
                                .nullAllowed(false)
                                .build(),
                        CreateTableColumnDto.builder()
                                .name("lat")
                                .type(ColumnTypeDto.DECIMAL)
                                .size(10L)
                                .d(10L)
                                .nullAllowed(false)
                                .build(),
                        CreateTableColumnDto.builder()
                                .name("lng")
                                .type(ColumnTypeDto.DECIMAL)
                                .size(10L)
                                .d(10L)
                                .nullAllowed(false)
                                .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .primaryKey(Set.of("lat", "lng"))
                        .foreignKeys(List.of())
                        .checks(Set.of())
                        .uniques(List.of())
                        .build())
                .build();

        /* test */
        final TableDto response = tableService.create(DATABASE_1_CACHE, request);
        assertEquals("composite_primary_key", response.getName());
        assertEquals("composite_primary_key", response.getInternalName());
        final List<ColumnDto> columns = response.getColumns();
        assertEquals(3, columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_1_ID, "name", "name", ColumnTypeDto.VARCHAR, 255L, null, false, null);
        assertColumn(columns.get(1), null, null, DATABASE_1_ID, "lat", "lat", ColumnTypeDto.DECIMAL, 10L, 10L, false, null);
        assertColumn(columns.get(2), null, null, DATABASE_1_ID, "lng", "lng", ColumnTypeDto.DECIMAL, 10L, 10L, false, null);
        final ConstraintsDto constraints = response.getConstraints();
        assertNotNull(constraints);
        final Set<String> checks = constraints.getChecks();
        assertNotNull(checks);
        assertEquals(0, checks.size());
        final List<PrimaryKeyDto> primaryKeys = new LinkedList<>(constraints.getPrimaryKey());
        assertNotNull(primaryKeys);
        assertEquals(2, primaryKeys.size());
        assertTrue(primaryKeys.stream().map(pk -> pk.getColumn().getInternalName()).toList().containsAll(List.of("lat", "lng")));
        final List<ForeignKeyDto> foreignKeys = constraints.getForeignKeys();
        assertNotNull(foreignKeys);
        assertEquals(0, foreignKeys.size());
        final List<UniqueDto> uniques = constraints.getUniques();
        assertNotNull(uniques);
        assertEquals(0, uniques.size());
    }

    @Test
    public void createTable_needSequence_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {

        /* mock */
        MariaDbUtil.dropTable(DATABASE_1_CACHE, TABLE_1_INTERNAL_NAME);

        /* test */
        final TableDto response = tableService.create(DATABASE_1_CACHE, TABLE_1_CREATE_DTO);
        assertEquals(TABLE_1_INTERNAL_NAME, response.getName());
        assertEquals(TABLE_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(TABLE_1_COLUMNS.size(), response.getColumns().size());
    }

    protected static void assertColumn(ColumnDto column, UUID id, UUID tableId, UUID databaseId, String name,
                                       String internalName, ColumnTypeDto type, Long size, Long d, Boolean nullAllowed,
                                       String description) {
        log.trace("assert column: {}", internalName);
        assertNotNull(column);
        assertEquals(id, column.getId());
        assertEquals(tableId, column.getTableId());
        assertEquals(databaseId, column.getDatabaseId());
        assertEquals(name, column.getName());
        assertEquals(internalName, column.getInternalName());
        assertEquals(type, column.getColumnType());
        assertEquals(size, column.getSize());
        assertEquals(d, column.getD());
        assertEquals(nullAllowed, column.getIsNullAllowed());
        assertEquals(description, column.getDescription());
    }

}
