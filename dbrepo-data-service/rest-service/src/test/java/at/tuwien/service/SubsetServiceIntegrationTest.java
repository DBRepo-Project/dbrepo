package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.config.S3Config;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.test.AbstractUnitTest;
import com.google.common.hash.Hashing;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
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

    @MockBean
    private DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @MockBean
    private StorageService storageService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Autowired
    private S3Config s3Config;

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
    }

    @Test
    public void execute_succeeds() throws QueryStoreInsertException, TableMalformedException, SQLException,
            QueryNotFoundException, UserNotFoundException, NotAllowedException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException, InterruptedException {

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
    public void execute_joinWithAlias_succeeds() throws QueryStoreInsertException, TableMalformedException,
            SQLException, QueryNotFoundException, UserNotFoundException, NotAllowedException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getUserById(QUERY_1_CREATED_BY))
                .thenReturn(QUERY_1_CREATOR);

        /* test */
        final QueryResultDto response = queryService.execute(DATABASE_1_PRIVILEGED_DTO, QUERY_7_STATEMENT, Instant.now(), USER_1_ID, 0L, 10L, null, null);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getHeaders());
        assertEquals(5, response.getHeaders().size());
        assertEquals(List.of(Map.of("id", 0), Map.of("date", 1), Map.of("location", 2), Map.of("lat", 3), Map.of("lng", 4)), response.getHeaders());
        assertNotNull(response.getResult());
        assertEquals(1, response.getResult().size());
        /* row 0 */
        assertEquals(BigInteger.valueOf(1L), response.getResult().get(0).get("id"));
        assertEquals(Instant.ofEpochSecond(1228089600), response.getResult().get(0).get("date"));
        assertEquals("Albury", response.getResult().get(0).get("location"));
        assertEquals(-36.0653583, response.getResult().get(0).get("lat"));
        assertEquals(146.9112214, response.getResult().get(0).get("lng"));
    }

    @Test
    public void execute_oneResult_succeeds() throws QueryStoreInsertException, TableMalformedException, SQLException,
            QueryNotFoundException, UserNotFoundException, NotAllowedException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException, InterruptedException {

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
            SQLException, QueryNotFoundException, UserNotFoundException, NotAllowedException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

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
    public void findAll_succeeds() throws SQLException, QueryNotFoundException, NotAllowedException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(null);
        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getId());
        assertEquals(2L, response.get(1).getId());
    }

    @Test
    public void findAll_onlyPersisted_succeeds() throws SQLException, QueryNotFoundException, NotAllowedException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(true);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
    }

    @Test
    public void findAll_onlyNonPersisted_succeeds() throws SQLException, QueryNotFoundException, NotAllowedException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(false);
        assertEquals(1, response.size());
        assertEquals(2L, response.get(0).getId());
    }

    @Test
    public void findById_succeeds() throws SQLException, QueryNotFoundException, UserNotFoundException,
            NotAllowedException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

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
    public void persist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, NotAllowedException, RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException, InterruptedException {

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
    public void persist_unPersist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, NotAllowedException, RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException, InterruptedException {

        /* mock */
        when(metadataServiceGateway.getUserById(QUERY_1_CREATED_BY))
                .thenReturn(QUERY_1_CREATOR);

        /* test */
        persist_generic(QUERY_1_ID, List.of(IDENTIFIER_2_DTO), false);
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID);
        assertEquals(1L, response.getId());
        assertFalse(response.getIsPersisted());
    }

    @Test
    public void createQueryStore_succeeds() throws SQLException, QueryStoreCreateException, InterruptedException {

        /* mock */
        MariaDbConfig.dropQueryStore(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        createQueryStore_generic(DATABASE_1_INTERNALNAME);
    }

    @Test
    public void createQueryStore_fails() {

        /* test */
        assertThrows(QueryStoreCreateException.class, () -> {
            createQueryStore_generic(DATABASE_1_INTERNALNAME);
        });
    }

    @Test
    public void export_succeeds() throws SQLException, StorageUnavailableException, QueryMalformedException,
            SidecarExportException, MetadataServiceException, RemoteUnavailableException, IOException,
            StorageNotFoundException, InterruptedException {

        /* mock */
        MariaDbConfig.dropQueryStore(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        export_generic();
    }

    protected void findById_generic(Long queryId) throws NotAllowedException, RemoteUnavailableException, SQLException,
            UserNotFoundException, QueryNotFoundException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

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

    protected List<QueryDto> findAll_generic(Boolean filterPersisted) throws SQLException, QueryNotFoundException,
            NotAllowedException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

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
            throws RemoteUnavailableException, SQLException, QueryStorePersistException, MetadataServiceException,
            DatabaseNotFoundException, InterruptedException {

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

    protected void createQueryStore_generic(String databaseName) throws SQLException, QueryStoreCreateException,
            InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* test */
        queryService.createQueryStore(CONTAINER_1_PRIVILEGED_DTO, databaseName);
        final List<Map<String, Object>> response = MariaDbConfig.listQueryStore(DATABASE_1_PRIVILEGED_DTO);
        assertEquals(0, response.size());
    }

    protected void export_generic() throws StorageUnavailableException, SQLException,
            QueryMalformedException, SidecarExportException, MetadataServiceException, RemoteUnavailableException,
            StorageNotFoundException, IOException, InterruptedException {
        final String filename = "68b329da9893e34099c7d8ad5cb9c940";

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        FileUtils.deleteQuietly(new File(s3Config.getS3FilePath() + "/" + filename));
        doNothing()
                .when(dataDatabaseSidecarGateway)
                .exportFile(anyString(), anyInt(), eq(filename));
        when(storageService.getResource(anyString()))
                .thenReturn(EXPORT_RESOURCE_DTO);

        /* test */
        final ExportResourceDto response = queryService.export(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, Instant.now(), filename);
        assertEquals(filename, response.getFilename());
        assertNotNull(response.getResource().getInputStream());
    }

}
