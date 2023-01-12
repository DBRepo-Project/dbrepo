package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.UserService;
import at.tuwien.service.impl.StoreServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class StoreEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @Autowired
    private StoreEndpoint storeEndpoint;

    @MockBean
    private QueryService queryService;

    @MockBean
    private StoreServiceImpl storeService;

    @MockBean
    private UserService userService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(List.of(QUERY_1)).when(storeService)
                .findAll(CONTAINER_1_ID, DATABASE_1_ID, true, principal);
        doReturn(Collections.singletonList(USER_1)).when(userService)
                .findAll();
        doReturn(DATABASE_1, DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        final ResponseEntity<List<QueryBriefDto>> response = storeEndpoint.findAll(CONTAINER_1_ID, DATABASE_1_ID, true, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        final QueryBriefDto query = response.getBody().get(0);
        assertEquals(QUERY_1_ID, query.getId());
        assertEquals(QUERY_1_STATEMENT, query.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(QUERY_1).when(storeService)
                .findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        doReturn(USER_1).when(userService)
                .find(USER_1_ID);
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.find(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final QueryDto query = response.getBody();
        assertNotNull(query);
        assertEquals(QUERY_1_ID, query.getId());
        assertEquals(QUERY_1_STATEMENT, query.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_notFound_fails() throws QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException, UserNotFoundException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doThrow(QueryNotFoundException.class).when(storeService)
                .findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            storeEndpoint.find(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_dbNotFound_fails() throws QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException, UserNotFoundException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doThrow(DatabaseNotFoundException.class).when(storeService)
                .findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            storeEndpoint.find(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        });
    }

    @Test
    public void persist_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, null, null, null);
        });
    }

    @Test
    public void persist_publicRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_READ_ACCESS);
        });
    }

    @Test
    public void persist_publicWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        persist_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void persist_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        persist_generic(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void persist_generic(Long containerId, Long databaseId, Long queryId, String username,
                                   Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, QueryStoreException, QueryNotFoundException, ImageNotSupportedException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException {

        /* mock */
        when(databaseService.find(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(databaseService.find(CONTAINER_2_ID, DATABASE_2_ID))
                .thenReturn(DATABASE_2);
        when(storeService.findOne(containerId, databaseId, queryId, principal))
                .thenReturn(QUERY_1);
        when(storeService.persist(containerId, databaseId, queryId, principal))
                .thenReturn(QUERY_2);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.persist(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_2_ID, response.getBody().getId());
        assertEquals(QUERY_2_STATEMENT, response.getBody().getQuery());
    }

}
