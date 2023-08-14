package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.*;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class TableServiceIntegrationReadTest extends BaseUnitTest {

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableService tableService;

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        imageRepository.save(IMAGE_1);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        tableRepository.save(TABLE_1_SIMPLE);
        tableRepository.save(TABLE_2_SIMPLE);
        tableRepository.save(TABLE_3_SIMPLE);
        tableRepository.save(TABLE_7_SIMPLE);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
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
        final Table response = tableService.findById(DATABASE_1_ID, TABLE_1_ID);
        assertEquals(TABLE_1_ID, response.getId());
        assertEquals(TABLE_1_NAME, response.getName());
        assertEquals(TABLE_1_INTERNALNAME, response.getInternalName());
    }

    @Test
    public void findById_tableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.findById(DATABASE_1_ID, 99999L);
        });
    }

    @Test
    public void findById_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findById(99999L, TABLE_3_ID);
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
