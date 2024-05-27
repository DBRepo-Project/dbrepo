package at.tuwien.service;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.TupleDeleteDto;
import at.tuwien.api.database.table.TupleDto;
import at.tuwien.api.database.table.TupleUpdateDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
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

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
    }

    @Test
    public void updateTuple_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
    public void updateTuple_modifyPrimaryKey_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
    public void updateTuple_missingPrimaryKey_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
    public void deleteTuple_withoutPrimaryKey_succeeds() throws InterruptedException, SQLException, RemoteUnavailableException,
            ContainerNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException {
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
        final TableDto table0 = response.get(0);
        Assertions.assertEquals("not_in_metadata_db", table0.getInternalName());
        Assertions.assertEquals("not_in_metadata_db", table0.getName());
        Assertions.assertEquals(DATABASE_1_ID, table0.getTdbid());
        assertTrue(table0.getIsVersioned());
        Assertions.assertEquals(DATABASE_1_PUBLIC, table0.getIsPublic());
        final List<ColumnDto> columns = table0.getColumns();
        assertNotNull(columns);
        Assertions.assertEquals(5, columns.size());
        final ColumnDto column0 = columns.get(0);
        Assertions.assertEquals("id", column0.getName());
        Assertions.assertEquals("id", column0.getInternalName());
        Assertions.assertEquals(ColumnTypeDto.BIGINT, column0.getColumnType());
        assertFalse(column0.getIsNullAllowed());
        final ColumnDto column1 = columns.get(1);
        Assertions.assertEquals("given_name", column1.getName());
        Assertions.assertEquals("given_name", column1.getInternalName());
        Assertions.assertEquals(ColumnTypeDto.VARCHAR, column1.getColumnType());
        Assertions.assertEquals(255, column1.getSize());
        assertFalse(column1.getIsNullAllowed());
        final ColumnDto column2 = columns.get(2);
        Assertions.assertEquals("middle_name", column2.getName());
        Assertions.assertEquals("middle_name", column2.getInternalName());
        Assertions.assertEquals(ColumnTypeDto.VARCHAR, column2.getColumnType());
        Assertions.assertEquals(255, column2.getSize());
        assertTrue(column2.getIsNullAllowed());
        final ColumnDto column3 = columns.get(3);
        Assertions.assertEquals("family_name", column3.getName());
        Assertions.assertEquals("family_name", column3.getInternalName());
        Assertions.assertEquals(ColumnTypeDto.VARCHAR, column3.getColumnType());
        Assertions.assertEquals(255, column3.getSize());
        assertFalse(column3.getIsNullAllowed());
        final ColumnDto column4 = columns.get(4);
        Assertions.assertEquals("age", column4.getName());
        Assertions.assertEquals("age", column4.getInternalName());
        Assertions.assertEquals(ColumnTypeDto.INT, column4.getColumnType());
        assertFalse(column4.getIsNullAllowed());
        final ConstraintsDto constraints = table0.getConstraints();
        assertNotNull(constraints);
        final Set<PrimaryKeyDto> primaryKey = constraints.getPrimaryKey();
        Assertions.assertEquals(1, primaryKey.size());
        final Set<String> checks = constraints.getChecks();
        Assertions.assertEquals(1, checks.size());
        Assertions.assertEquals(Set.of("`age` > 0 and `age` < 120"), checks);
        final List<UniqueDto> uniques = constraints.getUniques();
        Assertions.assertEquals(1, uniques.size());
        Assertions.assertEquals(2, uniques.get(0).getColumns().size());
        Assertions.assertEquals("not_in_metadata_db", uniques.get(0).getTable().getInternalName());
        Assertions.assertEquals("given_name", uniques.get(0).getColumns().get(0).getInternalName());
        Assertions.assertEquals("family_name", uniques.get(0).getColumns().get(1).getInternalName());
    }

}
