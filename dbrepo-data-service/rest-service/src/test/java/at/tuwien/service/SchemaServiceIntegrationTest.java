package at.tuwien.service;

import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class SchemaServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private SchemaService schemaService;

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
    public void inspectTable_succeeds() throws TableNotFoundException, SQLException, QueryMalformedException {

        /* test */
        final TableDto response = schemaService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "not_in_metadata_db");
        assertEquals("not_in_metadata_db", response.getInternalName());
        assertEquals("not_in_metadata_db", response.getName());
        assertEquals(DATABASE_1_ID, response.getTdbid());
        assertTrue(response.getIsVersioned());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        final List<ColumnDto> columns = response.getColumns();
        assertNotNull(columns);
        assertEquals(5, columns.size());
        final ColumnDto column0 = columns.get(0);
        assertEquals("id", column0.getName());
        assertEquals("id", column0.getInternalName());
        assertEquals(ColumnTypeDto.BIGINT, column0.getColumnType());
        assertFalse(column0.getIsNullAllowed());
        final ColumnDto column1 = columns.get(1);
        assertEquals("given_name", column1.getName());
        assertEquals("given_name", column1.getInternalName());
        assertEquals(ColumnTypeDto.VARCHAR, column1.getColumnType());
        assertEquals(255, column1.getSize());
        assertFalse(column1.getIsNullAllowed());
        final ColumnDto column2 = columns.get(2);
        assertEquals("middle_name", column2.getName());
        assertEquals("middle_name", column2.getInternalName());
        assertEquals(ColumnTypeDto.VARCHAR, column2.getColumnType());
        assertEquals(255, column2.getSize());
        assertTrue(column2.getIsNullAllowed());
        final ColumnDto column3 = columns.get(3);
        assertEquals("family_name", column3.getName());
        assertEquals("family_name", column3.getInternalName());
        assertEquals(ColumnTypeDto.VARCHAR, column3.getColumnType());
        assertEquals(255, column3.getSize());
        assertFalse(column3.getIsNullAllowed());
        final ColumnDto column4 = columns.get(4);
        assertEquals("age", column4.getName());
        assertEquals("age", column4.getInternalName());
        assertEquals(ColumnTypeDto.INT, column4.getColumnType());
        assertFalse(column4.getIsNullAllowed());
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
        assertEquals("not_in_metadata_db", uniques.get(0).getTable().getInternalName());
        assertEquals("given_name", uniques.get(0).getColumns().get(0).getInternalName());
        assertEquals("family_name", uniques.get(0).getColumns().get(1).getInternalName());
    }

    @Test
    public void inspectView_succeeds() throws ViewMalformedException, SQLException, ViewNotFoundException,
            ViewSchemaException {

        /* test */
        final ViewDto response = schemaService.inspectView(DATABASE_1_PRIVILEGED_DTO, "not_in_metadata_db2");
        assertEquals("not_in_metadata_db2", response.getInternalName());
        assertEquals("not_in_metadata_db2", response.getName());
        assertEquals(DATABASE_1_ID, response.getVdbid());
        assertEquals(DATABASE_1_ID, response.getDatabase().getId());
        assertEquals(DATABASE_1_OWNER, response.getCreatedBy());
        assertEquals(DATABASE_1_OWNER, response.getCreator().getId());
        assertFalse(response.getIsInitialView());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        assertTrue(response.getQuery().length() >= 69);
        assertNotNull(response.getQueryHash());
        assertEquals(4, response.getColumns().size());
        final ViewColumnDto column0 = response.getColumns().get(0);
        assertNotNull(column0.getName());
        assertEquals("date", column0.getInternalName());
        assertEquals(DATABASE_1_ID, column0.getDatabaseId());
        final ViewColumnDto column1 = response.getColumns().get(1);
        assertNotNull(column1.getName());
        assertEquals("location", column1.getInternalName());
        assertEquals(DATABASE_1_ID, column1.getDatabaseId());
        final ViewColumnDto column2 = response.getColumns().get(2);
        assertNotNull(column2.getName());
        assertEquals("MinTemp", column2.getInternalName());
        assertEquals(DATABASE_1_ID, column2.getDatabaseId());
        final ViewColumnDto column3 = response.getColumns().get(3);
        assertNotNull(column3.getName());
        assertEquals("Rainfall", column3.getInternalName());
        assertEquals(DATABASE_1_ID, column3.getDatabaseId());
    }

}
