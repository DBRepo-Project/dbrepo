package at.tuwien.service;

import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyCreateDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static at.tuwien.service.SchemaServiceIntegrationTest.assertColumn;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class TableServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private TableService tableService;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockBean
    private StorageService storageService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.updateTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 1", Set.of("id", "date", "location", "mintemp", "rainfall"));
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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.createTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
        assertEquals("4", result.get(0).get("id"));
        assertEquals("2023-10-03", result.get(0).get("date"));
        assertEquals("Vienna", result.get(0).get("location"));
        assertEquals("15", result.get(0).get("mintemp"));
        assertEquals("0.2", result.get(0).get("rainfall"));
    }

    @Test
    public void createTuple_autogeneratedBlob_succeeds() throws SQLException, RemoteUnavailableException, ContainerNotFoundException,
            TableNotFoundException, TableMalformedException, QueryMalformedException, StorageUnavailableException,
            StorageNotFoundException, MetadataServiceException {
        final String s3key = "2eec905f-17ed-41de-b12f-283c0aa3e4f9";
        final byte[] s3data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        /* add row with primary key */
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put("value", "24.3");
                    put("raw", s3key);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(storageService.getBytes(s3key))
                .thenReturn(s3data);
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);

        /* test */
        tableService.createTuple(TABLE_8_PRIVILEGED_DTO, request);
        final List<Map<String, byte[]>> result = MariaDbConfig.selectQueryByteArr(DATABASE_3_PRIVILEGED_DTO, "SELECT raw FROM mfcc WHERE raw IS NOT NULL", Set.of("raw"));
        assertNotNull(result.get(0).get("raw"));
        assertArrayEquals(s3data, result.get(0).get("raw"));
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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.createTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id, `date`, location, mintemp, rainfall FROM weather_aus WHERE id = 4", Set.of("id", "date", "location", "mintemp", "rainfall"));
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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.deleteTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
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
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        tableService.deleteTuple(TABLE_1_PRIVILEGED_DTO, request);
        final List<Map<String, String>> result = MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT id FROM weather_aus WHERE id = 1", Set.of("id"));
        assertEquals(0, result.size());
    }

    @Test
    public void getSchemas_succeeds() throws TableNotFoundException, SQLException, DatabaseMalformedException {

        /* test */
        final List<TableDto> response = tableService.getSchemas(DATABASE_1_PRIVILEGED_DTO);
        assertEquals(4, response.size());
        final TableDto table0 = response.get(0);
        Assertions.assertEquals("complex_foreign_keys", table0.getInternalName());
        Assertions.assertEquals("complex_foreign_keys", table0.getName());
        Assertions.assertEquals(DATABASE_1_ID, table0.getTdbid());
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
        Assertions.assertEquals(DATABASE_1_ID, table1.getTdbid());
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
        Assertions.assertEquals(DATABASE_1_ID, table2.getTdbid());
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
        Assertions.assertEquals(DATABASE_1_ID, table3.getTdbid());
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
    public void create_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {

        /* test */
        final TableDto response = tableService.createTable(DATABASE_1_PRIVILEGED_DTO, TABLE_4_CREATE_INTERNAL_DTO);
        assertEquals(TABLE_4_NAME, response.getName());
        assertEquals(TABLE_4_INTERNALNAME, response.getInternalName());
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
    @Disabled("Not stable CI/CD")
    public void getStatistics_succeeds() throws TableMalformedException, SQLException, TableNotFoundException {

        /* test */
        final TableStatisticDto response = tableService.getStatistics(TABLE_2_PRIVILEGED_DTO);
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
    public void create_malformed_fails() {
        final at.tuwien.api.database.table.internal.TableCreateDto request = TableCreateDto.builder()
                .name("missing_foreign_key")
                .columns(List.of())
                .constraints(ConstraintsCreateDto.builder()
                        .foreignKeys(List.of(ForeignKeyCreateDto.builder()
                                .columns(List.of("i_do_not_exist"))
                                .referencedTable("neither_do_i")
                                .referencedColumns(List.of("behold"))
                                .build()))
                        .build())
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableService.createTable(DATABASE_1_PRIVILEGED_DTO, request);
        });
    }

    @Test
    public void create_compositePrimaryKey_fails() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {
        final at.tuwien.api.database.table.internal.TableCreateDto request = TableCreateDto.builder()
                .name("composite_primary_key")
                .columns(List.of(ColumnCreateDto.builder()
                                .name("name")
                                .type(ColumnTypeDto.VARCHAR)
                                .size(255L)
                                .nullAllowed(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("lat")
                                .type(ColumnTypeDto.DECIMAL)
                                .size(10L)
                                .d(10L)
                                .nullAllowed(false)
                                .build(),
                        ColumnCreateDto.builder()
                                .name("lng")
                                .type(ColumnTypeDto.DECIMAL)
                                .size(10L)
                                .d(10L)
                                .nullAllowed(false)
                                .build()))
                .constraints(ConstraintsCreateDto.builder()
                        .primaryKey(Set.of("lat", "lng"))
                        .foreignKeys(List.of())
                        .checks(Set.of())
                        .uniques(List.of())
                        .build())
                .build();

        /* test */
        final TableDto response = tableService.createTable(DATABASE_1_PRIVILEGED_DTO, request);
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
        assertEquals("lat", primaryKeys.get(0).getColumn().getInternalName());
        assertEquals("lng", primaryKeys.get(1).getColumn().getInternalName());
        final List<ForeignKeyDto> foreignKeys = constraints.getForeignKeys();
        assertNotNull(foreignKeys);
        assertEquals(0, foreignKeys.size());
        final List<UniqueDto> uniques = constraints.getUniques();
        assertNotNull(uniques);
        assertEquals(0, uniques.size());
    }

    @Test
    public void create_needSequence_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {

        /* mock */
        MariaDbConfig.dropTable(DATABASE_1_PRIVILEGED_DTO, TABLE_1_INTERNALNAME);

        /* test */
        final TableDto response = tableService.createTable(DATABASE_1_PRIVILEGED_DTO, TABLE_1_CREATE_INTERNAL_DTO);
        assertEquals(TABLE_1_NAME, response.getName());
        assertEquals(TABLE_1_INTERNALNAME, response.getInternalName());
        assertEquals(TABLE_1_COLUMNS.size(), response.getColumns().size());
    }

    @Test
    public void delete_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        tableService.delete(TABLE_1_PRIVILEGED_DTO);
    }

    @Test
    public void delete_notFound_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.delete(TABLE_5_PRIVILEGED_DTO);
        });
    }

    @Test
    public void getCount_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(TABLE_1_PRIVILEGED_DTO, null);
        assertEquals(3, response);
    }

    @Test
    public void getCount_timestamp_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = tableService.getCount(TABLE_1_PRIVILEGED_DTO, Instant.ofEpochSecond(0));
        assertEquals(0, response);
    }

    @Test
    public void getCount_notFound_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.getCount(TABLE_5_PRIVILEGED_DTO, null);
        });
    }

    @Test
    public void history_succeeds() throws SQLException, TableNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.history(TABLE_1_PRIVILEGED_DTO, 1000L);
        assertEquals(1, response.size());
        final TableHistoryDto history0 = response.get(0);
        assertNotNull(history0.getTimestamp());
        assertEquals("INSERT", history0.getEvent());
        assertEquals(3, history0.getTotal());
    }

    @Test
    public void history_notFound_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.history(TABLE_5_PRIVILEGED_DTO, null);
        });
    }

}
