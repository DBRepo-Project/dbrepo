package at.tuwien.service;

import at.tuwien.api.container.image.ImageDateDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.tuwien.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.identifier.IdentifierDto;
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
import java.util.LinkedList;
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
        assertColumn(columns.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
        assertColumn(columns.get(1), null, null, DATABASE_1_ID, "given_name", "given_name", ColumnTypeDto.VARCHAR, 255L, null, false, null, null);
        assertColumn(columns.get(2), null, null, DATABASE_1_ID, "middle_name", "middle_name", ColumnTypeDto.VARCHAR, 255L, null, true, null, null);
        assertColumn(columns.get(3), null, null, DATABASE_1_ID, "family_name", "family_name", ColumnTypeDto.VARCHAR, 255L, null, false, null, null);
        assertColumn(columns.get(4), null, null, DATABASE_1_ID, "age", "age", ColumnTypeDto.INT, 10L, 0L, false, null, null);
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
    public void inspectTableFullConstraints_succeeds() throws TableNotFoundException, SQLException, QueryMalformedException {

        /* test */
        final TableDto response = schemaService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "weather_aus");
        assertEquals("weather_aus", response.getInternalName());
        assertEquals("weather_aus", response.getName());
        assertEquals(DATABASE_1_ID, response.getTdbid());
        assertTrue(response.getIsVersioned());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        assertEquals(DATABASE_1_OWNER, response.getCreatedBy());
        assertNotNull(response.getCreator());
        assertEquals(DATABASE_1_OWNER, response.getCreator().getId());
        assertEquals(USER_1_NAME, response.getCreator().getName());
        assertEquals(USER_1_USERNAME, response.getCreator().getUsername());
        assertEquals(USER_1_FIRSTNAME, response.getCreator().getFirstname());
        assertEquals(USER_1_LASTNAME, response.getCreator().getLastname());
        assertEquals(USER_1_QUALIFIED_NAME, response.getCreator().getQualifiedName());
        assertNotNull(response.getCreator().getAttributes());
        assertEquals(USER_1_AFFILIATION, response.getCreator().getAttributes().getAffiliation());
        assertEquals(USER_1_THEME, response.getCreator().getAttributes().getTheme());
        assertEquals(USER_1_LANGUAGE, response.getCreator().getAttributes().getLanguage());
        assertEquals(USER_1_ORCID_UNCOMPRESSED, response.getCreator().getAttributes().getOrcid());
        assertNull(response.getCreator().getAttributes().getMariadbPassword());
        final List<IdentifierDto> identifiers = response.getIdentifiers();
        assertNotNull(identifiers);
        assertEquals(0, identifiers.size());
        final List<ColumnDto> columns = response.getColumns();
        assertNotNull(columns);
        assertEquals(5, columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_1_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null, null);
        assertColumn(columns.get(1), null, null, DATABASE_1_ID, "date", "date", ColumnTypeDto.DATE, null, null, false, IMAGE_DATE_1_ID, null);
        assertColumn(columns.get(2), null, null, DATABASE_1_ID, "location", "location", ColumnTypeDto.VARCHAR, 255L, null, true, null, "Closest city");
        assertColumn(columns.get(3), null, null, DATABASE_1_ID, "mintemp", "mintemp", ColumnTypeDto.DOUBLE, 22L, null, true, null, null);
        assertColumn(columns.get(4), null, null, DATABASE_1_ID, "rainfall", "rainfall", ColumnTypeDto.DOUBLE, 22L, null, true, null, null);
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
        assertNull(unique0.getTable().getId());
        assertEquals(TABLE_1_INTERNALNAME, unique0.getTable().getName());
        assertEquals(TABLE_1_INTERNALNAME, unique0.getTable().getInternalName());
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
        assertEquals(TABLE_1_INTERNALNAME, fk0table.getName());
        assertEquals(TABLE_1_INTERNALNAME, fk0table.getInternalName());
        assertNotNull(fk0.getOnDelete());
        assertNotNull(fk0.getOnUpdate());
        assertNotNull(fk0.getReferencedTable());
        assertEquals(TABLE_2_INTERNALNAME, fk0.getReferencedTable().getName());
        assertEquals(TABLE_2_INTERNALNAME, fk0.getReferencedTable().getInternalName());
    }

    @Test
    public void inspectTable_multipleForeignKeyReferences_succeeds() throws TableNotFoundException, SQLException, QueryMalformedException {

        /* test */
        final TableDto response = schemaService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "complex_foreign_keys");
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
    public void inspectTable_multiplePrimaryKey_succeeds() throws TableNotFoundException, SQLException, QueryMalformedException {

        /* test */
        final TableDto response = schemaService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "complex_primary_key");
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

    protected static void assertViewColumn(ViewColumnDto column, Long id, Long databaseId, String name, String internalName,
                                           ColumnTypeDto type, Long size, Long d, Boolean nullAllowed,
                                           ImageDateDto dateFormat, String description) {
        log.trace("assert column: {}", internalName);
        assertNotNull(column);
        assertEquals(id, column.getId());
        assertEquals(databaseId, column.getDatabaseId());
        assertEquals(name, column.getName());
        assertEquals(internalName, column.getInternalName());
        assertEquals(type, column.getColumnType());
        assertEquals(size, column.getSize());
        assertEquals(d, column.getD());
        assertEquals(nullAllowed, column.getIsNullAllowed());
        assertEquals(description, column.getDescription());
        if (dateFormat != null) {
            assertNotNull(column.getDateFormat());
            assertEquals(dateFormat.getId(), column.getDateFormat().getId());
        } else {
            assertNull(column.getDateFormat());
        }
    }

    protected static void assertColumn(ColumnDto column, Long id, Long tableId, Long databaseId, String name,
                                           String internalName, ColumnTypeDto type, Long size, Long d, Boolean nullAllowed,
                                           Long dfid, String description) {
        log.trace("assert column: {}", internalName);
        assertNotNull(column);
        assertEquals(id, column.getId());
        assertEquals(tableId, column.getTableId());
        assertEquals(databaseId, column.getDatabaseId());
        assertNotNull(column.getTable());
        assertEquals(tableId, column.getTable().getId());
        assertEquals(name, column.getName());
        assertEquals(internalName, column.getInternalName());
        assertEquals(type, column.getColumnType());
        assertEquals(size, column.getSize());
        assertEquals(d, column.getD());
        assertEquals(nullAllowed, column.getIsNullAllowed());
        assertEquals(description, column.getDescription());
        if (dfid != null) {
            assertNotNull(column.getDateFormat());
            assertEquals(dfid, column.getDateFormat().getId());
        } else {
            assertNull(column.getDateFormat());
        }
    }

}
