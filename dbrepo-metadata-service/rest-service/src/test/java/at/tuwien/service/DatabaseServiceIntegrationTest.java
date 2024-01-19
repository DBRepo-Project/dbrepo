package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.*;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.config.QueryConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.impl.MariaDbServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class DatabaseServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private QueryConfig queryConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @Autowired
    private MariaDbConfig mariaDbConfig;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException, IOException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        TABLE_8.setColumns(TABLE_8_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2, CONTAINER_3));
        DATABASE_1.setAccesses(List.of());
        DATABASE_2.setAccesses(List.of());
        DATABASE_3.setAccesses(List.of(DATABASE_1_USER_3_READ_ACCESS));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3));
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_2);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_3);
    }

    @Test
    public void find_succeeds() throws DatabaseNotFoundException {

        /* test */
        final Database response = databaseService.find(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.find(9999L);
        });
    }

    @Test
    public void create_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_create(DATABASE_1_CREATE, DATABASE_1);
    }

    @Test
    public void create_sameName_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_create(DATABASE_1_CREATE, DATABASE_1);
        generic_create(DATABASE_1_CREATE, DATABASE_1);
    }

    @Test
    public void create_inSequence_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_2_INTERNALNAME);
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_3_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_2_DTO)
                .thenReturn(DATABASE_3_DTO);
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_create(DATABASE_2_CREATE, DATABASE_2);
        generic_create(DATABASE_3_CREATE, DATABASE_3);
    }

    @Test
    public void create_outOfSequence_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_2_INTERNALNAME);
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_3_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_3_DTO)
                .thenReturn(DATABASE_2_DTO);
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_create(DATABASE_3_CREATE, DATABASE_3);
        generic_create(DATABASE_2_CREATE, DATABASE_2);
    }

    @Test
    public void create_canLogin_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");
        final Database database = generic_create(DATABASE_1_CREATE, DATABASE_1);


        /* test */
        MariaDbConfig.getPrivileges(mariaDBContainer.getHost(), 3308, database.getInternalName(), USER_1_USERNAME, USER_1_PASSWORD);
    }

    @Test
    public void create_existsRollbackSucceeds_fails() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(queryConfig.getGrantPrivileges())
                .thenReturn("" /* (1) */, "SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE"/* (2) */);

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseService.create(DATABASE_1_CREATE, USER_1_PRINCIPAL); // (1)
        });
        generic_create(DATABASE_1_CREATE, DATABASE_1); // (2)
    }

    @Test
    public void updatePassword_canLogin_succeeds() throws Exception {

        /* mock */
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        assertThrows(SQLInvalidAuthorizationSpecException.class, () -> {
            MariaDbConfig.getPrivileges(mariaDBContainer.getHost(), 3308, USER_3_USERNAME, USER_4_PASSWORD);
        });
        databaseService.updatePassword(User.builder()
                .id(USER_3_ID)
                .username(USER_3_USERNAME)
                .mariadbPassword(USER_4_DATABASE_PASSWORD)
                .build());
        MariaDbConfig.getPrivileges(mariaDBContainer.getHost(), 3308, USER_3_USERNAME, USER_4_PASSWORD);
    }

    @Test
    public void create_queryStore_succeeds() throws Exception {

        /* mock */
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_queryStoreSameQueryHash_succeeds() throws Exception {

        /* mock */
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
        generic_insert(QUERY_5_STATEMENT, 2L);
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_systemProcedure_succeeds() throws Exception {

        /* mock */
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_system_insert(CONTAINER_1_PRIVILEGED_USERNAME, UUID.randomUUID(), CONTAINER_1_PRIVILEGED_PASSWORD);
    }

    @Test
    public void create_systemProcedure_fails() {

        /* mock */
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        assertThrows(SQLException.class, () -> {
            generic_system_insert(USER_1_USERNAME, USER_1_ID, USER_1_PASSWORD);
        });
    }

    @Test
    public void create_userProcedureRoot_succeeds() throws SQLException, QueryMalformedException {

        /* mock */
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_user_insert(CONTAINER_1_PRIVILEGED_USERNAME, CONTAINER_1_PRIVILEGED_PASSWORD);
    }

    @Test
    public void create_userProcedureUser_succeeds() throws SQLException, QueryMalformedException {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_3_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_3);
        mariaDbConfig.grantUserPermissions(CONTAINER_1, DATABASE_3, "junit1");
        when(queryConfig.getGrantPrivileges())
                .thenReturn("SELECT, CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE");

        /* test */
        generic_user_insert("junit1", "junit1");
    }

    @Test
    public void visibility_succeeds() throws DatabaseNotFoundException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        final Database response = databaseService.visibility(DATABASE_1_ID, request);
        assertTrue(response.getIsPublic());
    }

    @Test
    public void transfer_succeeds() throws DatabaseNotFoundException, UserNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_2_USERNAME)
                .build();

        /* test */
        final Database response = databaseService.transfer(DATABASE_1_ID, request);
        assertEquals(USER_2_ID, response.getOwnedBy());
    }

    @Test
    public void obtainMetadata_tableWithoutVersioning_succeeds() throws DatabaseUnchangedException, QueryMalformedException,
            DatabaseNotFoundException, ColumnParseException {

        /* test */
        final Database response = databaseService.obtainMetadata(DATABASE_1_ID);
        final List<Table> tables = response.getTables();
        assertEquals(7, tables.size());
        final Optional<Table> optional3 = tables.stream().filter(t -> t.getInternalName().equals("weather_aut_without_versioning")).findFirst();
        assertTrue(optional3.isPresent());
        final Table table3 = optional3.get();
        assertEquals(5, table3.getColumns().size());
        assertColumn(table3.getColumns().get(0), 0, "id", TableColumnType.BIGINT, null, false, true, false);
        assertColumn(table3.getColumns().get(1), 1, "date", TableColumnType.DATE, null, false, false, false);
        assertColumn(table3.getColumns().get(2), 2, "location", TableColumnType.VARCHAR, 255L, true, false, false);
        assertColumn(table3.getColumns().get(3), 3, "mintemp", TableColumnType.DOUBLE, null, true, false, false);
        assertColumn(table3.getColumns().get(4), 4, "rainfall", TableColumnType.DOUBLE, null, true, false, false);
    }

    @Test
    public void obtainMetadata_tableWithVersioning_succeeds() throws DatabaseUnchangedException, QueryMalformedException,
            DatabaseNotFoundException, ColumnParseException {

        /* test */
        final Database response = databaseService.obtainMetadata(DATABASE_1_ID);
        final List<Table> tables = response.getTables();
        assertEquals(7, tables.size());
        final Optional<Table> optional4 = tables.stream().filter(t -> t.getInternalName().equals("weather_aut")).findFirst();
        assertTrue(optional4.isPresent());
        final Table table4 = optional4.get();
        assertEquals("weather_aut", table4.getName());
        assertEquals(5, table4.getColumns().size());
        assertColumn(table4.getColumns().get(0), 0, "id", TableColumnType.BIGINT, null, false, true, true);
        assertColumn(table4.getColumns().get(1), 1, "date", TableColumnType.DATE, null, false, false, false);
        assertColumn(table4.getColumns().get(2), 2, "location", TableColumnType.VARCHAR, 255L, true, false, false);
        assertColumn(table4.getColumns().get(3), 3, "mintemp", TableColumnType.DOUBLE, null, true, false, false);
        assertColumn(table4.getColumns().get(4), 4, "rainfall", TableColumnType.DOUBLE, null, true, false, false);
    }

    @Test
    public void obtainMetadata_view_succeeds() throws DatabaseUnchangedException, QueryMalformedException,
            DatabaseNotFoundException, ColumnParseException {

        /* test */
        final Database response = databaseService.obtainMetadata(DATABASE_1_ID);
        final List<Table> tables = response.getTables();
        assertEquals(7, tables.size());
        final List<View> views = response.getViews();
        log.debug("found {} views: {}", views.size(), views.stream().map(View::getInternalName).toList());
        assertEquals(4, views.size());
        final Optional<View> optional1 = views.stream().filter(v -> v.getInternalName().equals("weather_aut_merge")).findFirst();
        assertTrue(optional1.isPresent());
        final View view1 = optional1.get();
        assertEquals("weather_aut_merge", view1.getInternalName());
        assertEquals("weather_aut_merge", view1.getName());
        assertEquals(DATABASE_1_PUBLIC, view1.getIsPublic());
        assertFalse(view1.getIsInitialView());
        assertEquals(DATABASE_1_OWNER, view1.getCreatedBy());
        assertNotNull(view1.getQuery());
        assertNotNull(view1.getQueryHash());
        assertColumn(view1.getColumns().get(0), 0, "id", TableColumnType.BIGINT, null, false, true, true);
        assertColumn(view1.getColumns().get(1), 1, "date", TableColumnType.DATE, null, false, false, false);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_insert(String query, Long assertQueryId) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.grantUserPermissions(CONTAINER_1, DATABASE_3, USER_1_USERNAME);

        /* test */
        final Long response = MariaDbConfig.mockSystemQueryInsert(DATABASE_3, query);
        assertNotNull(response);
        assertEquals(assertQueryId, response);
    }

    protected Database generic_create(DatabaseCreateDto createDto, Database database) throws Exception {

        /* test */
        final Database response = databaseService.create(createDto, USER_1_PRINCIPAL);
        assertEquals(database.getName(), response.getName());
        assertTrue(response.getInternalName().startsWith(database.getInternalName()));
        return response;
    }

    protected void generic_system_insert(String username, UUID userId, String password) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.grantUserPermissions(CONTAINER_1, DATABASE_3, USER_1_USERNAME);

        /* test */
        final Long queryId = MariaDbConfig.mockSystemQueryInsert(DATABASE_3, QUERY_4_STATEMENT, username, userId, password);
        assertEquals(1L, queryId);
    }

    protected void generic_user_insert(String username, String password) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.grantUserPermissions(CONTAINER_1, DATABASE_3, USER_1_USERNAME);

        /* test */
        final Long queryId = MariaDbConfig.mockUserQueryInsert(DATABASE_3, QUERY_4_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

    public void assertColumn(TableColumn column, Integer ordinalPosition, String columnName, TableColumnType type,
                             Long size, Boolean isNullAllowed, Boolean isPrimary, Boolean isAutoGenerated) {
        assertEquals(ordinalPosition, column.getOrdinalPosition());
        assertEquals(columnName, column.getName());
        assertEquals(columnName, column.getInternalName());
        assertEquals(type, column.getColumnType());
        if (size != null) {
            assertEquals(size, column.getSize());
        }
        assertEquals(isNullAllowed, column.getIsNullAllowed());
        assertEquals(isPrimary, column.getIsPrimaryKey());
        assertEquals(isAutoGenerated, column.getAutoGenerated());
    }

}
