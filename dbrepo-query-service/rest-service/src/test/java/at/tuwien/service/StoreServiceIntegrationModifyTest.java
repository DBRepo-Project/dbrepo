package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.ViewIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class StoreServiceIntegrationModifyTest extends BaseUnitTest {

    @MockBean
    private ViewIdxRepository viewIdxRepository;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

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

    @Autowired
    private QueryService queryService;

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws InterruptedException, SQLException {
        /* metadata database */
        userRepository.save(USER_5);
        DATABASE_1.setTables(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_7));
        DATABASE_1.setViews(List.of(VIEW_3));
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    public void insert_same_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);

        /* test */
        final Query response = storeService.insert(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        log.debug("found queries in query store: {}", MariaDbConfig.selectQuery(DATABASE_1,
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
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(DATABASE_1_ID, mock, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
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
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
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
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
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
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
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
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        MariaDbConfig.execute(DATABASE_1, "INSERT INTO weather_aus (id, `date`, location, mintemp, rainfall) VALUES (4, '2008-12-04', 'Albury', 12.9, 0.2)");

        /* test */
        storeService.insert(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void execute_semicolon_fails() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT + ";")
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
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
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        storeService.insert(DATABASE_1_ID, request, USER_1_PRINCIPAL);
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
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        storeService.insert(DATABASE_1_ID, request, null);
    }

    @Test
    public void insert_notFound_succeeds() {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.empty());
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            storeService.insert(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {

        /* mock */
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);

        /* test */
        storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void findOne_notFound_succeeds() {

        /* mock */
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_notFound_fails() {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, principal);
        });
    }

    @Test
    public void deleteStaleQueries_succeeds() throws QueryStoreException, ImageNotSupportedException, SQLException {
        final Query queryOk = Query.builder()
                .id(QUERY_1_ID)
                .query(QUERY_1_STATEMENT)
                .queryHash(QUERY_1_QUERY_HASH)
                .resultHash(QUERY_1_RESULT_HASH)
                .resultNumber(QUERY_1_RESULT_NUMBER)
                .created(Instant.now().minus(1, ChronoUnit.HOURS))
                .createdBy(USER_1_USERNAME)
                .isPersisted(QUERY_1_PERSISTED)
                .executed(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        final Query queryDelete = Query.builder()
                .id(QUERY_2_ID)
                .query(QUERY_2_STATEMENT)
                .queryHash(QUERY_2_QUERY_HASH)
                .resultHash(QUERY_2_RESULT_HASH)
                .resultNumber(QUERY_2_RESULT_NUMBER)
                .created(Instant.now().minus(25, ChronoUnit.HOURS))
                .createdBy(USER_2_USERNAME)
                .isPersisted(QUERY_2_PERSISTED)
                .executed(Instant.now().minus(25, ChronoUnit.HOURS))
                .build();

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, queryOk, USER_1_USERNAME);
        MariaDbConfig.insertQueryStore(DATABASE_1, queryDelete, USER_1_USERNAME);
        when(databaseRepository.findAll())
                .thenReturn(List.of(DATABASE_1));

        /* test */
        storeService.deleteStaleQueries();
        final List<Map<String, Object>> response = MariaDbConfig.listQueryStore(DATABASE_1);
        assertEquals(1, response.size());
    }

}
