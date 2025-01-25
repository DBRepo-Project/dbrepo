package at.tuwien.service;

import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.columns.CreateTableColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.CreateTableConstraintsDto;
import at.tuwien.api.database.table.constraints.foreign.CreateForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.tuwien.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.database.table.internal.TableCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
public class DatabaseServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private DatabaseService databaseService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException, InterruptedException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_2_DTO);
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @Test
    public void createView_succeeds() throws SQLException, ViewMalformedException {

        /* test */
        databaseService.createView(DATABASE_1_PRIVILEGED_DTO, VIEW_1_CREATE_DTO);
    }

    @Test
    public void exploreViews_succeeds() throws SQLException, ViewNotFoundException, DatabaseMalformedException {

        /* test */
        final List<ViewDto> response = databaseService.exploreViews(DATABASE_1_PRIVILEGED_DTO);
        final ViewDto view0 = response.get(0);
        assertEquals("not_in_metadata_db2", view0.getName());
        assertEquals("not_in_metadata_db2", view0.getInternalName());
        assertEquals(DATABASE_1_ID, view0.getVdbid());
        assertEquals(USER_1_BRIEF_DTO, view0.getOwner());
        assertFalse(view0.getIsInitialView());
        assertEquals(DATABASE_1_PUBLIC, view0.getIsPublic());
        assertEquals(DATABASE_1_SCHEMA_PUBLIC, view0.getIsSchemaPublic());
        assertTrue(view0.getQuery().length() >= 69);
        assertNotNull(view0.getQueryHash());
        assertEquals(4, view0.getColumns().size());
        final ViewColumnDto column0a = view0.getColumns().get(0);
        assertEquals("date", column0a.getInternalName());
        final ViewColumnDto column1a = view0.getColumns().get(1);
        assertEquals("location", column1a.getInternalName());
        final ViewColumnDto column2a = view0.getColumns().get(2);
        assertEquals("MinTemp", column2a.getInternalName());
        final ViewColumnDto column3a = view0.getColumns().get(3);
        assertEquals("Rainfall", column3a.getInternalName());
    }

    @Test
    public void update_succeeds() throws SQLException, DatabaseMalformedException {
        final UpdateUserPasswordDto request = UpdateUserPasswordDto.builder()
                .username(USER_1_USERNAME)
                .password(USER_2_PASSWORD)
                .build();

        /* mock */
        MariaDbConfig.grantWriteAccess(DATABASE_1_PRIVILEGED_DTO, USER_1_USERNAME);

        /* pre-condition */
        MariaDbConfig.mockQuery(CONTAINER_1_HOST, CONTAINER_1_PORT, DATABASE_1_INTERNALNAME, "CREATE SEQUENCE debug NOCACHE", USER_1_USERNAME, USER_1_PASSWORD);
        try {
            MariaDbConfig.mockQuery(CONTAINER_1_HOST, CONTAINER_1_PORT, DATABASE_1_INTERNALNAME, "CREATE SEQUENCE debug NOCACHE", USER_1_USERNAME, USER_2_PASSWORD);
            fail();
        } catch (SQLException e) {
            /* ignore */
        }

        /* test */
        databaseService.update(DATABASE_1_PRIVILEGED_DTO, request);
        MariaDbConfig.mockQuery(CONTAINER_1_HOST, CONTAINER_1_PORT, DATABASE_1_INTERNALNAME, "CREATE SEQUENCE debug2 NOCACHE", USER_1_USERNAME, USER_2_PASSWORD);
    }

    @Test
    public void update_notExists_fails() {
        final UpdateUserPasswordDto request = UpdateUserPasswordDto.builder()
                .username("i_do_not_exist")
                .password(USER_1_PASSWORD)
                .build();

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseService.update(DATABASE_1_PRIVILEGED_DTO, request);
        });
    }

    @Test
    public void inspectTable_sameNameDifferentDb_succeeds() throws TableNotFoundException, SQLException {

        /* mock */
        MariaDbConfig.execute(DATABASE_2_PRIVILEGED_DTO, "CREATE TABLE not_in_metadata_db (wrong_id BIGINT NOT NULL PRIMARY KEY, given_name VARCHAR(255) NOT NULL, middle_name VARCHAR(255), family_name VARCHAR(255) NOT NULL, age INT NOT NULL) WITH SYSTEM VERSIONING;");

        /* test */
        final TableDto response = databaseService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "not_in_metadata_db");
        assertEquals("not_in_metadata_db", response.getInternalName());
        assertEquals("not_in_metadata_db", response.getName());
        assertEquals(DATABASE_1_ID, response.getTdbid());
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
        final TableDto response = databaseService.inspectTable(DATABASE_2_PRIVILEGED_DTO, "experiments");
        assertEquals("experiments", response.getInternalName());
        assertEquals("experiments", response.getName());
        assertEquals(DATABASE_2_ID, response.getTdbid());
        assertTrue(response.getIsVersioned());
        assertEquals(DATABASE_2_PUBLIC, response.getIsPublic());
        assertNotNull(response.getOwner());
        assertEquals(DATABASE_2_OWNER, response.getOwner().getId());
        assertEquals(USER_2_NAME, response.getOwner().getName());
        assertEquals(USER_2_USERNAME, response.getOwner().getUsername());
        assertEquals(USER_2_FIRSTNAME, response.getOwner().getFirstname());
        assertEquals(USER_2_LASTNAME, response.getOwner().getLastname());
        assertEquals(USER_2_QUALIFIED_NAME, response.getOwner().getQualifiedName());
        final List<IdentifierDto> identifiers = response.getIdentifiers();
        assertNotNull(identifiers);
        assertEquals(0, identifiers.size());
        final List<ColumnDto> columns = response.getColumns();
        assertNotNull(columns);
        assertEquals(3, columns.size());
        assertColumn(columns.get(0), null, null, DATABASE_2_ID, "id", "id", ColumnTypeDto.BIGINT, 19L, 0L, false, null);
        assertColumn(columns.get(1), null, null, DATABASE_2_ID, "mode", "mode", ColumnTypeDto.ENUM, 3L, null, false, null);
        assertEquals(2, columns.get(1).getEnums().size());
        assertEquals(List.of("ABC", "DEF"), columns.get(1).getEnums());
        assertColumn(columns.get(2), null, null, DATABASE_2_ID, "seq", "seq", ColumnTypeDto.SET, 5L, null, true, null);
        assertEquals(3, columns.get(2).getSets().size());
        assertEquals(List.of("1", "2", "3"), columns.get(2).getSets());
        /* ignore rest (constraints) */
    }

    @Test
    public void inspectTableFullConstraints_succeeds() throws TableNotFoundException, SQLException {

        /* test */
        final TableDto response = databaseService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "weather_aus");
        assertEquals("weather_aus", response.getInternalName());
        assertEquals("weather_aus", response.getName());
        assertEquals(DATABASE_1_ID, response.getTdbid());
        assertTrue(response.getIsVersioned());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        assertNotNull(response.getOwner());
        assertEquals(USER_1_BRIEF_DTO, response.getOwner());
        assertEquals(USER_1_NAME, response.getOwner().getName());
        assertEquals(USER_1_USERNAME, response.getOwner().getUsername());
        assertEquals(USER_1_FIRSTNAME, response.getOwner().getFirstname());
        assertEquals(USER_1_LASTNAME, response.getOwner().getLastname());
        assertEquals(USER_1_QUALIFIED_NAME, response.getOwner().getQualifiedName());
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
        assertEquals(TABLE_2_INTERNALNAME, fk0.getReferencedTable().getName());
        assertEquals(TABLE_2_INTERNALNAME, fk0.getReferencedTable().getInternalName());
    }

    @Test
    public void inspectTable_multipleForeignKeyReferences_succeeds() throws TableNotFoundException, SQLException {

        /* test */
        final TableDto response = databaseService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "complex_foreign_keys");
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
        final TableDto response = databaseService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "complex_primary_key");
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
        final TableDto response = databaseService.inspectTable(DATABASE_1_PRIVILEGED_DTO, "exotic_boolean");
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
    public void inspectView_succeeds() throws SQLException, ViewNotFoundException {

        /* test */
        final ViewDto response = databaseService.inspectView(DATABASE_1_PRIVILEGED_DTO, "not_in_metadata_db2");
        assertEquals("not_in_metadata_db2", response.getInternalName());
        assertEquals("not_in_metadata_db2", response.getName());
        assertEquals(DATABASE_1_ID, response.getVdbid());
        assertEquals(USER_1_BRIEF_DTO, response.getOwner());
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

    @Test
    public void getSchemas_succeeds() throws TableNotFoundException, SQLException, DatabaseMalformedException {

        /* test */
        final List<TableDto> response = databaseService.exploreTables(DATABASE_1_PRIVILEGED_DTO);
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
    public void createTable_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {

        /* test */
        final TableDto response = databaseService.createTable(DATABASE_1_PRIVILEGED_DTO, TABLE_4_CREATE_INTERNAL_DTO);
        assertEquals(TABLE_4_INTERNALNAME, response.getName());
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
    public void createTable_malformed_fails() {
        final at.tuwien.api.database.table.internal.TableCreateDto request = TableCreateDto.builder()
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
            databaseService.createTable(DATABASE_1_PRIVILEGED_DTO, request);
        });
    }

    @Test
    public void createTable_compositePrimaryKey_fails() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {
        final at.tuwien.api.database.table.internal.TableCreateDto request = TableCreateDto.builder()
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
        final TableDto response = databaseService.createTable(DATABASE_1_PRIVILEGED_DTO, request);
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
    public void createTable_needSequence_succeeds() throws TableNotFoundException, TableMalformedException, SQLException,
            TableExistsException {

        /* mock */
        MariaDbConfig.dropTable(DATABASE_1_PRIVILEGED_DTO, TABLE_1_INTERNAL_NAME);

        /* test */
        final TableDto response = databaseService.createTable(DATABASE_1_PRIVILEGED_DTO, TABLE_1_CREATE_INTERNAL_DTO);
        assertEquals(TABLE_1_INTERNAL_NAME, response.getName());
        assertEquals(TABLE_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(TABLE_1_COLUMNS.size(), response.getColumns().size());
    }

    protected static void assertViewColumn(ViewColumnDto column, ViewColumnDto other) {
        assertNotNull(column);
        assertNotNull(other);
        assertEquals(column.getId(), other.getId());
        assertEquals(column.getDatabaseId(), other.getDatabaseId());
        assertEquals(column.getName(), other.getName());
        assertEquals(column.getInternalName(), other.getInternalName());
        assertEquals(column.getColumnType(), other.getColumnType());
        assertEquals(column.getSize(), other.getSize());
        assertEquals(column.getD(), other.getD());
        assertEquals(column.getIsNullAllowed(), other.getIsNullAllowed());
        assertEquals(column.getDescription(), other.getDescription());
    }

    protected static void assertColumn(ColumnDto column, Long id, Long tableId, Long databaseId, String name,
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
