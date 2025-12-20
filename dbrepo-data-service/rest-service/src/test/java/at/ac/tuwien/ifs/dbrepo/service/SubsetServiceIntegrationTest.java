package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.api.SubsetMetadata;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class SubsetServiceIntegrationTest extends BaseTest {

    @Autowired
    private SubsetService subsetService;

    @MockitoBean
    private DataService dataService;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Container
    private static RedisContainerConfig.CustomRedisContainer redisContainer = RedisContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
    }

    @Test
    public void findAll_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException,
            UserNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(null);
        assertEquals(2, response.size());
        assertNotNull(response.get(0).getId());
        assertNotNull(response.get(1).getId());
    }

    @Test
    public void findAll_onlyPersisted_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException,
            UserNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(true);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getId());
    }

    @Test
    public void findAll_onlyNonPersisted_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException,
            UserNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(false);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getId());
    }

    @Test
    public void findById_succeeds() throws SQLException, QueryNotFoundException, UserNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* mock */
        final UUID queryId = MariaDbUtil.insertQueryStore(DATABASE_1_CACHE, QUERY_1_DTO, USER_1_USERNAME);

        /* test */
        findById_generic(queryId);
    }

    @Test
    public void findById_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            findById_generic(UUID.randomUUID());
        });
    }

    @Test
    public void persist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        final UUID id = MariaDbUtil.insertQueryStore(DATABASE_1_CACHE, QUERY_2_DTO, USER_1_USERNAME);
        when(metadataServiceGateway.getUserByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);

        /* test */
        persist_generic(id, List.of(IDENTIFIER_5_BRIEF_DTO), true);
        final Subset response = subsetService.findById(DATABASE_1_CACHE, id);
        assertEquals(id, response.getId());
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        final UUID id = MariaDbUtil.insertQueryStore(DATABASE_1_CACHE, QUERY_1_DTO, USER_1_USERNAME);
        when(metadataServiceGateway.getUserByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);

        /* test */
        persist_generic(id, List.of(IDENTIFIER_2_BRIEF_DTO), false);
        final Subset response = subsetService.findById(DATABASE_1_CACHE, id);
        assertEquals(id, response.getId());
        assertFalse(response.getIsPersisted());
    }

    @Test
    public void getMetadata_malformed_fails() {

        /* test */
        assertThrows(QueryExecutionException.class, () -> {
            subsetService.getMetadata(DATABASE_1_CACHE, "SELECT");
        });
    }

    @Test
    public void storeQuery_succeeds() throws SQLException, QueryStoreInsertException {

        /* test */
        final UUID response = subsetService.storeQuery(DATABASE_1_CACHE, QUERY_1_STATEMENT,
                QUERY_1_STATEMENT_NORMALIZED, QUERY_1_EXECUTION, USER_1_USERNAME);
        assertNotNull(response);
        final List<Map<String, Object>> subsets = MariaDbUtil.listQueryStore(DATABASE_1_CACHE);
        assertEquals(1, subsets.size());
        final Map<String, Object> subset0 = subsets.get(0);
        assertEquals(USER_1_USERNAME, subset0.get("created_by"));
        assertEquals(QUERY_1_STATEMENT, subset0.get("query"));
        assertEquals(QUERY_1_STATEMENT_NORMALIZED, subset0.get("query_normalized"));
    }

    @Test
    public void getMetadata_succeeds() throws SQLException, QueryMalformedException, QueryExecutionException {
        /* test */
        final SubsetMetadata response = subsetService.getMetadata(DATABASE_1_CACHE, MariaDbUtil.replaceExecutionTimestamp(
                QUERY_1_STATEMENT_NORMALIZED, QUERY_1_EXECUTION, Instant.now().plus(12, ChronoUnit.HOURS)));
        assertNotNull(response);
        assertEquals(QUERY_1_RESULT_HASH, response.getResultHash());
        assertEquals(3, response.getResultCount());
    }

    @Test
    public void create_succeeds() throws SQLException, QueryStoreInsertException, TableNotFoundException,
            QueryMalformedException, ImageNotFoundException, ViewNotFoundException, ColumnNotFoundException {

        /* test */
        final UUID response = subsetService.create(DATABASE_1_CACHE, QUERY_1_SUBSET_DTO, QUERY_1_EXECUTION, USER_1_USERNAME);
        assertNotNull(response);
    }

    @Test
    public void storeQuery_fails() {

        /* test */
        assertThrows(QueryStoreInsertException.class, () -> {
            subsetService.storeQuery(DATABASE_1_CACHE, "DROP DATABASE `weather`", "", QUERY_1_EXECUTION, USER_1_USERNAME);
        });
    }

    @Test
    public void storeQuery_simple_succeeds() throws QueryStoreInsertException, SQLException {

        /* test */
        subsetService.storeQuery(DATABASE_1_CACHE, "SELECT 1", "SELECT 1", QUERY_1_EXECUTION, USER_1_USERNAME);
    }

    @Test
    public void storeQuery_simpleRow_succeeds() throws QueryStoreInsertException, SQLException {

        /* test */
        subsetService.storeQuery(DATABASE_1_CACHE, "VALUES (1, 2, 3)", "VALUES (1, 2, 3)", QUERY_1_EXECUTION, USER_1_USERNAME);
    }

    protected void findById_generic(UUID queryId) throws RemoteUnavailableException, SQLException,
            UserNotFoundException, QueryNotFoundException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, queryId))
                .thenReturn(List.of(IDENTIFIER_2_BRIEF_DTO));
        when(metadataServiceGateway.getUserByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);

        /* test */
        final Subset response = subsetService.findById(DATABASE_1_CACHE, queryId);
        assertEquals(queryId, response.getId());
    }

    protected List<QueryDto> findAll_generic(Boolean filterPersisted) throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException, UserNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        MariaDbUtil.insertQueryStore(DATABASE_1_CACHE, QUERY_1_DTO, USER_1_USERNAME);
        MariaDbUtil.insertQueryStore(DATABASE_1_CACHE, QUERY_2_DTO, USER_1_USERNAME);
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, null))
                .thenReturn(List.of(IDENTIFIER_2_BRIEF_DTO, IDENTIFIER_5_BRIEF_DTO));

        /* test */
        return subsetService.findAll(DATABASE_1_CACHE, filterPersisted);
    }

    protected void persist_generic(UUID subsetId, List<IdentifierBriefDto> identifiers, Boolean persist)
            throws RemoteUnavailableException, SQLException, QueryStorePersistException, MetadataServiceException,
            DatabaseNotFoundException, InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, subsetId))
                .thenReturn(identifiers);

        /* test */
        subsetService.persist(DATABASE_1_CACHE, subsetId, persist);
    }

}
