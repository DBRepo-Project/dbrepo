package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.repository.jpa.UserRepository;
import com.rabbitmq.client.Channel;
import at.tuwien.config.DockerConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.security.Principal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude= RabbitAutoConfiguration.class)
@SpringBootTest
public class StoreServiceIntegrationReadTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private StoreService storeService;

    final static String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() throws InterruptedException, SQLException {
        afterAll();
        /* create networks */
        DockerConfig.createAllNetworks();
        /* create user container */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        /* insert query */
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_1, USER_1_USERNAME);
    }

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        userRepository.save(USER_5);
        DATABASE_1.setTables(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_7));
        DATABASE_1.setViews(List.of(VIEW_3));
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void findAll_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, ContainerNotFoundException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final List<Query> queries = storeService.findAll(CONTAINER_1_ID, DATABASE_1_ID, null, USER_1_PRINCIPAL);
        assertEquals(1, queries.size());
    }

    @Test
    public void findAll_filterPersisted_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, ContainerNotFoundException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final List<Query> queries = storeService.findAll(CONTAINER_1_ID, DATABASE_1_ID, true, USER_1_PRINCIPAL);
        assertEquals(0, queries.size());
    }

    @Test
    public void findOne_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void findOne_notFound_succeeds() {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_notFound_fails() {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_2_ID, principal);
        });
    }

}
