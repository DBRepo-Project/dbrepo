package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class TableServiceIntegrationReadTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private UserRepository userRepository;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        DATABASE_1.setAccesses(List.of());
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    @Transactional(readOnly = true)
    public void findAll_succeeds() throws DatabaseNotFoundException {

        /* test */
        final List<Table> response = tableService.findAll(DATABASE_1_ID);
        assertEquals(4, response.size());
    }

    @Test
    public void findAll_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findAll(DATABASE_2_ID);
        });
    }

    @Test
    public void findById_succeeds() throws TableNotFoundException, DatabaseNotFoundException{

        /* test */
        final Table response = tableService.find(DATABASE_1_ID, TABLE_1_ID);
        assertEquals(TABLE_1_ID, response.getId());
        assertEquals(TABLE_1_NAME, response.getName());
        assertEquals(TABLE_1_INTERNALNAME, response.getInternalName());
    }

    @Test
    public void findById_tableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.find(DATABASE_1_ID, 99999L);
        });
    }

    @Test
    public void findById_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.find(99999L, TABLE_3_ID);
        });
    }

    @Test
    public void findHistory_anonymous_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(DATABASE_1_ID, TABLE_1_ID, null);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithAnonymousUser
    public void findHistory_anonymous2_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(DATABASE_1_ID, TABLE_1_ID, null);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findHistory_researcher_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findHistory_developer_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(DATABASE_1_ID, TABLE_1_ID, USER_2_PRINCIPAL);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findHistory_dataSteward_succeeds() throws UserNotFoundException, TableNotFoundException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        final List<TableHistoryDto> response = tableService.findHistory(DATABASE_1_ID, TABLE_1_ID, USER_3_PRINCIPAL);
        assertEquals(1, response.size());
        final TableHistoryDto history = response.get(0);
        assertEquals("INSERT", history.getEvent());
    }

}
