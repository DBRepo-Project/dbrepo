package at.tuwien.service;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class SubsetServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private SubsetService queryService;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

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
    public void execute_succeeds() throws QueryStoreInsertException, TableMalformedException, SQLException,
            QueryNotFoundException, InterruptedException, UserNotFoundException, NotAllowedException,
            RemoteUnavailableException, ServiceException, DatabaseNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getUserById(QUERY_1_CREATED_BY))
                .thenReturn(QUERY_1_CREATOR);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_PRIVILEGED_DTO, QUERY_1_STATEMENT, Instant.now(), USER_1_ID, 0L, 10L, null, null);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getHeaders());
        assertEquals(5, response.getHeaders().size());
        assertEquals(List.of(Map.of("id", 0), Map.of("date", 1), Map.of("location", 2), Map.of("mintemp", 3), Map.of("rainfall", 4)), response.getHeaders());
        assertNotNull(response.getResult());
        assertEquals(3, response.getResult().size());
        /* row 0 */
        assertEquals(BigInteger.valueOf(1L), response.getResult().get(0).get("id"));
        assertEquals(Instant.ofEpochSecond(1228089600), response.getResult().get(0).get("date"));
        assertEquals("Albury", response.getResult().get(0).get("location"));
        assertEquals(13.4, response.getResult().get(0).get("mintemp"));
        assertEquals(0.6, response.getResult().get(0).get("rainfall"));
        /* row 1 */
        assertEquals(BigInteger.valueOf(2L), response.getResult().get(1).get("id"));
        assertEquals(Instant.ofEpochSecond(1228176000), response.getResult().get(1).get("date"));
        assertEquals("Albury", response.getResult().get(1).get("location"));
        assertEquals(7.4, response.getResult().get(1).get("mintemp"));
        assertEquals(0.0, response.getResult().get(1).get("rainfall"));
        /* row 2 */
        assertEquals(BigInteger.valueOf(3L), response.getResult().get(2).get("id"));
        assertEquals(Instant.ofEpochSecond(1228262400), response.getResult().get(2).get("date"));
        assertEquals("Albury", response.getResult().get(2).get("location"));
        assertEquals(12.9, response.getResult().get(2).get("mintemp"));
        assertEquals(0.0, response.getResult().get(2).get("rainfall"));
    }

    @Test
    public void execute_oneResult_succeeds() throws QueryStoreInsertException, TableMalformedException, SQLException,
            QueryNotFoundException, InterruptedException, UserNotFoundException, NotAllowedException,
            RemoteUnavailableException, ServiceException, DatabaseNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_2_DTO));
        when(metadataServiceGateway.getUserById(QUERY_1_CREATED_BY))
                .thenReturn(QUERY_1_CREATOR);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_PRIVILEGED_DTO, QUERY_1_STATEMENT, Instant.now(), USER_1_ID, 0L, 1L, null, null);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getHeaders());
        assertEquals(5, response.getHeaders().size());
        assertEquals(List.of(Map.of("id", 0), Map.of("date", 1), Map.of("location", 2), Map.of("mintemp", 3), Map.of("rainfall", 4)), response.getHeaders());
        assertNotNull(response.getResult());
        assertEquals(1, response.getResult().size());
        /* row 0 */
        assertEquals(BigInteger.valueOf(1L), response.getResult().get(0).get("id"));
        assertEquals(Instant.ofEpochSecond(1228089600), response.getResult().get(0).get("date"));
        assertEquals("Albury", response.getResult().get(0).get("location"));
        assertEquals(13.4, response.getResult().get(0).get("mintemp"));
        assertEquals(0.6, response.getResult().get(0).get("rainfall"));
    }

    @Test
    public void execute_oneResultPagination_succeeds() throws QueryStoreInsertException, TableMalformedException,
            SQLException, QueryNotFoundException, InterruptedException, UserNotFoundException, NotAllowedException,
            RemoteUnavailableException, ServiceException, DatabaseNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);
        when(metadataServiceGateway.getIdentifiers(eq(DATABASE_1_ID), anyLong()))
                .thenReturn(List.of());

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_PRIVILEGED_DTO, QUERY_1_STATEMENT, Instant.now(), USER_1_ID, 1L, 1L, null, null);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getHeaders());
        assertEquals(5, response.getHeaders().size());
        assertEquals(List.of(Map.of("id", 0), Map.of("date", 1), Map.of("location", 2), Map.of("mintemp", 3), Map.of("rainfall", 4)), response.getHeaders());
        assertNotNull(response.getResult());
        assertEquals(1, response.getResult().size());
        /* row 1 */
        assertEquals(BigInteger.valueOf(2L), response.getResult().get(0).get("id"));
        assertEquals(Instant.ofEpochSecond(1228176000), response.getResult().get(0).get("date"));
        assertEquals("Albury", response.getResult().get(0).get("location"));
        assertEquals(7.4, response.getResult().get(0).get("mintemp"));
        assertEquals(0.0, response.getResult().get(0).get("rainfall"));
    }

    @Test
    public void findAll_succeeds() throws SQLException, QueryNotFoundException, InterruptedException,
            NotAllowedException, RemoteUnavailableException, ServiceException, DatabaseNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(null);
        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals(2L, response.get(1).getId());
    }

    @Test
    public void findAll_onlyPersisted_succeeds() throws SQLException, QueryNotFoundException, InterruptedException,
            NotAllowedException, RemoteUnavailableException, ServiceException, DatabaseNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(true);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
    }

    @Test
    public void findAll_onlyNonPersisted_succeeds() throws SQLException, QueryNotFoundException, InterruptedException,
            NotAllowedException, RemoteUnavailableException, ServiceException, DatabaseNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(false);
        assertEquals(1, response.size());
        assertEquals(2L, response.get(0).getId());
    }

    @Test
    public void findById_succeeds() throws SQLException, QueryNotFoundException, InterruptedException,
            UserNotFoundException, NotAllowedException, RemoteUnavailableException, ServiceException,
            DatabaseNotFoundException {

        /* test */
        findById_generic(QUERY_1_ID);
    }

    @Test
    public void findById_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            findById_generic(9999L);
        });
    }

    @Test
    public void persist_succeeds() throws SQLException, InterruptedException, QueryStorePersistException,
            QueryNotFoundException, UserNotFoundException, NotAllowedException, RemoteUnavailableException,
            ServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataServiceGateway.getUserById(QUERY_2_CREATED_BY))
                .thenReturn(QUERY_2_CREATOR);

        /* test */
        persist_generic(QUERY_2_ID, List.of(IDENTIFIER_5_DTO), true);
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_2_ID);
        assertEquals(2L, response.getId());
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersist_succeeds() throws SQLException, InterruptedException, QueryStorePersistException,
            QueryNotFoundException, UserNotFoundException, NotAllowedException, RemoteUnavailableException,
            ServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataServiceGateway.getUserById(QUERY_1_CREATED_BY))
                .thenReturn(QUERY_1_CREATOR);

        /* test */
        persist_generic(QUERY_1_ID, List.of(IDENTIFIER_2_DTO), false);
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID);
        assertEquals(1L, response.getId());
        assertFalse(response.getIsPersisted());
    }

    protected void findById_generic(Long queryId) throws InterruptedException, NotAllowedException,
            RemoteUnavailableException, SQLException, UserNotFoundException, QueryNotFoundException, ServiceException,
            DatabaseNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_2_DTO));
        when(metadataServiceGateway.getUserById(QUERY_1_CREATED_BY))
                .thenReturn(QUERY_1_CREATOR);
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);

        /* test */
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, queryId);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(DATABASE_1_ID, response.getDatabaseId());
    }

    protected List<QueryDto> findAll_generic(Boolean filterPersisted) throws InterruptedException, SQLException,
            QueryNotFoundException, NotAllowedException, RemoteUnavailableException, ServiceException,
            DatabaseNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_2_DTO, USER_1_ID);
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, null))
                .thenReturn(List.of(IDENTIFIER_2_DTO, IDENTIFIER_5_DTO));

        /* test */
        return queryService.findAll(DATABASE_1_PRIVILEGED_DTO, filterPersisted);
    }

    protected void persist_generic(Long queryId, List<IdentifierDto> identifiers, Boolean persist)
            throws InterruptedException, RemoteUnavailableException, SQLException, QueryStorePersistException,
            ServiceException, DatabaseNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, queryId))
                .thenReturn(identifiers);
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_2_DTO, USER_1_ID);

        /* test */
        queryService.persist(DATABASE_1_PRIVILEGED_DTO, queryId, persist);
    }

}
