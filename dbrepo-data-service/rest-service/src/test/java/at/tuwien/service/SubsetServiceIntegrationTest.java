package at.tuwien.service;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
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

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_PRIVILEGED_DTO);
    }

    @Test
    public void findAll_succeeds() throws SQLException, QueryNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(null);
        assertEquals(2, response.size());
        assertNotNull(response.get(0).getId());
        assertNotNull(response.get(1).getId());
    }

    @Test
    public void findAll_onlyPersisted_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(true);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getId());
    }

    @Test
    public void findAll_onlyNonPersisted_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(false);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getId());
    }

    @Test
    public void findById_succeeds() throws SQLException, QueryNotFoundException, UserNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* mock */
        final UUID queryId = MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);

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
        final UUID queryId2 = MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_2_DTO, USER_1_ID);
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        persist_generic(queryId2, List.of(IDENTIFIER_5_BRIEF_DTO), true);
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, queryId2);
        assertEquals(queryId2, response.getId());
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        final UUID queryId1 = MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        persist_generic(queryId1, List.of(IDENTIFIER_2_BRIEF_DTO), false);
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, queryId1);
        assertEquals(queryId1, response.getId());
        assertFalse(response.getIsPersisted());
    }

    protected void findById_generic(UUID queryId) throws RemoteUnavailableException, SQLException,
            UserNotFoundException, QueryNotFoundException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, queryId))
                .thenReturn(List.of(IDENTIFIER_2_BRIEF_DTO));
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, queryId);
        assertEquals(queryId, response.getId());
    }

    protected List<QueryDto> findAll_generic(Boolean filterPersisted) throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_2_DTO, USER_1_ID);
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, null))
                .thenReturn(List.of(IDENTIFIER_2_BRIEF_DTO, IDENTIFIER_5_BRIEF_DTO));

        /* test */
        return queryService.findAll(DATABASE_1_PRIVILEGED_DTO, filterPersisted);
    }

    protected void persist_generic(UUID queryId, List<IdentifierBriefDto> identifiers, Boolean persist)
            throws RemoteUnavailableException, SQLException, QueryStorePersistException, MetadataServiceException,
            DatabaseNotFoundException, InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, queryId))
                .thenReturn(identifiers);

        /* test */
        queryService.persist(DATABASE_1_PRIVILEGED_DTO, queryId, persist);
    }

}
