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
        assertEquals(1L, response.get(0).getId());
        assertEquals(2L, response.get(1).getId());
    }

    @Test
    public void findAll_onlyPersisted_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(true);
        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
    }

    @Test
    public void findAll_onlyNonPersisted_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* test */
        final List<QueryDto> response = findAll_generic(false);
        assertEquals(1, response.size());
        assertEquals(2L, response.get(0).getId());
    }

    @Test
    public void findById_succeeds() throws SQLException, QueryNotFoundException, UserNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

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
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        persist_generic(QUERY_2_ID, List.of(IDENTIFIER_5_BRIEF_DTO), true);
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_2_ID);
        assertEquals(2L, response.getId());
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        persist_generic(QUERY_1_ID, List.of(IDENTIFIER_2_BRIEF_DTO), false);
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, QUERY_1_ID);
        assertEquals(1L, response.getId());
        assertFalse(response.getIsPersisted());
    }

    protected void findById_generic(Long queryId) throws RemoteUnavailableException, SQLException,
            UserNotFoundException, QueryNotFoundException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_2_BRIEF_DTO));
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);

        /* test */
        final QueryDto response = queryService.findById(DATABASE_1_PRIVILEGED_DTO, queryId);
        assertEquals(QUERY_1_ID, response.getId());
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

    protected void persist_generic(Long queryId, List<IdentifierBriefDto> identifiers, Boolean persist)
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

}
