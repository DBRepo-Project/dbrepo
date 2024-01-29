package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.*;
import com.github.jsonldjava.utils.Obj;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class StoreServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private QueryService queryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreService storeService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4, USER_5));
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setAccesses(List.of());
        databaseRepository.save(DATABASE_1);
        /* data stuff */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_ID);
    }

    @Test
    public void findAll_filterPersisted_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, ContainerNotFoundException {

        /* test */
        final List<Query> queries = storeService.findAll(DATABASE_1_ID, true, USER_1_PRINCIPAL);
        assertEquals(1, queries.size());
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
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, principal);
        });
    }

    @Test
    public void findAll_succeeds() throws ContainerNotFoundException, UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException {

        /* test */
        final List<Query> response = storeService.findAll(DATABASE_1_ID, null, USER_1_PRINCIPAL);
        assertEquals(1, response.size());
    }

    @Test
    public void findAll_onlyPersisted_succeeds() throws ContainerNotFoundException, UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException {

        /* test */
        final List<Query> response = storeService.findAll(DATABASE_1_ID, true, USER_1_PRINCIPAL);
        assertEquals(1, response.size());
    }

    @Test
    public void findAll_onlyNotPersisted_succeeds() throws ContainerNotFoundException, UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException {

        /* test */
        final List<Query> response = storeService.findAll(DATABASE_1_ID, false, USER_1_PRINCIPAL);
        assertEquals(0, response.size());
    }

    @Test
    public void findOne_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeService.findOne(DATABASE_1_ID, 9999L, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void persist_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException,
            IdentifierAlreadyPublishedException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* precondition */
        final Query query1 = storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
        assertTrue(query1.getIsPersisted());

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, QUERY_1_ID, request);
        assertNotNull(response);
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersistUnchanged_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException,
            IdentifierAlreadyPublishedException, SQLException {
        final Query query = Query.builder()
                .id(2L)
                .query(QUERY_3_STATEMENT)
                .queryHash(QUERY_3_QUERY_HASH)
                .resultHash(QUERY_3_RESULT_HASH)
                .created(QUERY_3_CREATED)
                .executed(QUERY_3_EXECUTION)
                .createdBy(USER_1_ID)
                .resultNumber(QUERY_3_RESULT_NUMBER)
                .isPersisted(false) // <<<<<<<
                .build();
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(false) // <<<<<<<
                .build();

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, query, USER_1_ID);

        /* precondition */
        final Query query2 = storeService.findOne(DATABASE_1_ID, 2L, USER_1_PRINCIPAL);
        assertFalse(query2.getIsPersisted());

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, 2L, request);
        assertNotNull(response);
        assertFalse(response.getIsPersisted());
    }

    @Test
    public void persist_unPersistIdentifierAlreadyAttached_fails () {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(false)
                .build();

        /* test */
        assertThrows(IdentifierAlreadyPublishedException.class, () -> {
            storeService.persist(DATABASE_1_ID, QUERY_1_ID, request);
        });
    }

    @Test
    public void insert_same_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, SQLException, KeycloakRemoteException,
            AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_2_STATEMENT)
                .build();

        /* test */
        final Query response = storeService.insert(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        log.debug("found queries in query store: {}", MariaDbConfig.selectQuery(DATABASE_1,
                "SELECT `query_normalized`, `query_hash`, `result_hash` FROM `qs_queries`", "query_normalized", "query_hash", "result_hash"));
        assertEquals(QUERY_1_ID, response.getId()) /* no new query inserted */;
    }

    @Test
    public void execute_differentResult_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, ColumnParseException, KeycloakRemoteException, AccessDeniedException,
            QueryNotFoundException, SQLException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* mock */
        MariaDbConfig.execute(DATABASE_1, "INSERT INTO `weather_aus` (`id`, `date`) VALUES (4, '2024-01-12');");

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(2L, response.getId()) /* new query inserted */;
    }

    @Test
    @Disabled("not testable")
    public void execute_same_succeeds() throws UserNotFoundException, QueryStoreException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
            ColumnParseException, KeycloakRemoteException, AccessDeniedException, QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(1L, response.getId()) /* no new query inserted */;
    }

    @Test
    @Disabled("not testable")
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
                .statement("SELECT `id`, `date`, `location`, `mintemp`, `rainfall` FROM `weather_aus` WHERE `location` = 'Wien'")
                .build();

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(2L, response.getId()) /* new query inserted */;
    }

    @Test
    public void execute_emptyResultTwice_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, ColumnParseException, KeycloakRemoteException, AccessDeniedException,
            QueryNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT `location`, `mintemp` FROM `weather_aus` WHERE `rainfall` < 0")
                .build();

        /* mock */
        queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_ID, request, USER_1_PRINCIPAL, 0L, 10L, null, null);
        assertEquals(2L, response.getId()) /* no new query inserted */;
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
    public void persist_alreadyPersisted_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, SQLException,
            IdentifierAlreadyPublishedException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_2, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_3, USER_1_ID);

        /* test */
        final Query response = storeService.persist(DATABASE_1_ID, QUERY_3_ID, request);
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersist_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException, SQLException,
            IdentifierAlreadyPublishedException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(false)
                .build();

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_2, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_3, USER_1_ID);

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
        MariaDbConfig.insertQueryStore(DATABASE_1, QUERY_1, USER_1_ID);

        /* test */
        storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    public void deleteStaleQueries_succeeds() throws QueryStoreException, ImageNotSupportedException, SQLException {

        /* test */
        storeService.deleteStaleQueries();
        final List<Map<String, Object>> response = MariaDbConfig.listQueryStore(DATABASE_1);
        assertEquals(1, response.size());
    }
}
