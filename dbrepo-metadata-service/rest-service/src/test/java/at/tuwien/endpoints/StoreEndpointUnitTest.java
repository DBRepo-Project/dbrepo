package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.ViewIdxRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
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
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class StoreEndpointUnitTest extends BaseUnitTest {

    @Autowired
    private StoreEndpoint storeEndpoint;

    @MockBean
    private StoreServiceImpl storeService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithAnonymousUser
    public void findAll_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            findAll_generic(DATABASE_1_ID, DATABASE_1, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void findAll_publicAnonymous_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException, NotAllowedException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_noRole_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException, NotAllowedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-queries"})
    public void findAll_hasRole_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException, NotAllowedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-queries"})
    public void findAll_privateNoAccess_fails() throws NotAllowedException {

        /* mock */
        doThrow(NotAllowedException.class)
                .when(accessService)
                .find(DATABASE_1_ID, USER_2_USERNAME);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-queries"})
    public void findAll_publicNoAccess_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, NotAllowedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {

        /* mock */
        doThrow(NotAllowedException.class)
                .when(accessService)
                .find(DATABASE_3_ID, USER_2_USERNAME);

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-queries"})
    public void findAll_hasAccess_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException {

        /* mock */
        when(accessService.find(DATABASE_2_ID, USER_1_USERNAME))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        findAll_generic(DATABASE_2_ID, DATABASE_2, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void find_publicAnonymous_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryDto response = find_generic(DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, null, null, null);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    @WithAnonymousUser
    public void find_privateAnonymous_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-query")
    public void find_hasRole_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_noRole_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-query")
    public void find_notFound_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, null, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-query")
    public void find_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            find_generic(DATABASE_1_ID, null, QUERY_1_ID, QUERY_1, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "persist-query")
    public void persist_ownRead_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "persist-query")
    public void persist_ownWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "persist-query")
    public void persist_ownWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_ALL_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = "persist-query")
    public void persist_foreignWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_2_USERNAME, USER_2, USER_2_PRINCIPAL, DATABASE_1_USER_2_WRITE_ALL_ACCESS);

    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected QueryDto persist_generic(Long databaseId, Database database, Long queryId, Query query,
                                       String username, User user, Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, UserNotFoundException, QueryStoreException, QueryNotFoundException,
            ImageNotSupportedException, NotAllowedException, DatabaseConnectionException,
            QueryAlreadyPersistedException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(storeService.findOne(databaseId, queryId, principal))
                .thenReturn(query);
        when(storeService.persist(databaseId, queryId, request))
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
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.persist(databaseId, queryId, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected void findAll_generic(Long databaseId, Database database, Principal principal)
            throws UserNotFoundException, QueryStoreException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, NotAllowedException {

        /* mock */
        doReturn(List.of(QUERY_1)).when(storeService)
                .findAll(databaseId, true, principal);
        when(databaseService.find(databaseId))
                .thenReturn(database);

        /* test */
        final ResponseEntity<List<QueryBriefDto>> response = storeEndpoint.findAll(databaseId, true, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        final QueryBriefDto query = response.getBody().get(0);
        assertEquals(QUERY_1_ID, query.getId());
        assertEquals(QUERY_1_STATEMENT, query.getQuery());
    }

    protected QueryDto find_generic(Long databaseId, Database database, Long queryId, Query query,
                                    String username, User user, Principal principal) throws QueryStoreException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, UserNotFoundException,
            NotAllowedException, DatabaseConnectionException {

        /* mock */
        if (query != null) {
            when(storeService.findOne(databaseId, queryId, principal))
                    .thenReturn(query);
        } else {
            when(storeService.findOne(databaseId, queryId, principal))
                    .thenThrow(QueryNotFoundException.class);
        }
        if (database != null) {
            when(databaseService.find(databaseId))
                    .thenReturn(database);
        } else {
            when(databaseService.find(databaseId))
                    .thenThrow(DatabaseNotFoundException.class);
        }
        if (user != null) {
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(user));
        } else {
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.empty());
        }

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.find(databaseId, queryId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final QueryDto body = response.getBody();
        assertNotNull(body);
        return body;
    }

}
