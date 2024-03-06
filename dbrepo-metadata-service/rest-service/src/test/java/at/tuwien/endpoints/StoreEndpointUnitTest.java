package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.UserService;
import at.tuwien.service.impl.StoreServiceImpl;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class StoreEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private StoreEndpoint storeEndpoint;

    @MockBean
    private StoreServiceImpl storeService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private UserService userService;

    @MockBean
    private AccessService accessService;

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
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException, NotAllowedException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_noRole_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException, NotAllowedException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-queries"})
    public void findAll_hasRole_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException, NotAllowedException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-queries"})
    public void findAll_privateNoAccess_fails() throws AccessDeniedException, DatabaseNotFoundException {

        /* mock */
        doThrow(AccessDeniedException.class)
                .when(accessService)
                .find(DATABASE_1_ID, USER_2_ID);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-queries"})
    public void findAll_publicNoAccess_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, NotAllowedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, AccessDeniedException {

        /* mock */
        doThrow(AccessDeniedException.class)
                .when(accessService)
                .find(DATABASE_3_ID, USER_2_ID);

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-queries"})
    public void findAll_hasAccess_succeeds() throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, AccessDeniedException {

        /* mock */
        when(accessService.find(DATABASE_2_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        findAll_generic(DATABASE_2_ID, DATABASE_2, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void find_publicAnonymous_succeeds() throws QueryStoreException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException, UserNotFoundException, NotAllowedException,
            DatabaseConnectionException, KeycloakRemoteException, AccessDeniedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryDto response = find_generic(DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, null);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    @WithAnonymousUser
    public void find_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-query")
    public void find_hasRole_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException,
            KeycloakRemoteException, AccessDeniedException {

        /* mock */
        when(userService.find(USER_1_ID))
                .thenReturn(USER_1);

        /* test */
        final QueryDto response = find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_PRINCIPAL);
        assertNotNull(response.getCreator());
        assertEquals(DATABASE_1_ID, response.getDatabaseId());
        assertEquals(QUERY_1_ID, response.getId());
        assertNotNull(response.getIdentifiers());
        assertTrue(response.getIsPersisted());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
        assertNotNull(response.getResultNumber());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_noRole_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException,
            KeycloakRemoteException, AccessDeniedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryDto response = find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_PRINCIPAL);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-query")
    public void find_notFound_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            find_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, null, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "find-query")
    public void find_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            find_generic(DATABASE_1_ID, null, QUERY_1_ID, QUERY_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "persist-query")
    public void persist_ownRead_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, AccessDeniedException, IdentifierAlreadyPublishedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryDto response = persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_READ_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "persist-query")
    public void persist_ownWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, AccessDeniedException, IdentifierAlreadyPublishedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryDto response = persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = "persist-query")
    public void persist_ownWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            AccessDeniedException, IdentifierAlreadyPublishedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryDto response = persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_ALL_ACCESS);
        assertEquals(QUERY_1_ID, response.getId());
        assertEquals(QUERY_1_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = "persist-query")
    public void persist_foreignWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, AccessDeniedException, IdentifierAlreadyPublishedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        persist_generic(DATABASE_1_ID, DATABASE_1, QUERY_1_ID, QUERY_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_1_USER_2_WRITE_ALL_ACCESS);

    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected QueryDto persist_generic(Long databaseId, Database database, Long queryId, Query query,
                                       UUID userId, Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, UserNotFoundException, QueryStoreException, QueryNotFoundException,
            ImageNotSupportedException, NotAllowedException, AccessDeniedException, IdentifierAlreadyPublishedException {
        final QueryPersistDto request = QueryPersistDto.builder()
                .persist(true)
                .build();

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(storeService.findOne(databaseId, queryId, principal))
                .thenReturn(query);
        doReturn(query)
                .when(storeService)
                .persist(databaseId, queryId, request);
        if (access != null) {
            log.trace("mock access for database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenThrow(NotAllowedException.class);
        }

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.persist(databaseId, queryId, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected void findAll_generic(Long databaseId, Database database, Principal principal)
            throws UserNotFoundException, QueryStoreException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, NotAllowedException,
            AccessDeniedException {

        /* mock */
        when(storeService.findAll(databaseId, true, principal))
                .thenReturn(List.of(QUERY_1));
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(userService.findAll())
                .thenReturn(List.of(USER_1));

        /* test */
        final ResponseEntity<List<QueryBriefDto>> response = storeEndpoint.findAll(databaseId, true, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        final QueryBriefDto query0 = response.getBody().get(0);
        assertNotNull(query0.getCreator());
        assertEquals(databaseId, query0.getDatabaseId());
        assertEquals(QUERY_1_ID, query0.getId());
        assertNotNull(query0.getIdentifiers());
        assertTrue(query0.getIsPersisted());
        assertEquals(QUERY_1_STATEMENT, query0.getQuery());
        assertNotNull(query0.getResultNumber());
    }

    protected QueryDto find_generic(Long databaseId, Database database, Long queryId, Query query, Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            UserNotFoundException, NotAllowedException, DatabaseConnectionException, KeycloakRemoteException,
            AccessDeniedException {

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

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.find(databaseId, queryId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final QueryDto body = response.getBody();
        assertNotNull(body);
        return body;
    }

}
