package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class StoreServiceIntegrationModifyTest extends BaseUnitTest {

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private UserRepository userRepository;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws InterruptedException, SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4, USER_5));
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void insert_same_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, SQLException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* mock */
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
            QueryMalformedException, ColumnParseException, KeycloakRemoteException, AccessDeniedException,
            QueryNotFoundException {
        final ExecuteStatementDto mock = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* mock */
        queryService.execute(DATABASE_1_ID, mock, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(2L, response.getId()) /* new query inserted */;
    }

    @Test
    public void execute_same_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
            ColumnParseException, KeycloakRemoteException, AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);


        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_notPersisted_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, ColumnParseException, SQLException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();


        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* no new query inserted */;
        assertFalse(Boolean.parseBoolean(MariaDbConfig.listQueryStore(DATABASE_1).get(0).get("is_persisted").toString()));
    }

    @Test
    public void execute_emptyResult_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, ColumnParseException, KeycloakRemoteException, AccessDeniedException,
            QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` WHERE `location` = 'Vienna'")
                .build();

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* new query inserted */;
    }

    @Test
    public void execute_emptyResultTwice_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, ColumnParseException, KeycloakRemoteException, AccessDeniedException,
            QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` WHERE `location` = 'Vienna'")
                .build();

        /* mock */
        queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_dataChangeSameQuery_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, ColumnParseException, SQLException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
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

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        });
    }

    @Test
    public void persist_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, QUERY_1_ID, request);
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_alreadyPersisted_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_2, USER_1_USERNAME);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_3, USER_1_USERNAME);

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, QUERY_3_ID, request);
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersist_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(false)
                .build();

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_2, USER_1_USERNAME);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_3, USER_1_USERNAME);

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, QUERY_3_ID, request);
        assertFalse(response.getIsPersisted());
    }

    @Test
    public void insert_timestamp_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .timestamp(Instant.now().plus(1, ChronoUnit.SECONDS))
                .build();

        /* test */
        storeService.insert(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_anonymous_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* test */
        storeService.insert(DATABASE_1_ID, request, null);
    }

    @Test
    public void findOne_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, SQLException {

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_USERNAME);

        /* test */
        storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void findOne_notFound_succeeds() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findOne_notFound_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
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

        /* test */
        storeService.deleteStaleQueries();
        final List<Map<String, Object>> response = MariaDbConfig.listQueryStore(DATABASE_1);
        assertEquals(1, response.size());
    }

}
