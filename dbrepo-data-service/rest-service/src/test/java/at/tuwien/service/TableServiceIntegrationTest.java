package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryResultDto;
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
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static at.tuwien.service.SchemaServiceIntegrationTest.assertColumn;
import static at.tuwien.service.SchemaServiceIntegrationTest.assertViewColumn;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
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
    private DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @MockBean
    private StorageService storageService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
    }

    @Test
    public void updateTuple_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            ServiceException {
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

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void updateTuple_modifyPrimaryKey_succeeds() throws InterruptedException, SQLException,
            RemoteUnavailableException, ContainerNotFoundException, TableNotFoundException, TableMalformedException,
            QueryMalformedException, ServiceException {
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

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void updateTuple_missingPrimaryKey_succeeds() throws InterruptedException, SQLException,
            RemoteUnavailableException, ContainerNotFoundException, TableNotFoundException, TableMalformedException,
            QueryMalformedException, ServiceException {
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

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void updateTuple_notInOrder_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            ServiceException {
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

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void createTuple_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException, ServiceException {
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

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void createTuple_notInOrder_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException, ServiceException {
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

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void deleteTuple_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException,
            ServiceException {
        /* delete row based on primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("id", 1L);
                }})
                .build();

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void deleteTuple_withoutPrimaryKey_succeeds() throws InterruptedException, SQLException,
            RemoteUnavailableException, ContainerNotFoundException, TableNotFoundException, TableMalformedException,
            QueryMalformedException, ServiceException {
        /* remove row based on non-primary key */
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put("date", "2008-12-01");
                    put("location", "Albury");
                }})
                .build();

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

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
    public void getSchemas_succeeds() throws TableNotFoundException, SQLException, QueryMalformedException,
            DatabaseMalformedException {

        /* test */
        final List<TableDto> response = tableService.getSchemas(DATABASE_1_PRIVILEGED_DTO);
        assertEquals(3, response.size());
        final TableDto table0 = response.get(0);
        Assertions.assertEquals("complex_foreign_keys", table0.getInternalName());
        Assertions.assertEquals("complex_foreign_keys", table0.getName());
        Assertions.assertEquals(DATABASE_1_ID, table0.getTdbid());
        assertTrue(table0.getIsVersioned());
        Assertions.assertEquals(DATABASE_1_PUBLIC, table0.getIsPublic());
        final List<ColumnDto> columns0 = table0.getColumns();
        assertNotNull(columns0);
        Assertions.assertEquals(3, columns0.size());
        assertColumn(columns0.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
        assertColumn(columns0.get(1), null, null, DATABASE_1_ID, "weather_id", "weather_id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
        assertColumn(columns0.get(2), null, null, DATABASE_1_ID, "other_id", "other_id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
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
        assertColumn(columns1.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
        assertColumn(columns1.get(1), null, null, DATABASE_1_ID, "other_id", "other_id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
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
        Assertions.assertEquals("not_in_metadata_db", table2.getInternalName());
        Assertions.assertEquals("not_in_metadata_db", table2.getName());
        Assertions.assertEquals(DATABASE_1_ID, table2.getTdbid());
        assertTrue(table2.getIsVersioned());
        Assertions.assertEquals(DATABASE_1_PUBLIC, table2.getIsPublic());
        final List<ColumnDto> columns2 = table2.getColumns();
        assertNotNull(columns2);
        Assertions.assertEquals(5, columns2.size());
        assertColumn(columns2.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
        assertColumn(columns2.get(1), null, null, DATABASE_1_ID, "given_name", "given_name", ColumnTypeDto.VARCHAR, 255L, null, false, null, null);
        assertColumn(columns2.get(2), null, null, DATABASE_1_ID, "middle_name", "middle_name", ColumnTypeDto.VARCHAR, 255L, null, true, null, null);
        assertColumn(columns2.get(3), null, null, DATABASE_1_ID, "family_name", "family_name", ColumnTypeDto.VARCHAR, 255L, null, false, null, null);
        assertColumn(columns2.get(4), null, null, DATABASE_1_ID, "age", "age", ColumnTypeDto.INT, 10L, 0L, false, null, null);
        final ConstraintsDto constraints2 = table2.getConstraints();
        assertNotNull(constraints2);
        final Set<PrimaryKeyDto> primaryKey2 = constraints2.getPrimaryKey();
        Assertions.assertEquals(1, primaryKey2.size());
        final Set<String> checks2 = constraints2.getChecks();
        Assertions.assertEquals(1, checks2.size());
        Assertions.assertEquals(Set.of("`age` > 0 and `age` < 120"), checks2);
        final List<UniqueDto> uniques2 = constraints2.getUniques();
        Assertions.assertEquals(1, uniques2.size());
        Assertions.assertEquals(2, uniques2.get(0).getColumns().size());
        Assertions.assertEquals("not_in_metadata_db", uniques2.get(0).getTable().getInternalName());
        Assertions.assertEquals("given_name", uniques2.get(0).getColumns().get(0).getInternalName());
        Assertions.assertEquals("family_name", uniques2.get(0).getColumns().get(1).getInternalName());
    }

    @Test
    public void create_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            QueryMalformedException, TableExistsException {

        /* test */
        final TableDto response = tableService.createTable(DATABASE_1_PRIVILEGED_DTO, TABLE_4_CREATE_INTERNAL_DTO);
        assertEquals(TABLE_4_NAME, response.getName());
        assertEquals(TABLE_4_INTERNALNAME, response.getInternalName());
        assertEquals(TABLE_4_COLUMNS.size(), response.getColumns().size());
    }

    @Test
    public void getStatistics_succeeds() throws TableMalformedException, SQLException, QueryMalformedException {

        /* test */
        final TableStatisticDto response = tableService.getStatistics(TABLE_1_PRIVILEGED_DTO);
        assertEquals(TABLE_1_COLUMNS.size(), response.getColumns().size());
        assertEquals(3L, response.getRows());
        assertEquals(Set.of("id", "date", "location", "mintemp", "rainfall"), response.getColumns().keySet());
        final ColumnStatisticDto column0 = response.getColumns().get("id");
        assertEquals(BigDecimal.valueOf(1L), column0.getMin());
        assertEquals(BigDecimal.valueOf(3L), column0.getMax());
        assertNotNull(column0.getMean());
        assertNotNull(column0.getMedian());
        assertNotNull(column0.getStdDev());
        final ColumnStatisticDto column1 = response.getColumns().get("date");
        assertNull(column1.getMin());
        assertNull(column1.getMax());
        assertNull(column1.getMean());
        assertNull(column1.getMedian());
        assertNull(column1.getStdDev());
        final ColumnStatisticDto column2 = response.getColumns().get("location");
        assertNull(column2.getMin());
        assertNull(column2.getMax());
        assertNull(column2.getMean());
        assertNull(column2.getMedian());
        assertNull(column2.getStdDev());
        final ColumnStatisticDto column3 = response.getColumns().get("mintemp");
        assertEquals(BigDecimal.valueOf(7.4), column3.getMin());
        assertEquals(BigDecimal.valueOf(13.4), column3.getMax());
        assertNotNull(column3.getMean());
        assertNotNull(column3.getMedian());
        assertNotNull(column3.getStdDev());
        final ColumnStatisticDto column4 = response.getColumns().get("rainfall");
        assertEquals(BigDecimal.valueOf(0L), column4.getMin());
        assertEquals(BigDecimal.valueOf(0.6), column4.getMax());
        assertNotNull(column4.getMean());
        assertNotNull(column4.getMedian());
        assertNotNull(column4.getStdDev());
    }

    @Test
    public void create_malformed_fails() {
        final at.tuwien.api.database.table.internal.TableCreateDto request = TableCreateDto.builder()
                .needSequence(false)
                .name("missing_foreign_key")
                .columns(List.of(ColumnCreateDto.builder()
                        .name("id")
                        .type(ColumnTypeDto.BIGINT)
                        .nullAllowed(false)
                        .build()))
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
    public void create_needSequence_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            QueryMalformedException, TableExistsException {

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
    public void getData_succeeds() throws SQLException, TableMalformedException {

        /* test */
        final QueryResultDto response = tableService.getData(TABLE_1_PRIVILEGED_DTO, null, 0L, 10L);
        assertEquals(TABLE_1_ID, response.getId());
        final List<Map<String, Integer>> headers = response.getHeaders();
        assertEquals(5, headers.size());
        assertEquals(0, headers.get(0).get("id"));
        assertEquals(1, headers.get(1).get("date"));
        assertEquals(2, headers.get(2).get("location"));
        assertEquals(3, headers.get(3).get("mintemp"));
        assertEquals(4, headers.get(4).get("rainfall"));
        final List<Map<String, Object>> result = response.getResult();
        assertEquals(Instant.ofEpochSecond(1228089600), result.get(0).get("date"));
        assertEquals(0.6, result.get(0).get("rainfall"));
        assertEquals("Albury", result.get(0).get("location"));
        assertEquals(BigInteger.valueOf(1L), result.get(0).get("id"));
        assertEquals(13.4, result.get(0).get("mintemp"));
        assertEquals(Instant.ofEpochSecond(1228176000), result.get(1).get("date"));
        assertEquals(0.0, result.get(1).get("rainfall"));
        assertEquals("Albury", result.get(1).get("location"));
        assertEquals(BigInteger.valueOf(2L), result.get(1).get("id"));
        assertEquals(7.4, result.get(1).get("mintemp"));
        assertEquals(Instant.ofEpochSecond(1228262400), result.get(2).get("date"));
        assertEquals(0.0, result.get(2).get("rainfall"));
        assertEquals("Albury", result.get(2).get("location"));
        assertEquals(BigInteger.valueOf(3L), result.get(2).get("id"));
        assertEquals(12.9, result.get(2).get("mintemp"));
    }

    @Test
    public void getData_notFound_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableService.getData(TABLE_5_PRIVILEGED_DTO, null, 0L, 10L);
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

    @Test
    public void importDataset_withSeparatorAndQuoteAndNullElement_succeeds() throws SidecarImportException, ServiceException, SQLException,
            QueryMalformedException, RemoteUnavailableException, StorageNotFoundException, IOException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .location("weather_aus.csv")
                .separator(';')
                .quote('"')
                .nullElement("NA")
                .build();

        /* mock */
        final File source = new File("src/test/resources/csv/weather_aus.csv");
        final File target = new File("/tmp/weather_aus.csv");
        log.trace("copy dataset from {} to {}", source.toPath().toAbsolutePath(), target.toPath().toAbsolutePath());
        FileUtils.copyFile(source, target);
        doNothing()
                .when(dataDatabaseSidecarGateway)
                .importFile(anyString(), anyInt(), eq("weather_aus.csv"));

        /* test */
        tableService.importDataset(TABLE_1_PRIVILEGED_DTO, request);
    }

    @Test
    public void importDataset_malformedData_fails() throws ServiceException, RemoteUnavailableException, StorageNotFoundException,
            IOException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .location("weather_aus.csv")
                .separator(';')
                .quote('"')
                .build();

        /* mock */
        final File source = new File("src/test/resources/csv/weather_aus.csv");
        final File target = new File("/tmp/weather_aus.csv");
        log.trace("copy dataset from {} to {}", source.toPath().toAbsolutePath(), target.toPath().toAbsolutePath());
        FileUtils.copyFile(source, target);
        doNothing()
                .when(dataDatabaseSidecarGateway)
                .importFile(anyString(), anyInt(), eq("weather_aus.csv"));

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.importDataset(TABLE_1_PRIVILEGED_DTO, request);
        });
    }

    @Test
    public void exportDataset_succeeds() throws ServiceException, SQLException,
            QueryMalformedException, RemoteUnavailableException, StorageNotFoundException, StorageUnavailableException,
            SidecarExportException {
        final ExportResourceDto mock = ExportResourceDto.builder()
                .filename("weather_aus.csv")
                .resource(new InputStreamResource(InputStream.nullInputStream()))
                .build();

        /* mock */
        doNothing()
                .when(dataDatabaseSidecarGateway)
                .exportFile(anyString(), anyInt(), eq("weather_aus.csv"));
        when(storageService.getResource("weather_aus.csv"))
                .thenReturn(mock);

        /* test */
        final ExportResourceDto response = tableService.exportDataset(TABLE_1_PRIVILEGED_DTO, null);
    }

    @Test
    public void exportDataset_malformedData_fails() throws SQLException {

        /* mock */
        MariaDbConfig.createDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            tableService.exportDataset(TABLE_5_PRIVILEGED_DTO, null);
        });
    }

}
