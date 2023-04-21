package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.*;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.security.Principal;
import java.sql.SQLException;
import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude= RabbitAutoConfiguration.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class StoreServiceIntegrationModifyTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    /* keep */
    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    /* keep */
    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private StoreService storeService;

    final static String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @Autowired
    private QueryService queryService;

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        DockerConfig.createAllNetworks();
    }

    @BeforeEach
    public void beforeEach() throws InterruptedException {
        afterEach();
        /* create networks */
        DockerConfig.createAllNetworks();
        /* create containers */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
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

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void insert_same_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_1, USER_1_USERNAME);

        /* test */
        final Query response = storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        log.debug("found queries in query store: {}", MariaDbConfig.selectQuery(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME,
                "SELECT `query_normalized`, `query_hash`, `result_hash` FROM `qs_queries`", "query_normalized", "query_hash", "result_hash"));
        assertEquals(QUERY_1_ID, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_different_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException {
        final ExecuteStatementDto mock = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, mock, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(2L, response.getId()) /* new query inserted */;
    }

    @Test
    public void execute_same_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_emptyResult_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` WHERE `location` = 'Vienna'")
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* new query inserted */;
    }

    @Test
    public void execute_emptyResultTwice_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` WHERE `location` = 'Vienna'")
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_dataChangeSameQuery_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, QueryMalformedException, ColumnParseException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        MariaDbConfig.execute(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, "INSERT INTO weather_aus (id, `date`, location, mintemp, rainfall) VALUES (4, '2008-12-04', 'Albury', 12.9, 0.2)");

        /* test */
        storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void execute_semicolon_fails() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT + ";")
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        });
    }

    @Test
    public void insert_timestamp_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .timestamp(Instant.now().plus(1, ChronoUnit.SECONDS))
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_anonymous_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_5_USERNAME))
                .thenReturn(Optional.of(USER_5));
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, null);
    }

    @Test
    public void insert_notFound_succeeds() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.empty());
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            storeService.insert(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_1, USER_1_USERNAME);

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
    public void findOne_notFound_fails() throws SQLException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, 9999L, principal);
        });
    }

    @Test
    public void deleteStaleQueries_succeeds() throws QueryStoreException, ImageNotSupportedException, SQLException {

        /* mock */
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_1, USER_1_USERNAME);
        MariaDbConfig.insertQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME, QUERY_2, USER_1_USERNAME);
        when(databaseRepository.findAll())
                .thenReturn(List.of(DATABASE_1));

        /* test */
        storeService.deleteStaleQueries();
        final List<Map<String, Object>> response = MariaDbConfig.listQueryStore(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME);
        assertEquals(1, response.size());
    }

}
