package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.AccessService;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
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
    private AccessService accessService;

    @Test
    public void findAll_anonymous_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, null);
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymous2_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_researcher_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_researcherPrivateNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_dataSteward_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_2, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_developer_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_3, USER_3_PRINCIPAL);
    }

    @Test
    public void find_anonymous_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_ID, null, null);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous2_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_ID, null, null);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_researcher_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = "DATA_STEWARD")
    public void find_dataSteward_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_2_ID, USER_2, USER_2_PRINCIPAL);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = "DEVELOPER")
    public void find_developer_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_3_ID, USER_3, USER_3_PRINCIPAL);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_notFound_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            find_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, null, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_databaseNotFound_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(CONTAINER_1_ID, DATABASE_1_ID, null, QUERY_1_ID, QUERY_1, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void persist_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, null, null, null);
        });
    }

    @Test
    public void persist_publicRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_READ_ACCESS);
        });
    }

    @Test
    public void persist_publicWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    public void persist_publicWriteAllAlreadyPersisted_succeeds() {

        /* test */
        assertThrows(QueryAlreadyPersistedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
        });
    }

    @Test
    public void persist_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected QueryDto persist_generic(Long containerId, Long databaseId, Database database, Long queryId, Query query,
                                       String username, Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, UserNotFoundException, QueryStoreException, QueryNotFoundException,
            ImageNotSupportedException, NotAllowedException, DatabaseConnectionException,
            QueryAlreadyPersistedException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(storeService.findOne(containerId, databaseId, queryId, principal))
                .thenReturn(query);
        when(storeService.persist(containerId, databaseId, queryId, principal))
                .thenReturn(query);
        if (access != null) {
            log.trace("mock access for database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenThrow(NotAllowedException.class);
        }

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.persist(containerId, databaseId, queryId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected void findAll_generic(Long containerId, Long databaseId, Database database, User user, Principal principal)
            throws UserNotFoundException, QueryStoreException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, NotAllowedException {

        /* mock */
        doReturn(List.of(QUERY_1)).when(storeService)
                .findAll(containerId, databaseId, true, principal);
        if (user != null) {
            when(userService.findAll())
                    .thenReturn(List.of(user));
        } else {
            when(userService.findAll())
                    .thenReturn(List.of());
        }
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);

        /* test */
        final ResponseEntity<List<QueryBriefDto>> response = storeEndpoint.findAll(containerId, databaseId, true, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        final QueryBriefDto query = response.getBody().get(0);
        assertEquals(QUERY_1_ID, query.getId());
        assertEquals(QUERY_1_STATEMENT, query.getQuery());
    }

    protected QueryDto find_generic(Long containerId, Long databaseId, Database database, Long queryId, Query query,
                                    Long userId, User user, Principal principal) throws QueryStoreException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, UserNotFoundException,
            NotAllowedException, DatabaseConnectionException {

        /* mock */
        if (query != null) {
            when(storeService.findOne(containerId, databaseId, queryId, principal))
                    .thenReturn(query);
        } else {
            when(storeService.findOne(containerId, databaseId, queryId, principal))
                    .thenThrow(QueryNotFoundException.class);
        }
        if (user != null) {
            when(userService.find(userId))
                    .thenReturn(user);
        } else {
            when(userService.find(userId))
                    .thenThrow(UserNotFoundException.class);
        }
        if (database != null) {
            when(databaseService.find(containerId, databaseId))
                    .thenReturn(database);
        } else {
            when(databaseService.find(containerId, databaseId))
                    .thenThrow(DatabaseNotFoundException.class);
        }

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.find(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final QueryDto body = response.getBody();
        assertNotNull(body);
        return body;
    }

}
