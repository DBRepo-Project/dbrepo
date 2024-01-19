package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.ViewService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class ViewEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private QueryService queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private ViewService viewService;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @Test
    @WithAnonymousUser
    public void findAll_publicAnonymous_succeeds() throws UserNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_publicHasRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_publicHasRoleHasAccess_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_3_USER_2_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_publicNoRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, null);
    }

    @Test
    @WithAnonymousUser
    public void create_publicAnonymous_succeeds() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_publicHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_publicHasRoleHasAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void create_publicNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"find-database-view"})
    public void find_publicHasRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_publicNoRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_publicHasRoleHasAccess_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void delete_publicAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-database-view"})
    public void delete_publicHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void delete_publicNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-database-view"})
    public void delete_publicOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException, AccessDeniedException {

        /* test */
        delete_generic(DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_ID, USER_3_PRINCIPAL, DATABASE_3_USER_1_WRITE_ALL_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void data_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException, ViewMalformedException, AccessDeniedException {

        /* test */
        data_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void data_publicNoRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException, ViewMalformedException, AccessDeniedException {

        /* test */
        data_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"view-database-view-data"})
    public void data_publicHasRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException, ViewMalformedException, AccessDeniedException {

        /* test */
        data_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"view-database-view-data"})
    public void data_publicHasRoleHasAccess_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException, ViewMalformedException, AccessDeniedException {

        /* test */
        data_generic(DATABASE_3_ID, VIEW_1_ID, DATABASE_3, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void findAll_privateAnonymous_succeeds() throws UserNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_privateHasRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_privateHasRoleHasAccess_succeeds() throws UserNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_1_USER_2_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_privateNoRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException, AccessDeniedException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, null);
    }

    @Test
    @WithAnonymousUser
    public void create_privateAnonymous_succeeds() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_privateHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_privateHasRoleHasAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void create_privateNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_privateAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"find-database-view"})
    public void find_privateHasRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_privateNoRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_privateHasRoleHasAccess_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, AccessDeniedException {

        /* test */
        find_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void delete_privateAnonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-database-view"})
    public void delete_privateHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void delete_privateNoRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_1_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-database-view"})
    public void delete_privateOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException, AccessDeniedException {

        /* test */
        delete_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_1_ID, USER_1_PRINCIPAL, DATABASE_1_USER_1_WRITE_ALL_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void data_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            data_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void data_privateNoRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException, ViewMalformedException, AccessDeniedException {

        /* test */
        data_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"view-database-view-data"})
    public void data_privateHasRole_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException, ViewMalformedException, AccessDeniedException {

        /* test */
        data_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"view-database-view-data"})
    public void data_privateHasRoleHasAccess_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException, ViewMalformedException, AccessDeniedException {

        /* test */
        data_generic(DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_ID, USER_2_PRINCIPAL, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void count_privateAnonymous_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            ViewNotFoundException, DatabaseConnectionException, QueryMalformedException, QueryStoreException,
            TableMalformedException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        count_generic(DATABASE_1_ID, VIEW_2_ID, DATABASE_1, VIEW_2, USER_2_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void count_privateAnonymousDatabaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            count_generic(DATABASE_1_ID, VIEW_2_ID, null, VIEW_2, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void count_privateAnonymousViewNotFound_fails() {

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            count_generic(DATABASE_1_ID, VIEW_2_ID, DATABASE_1, null, USER_2_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void findAll_generic(Long databaseId, Database database, UUID userId, Principal principal,
                                   DatabaseAccess access) throws UserNotFoundException, DatabaseNotFoundException,
            AccessDeniedException {

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
            when(viewService.findAll(databaseId, principal))
                    .thenReturn(List.of(VIEW_1, VIEW_2));
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenThrow(AccessDeniedException.class);
            when(viewService.findAll(databaseId, principal))
                    .thenReturn(List.of(VIEW_1));
        }

        /* test */
        final ResponseEntity<List<ViewBriefDto>> response = viewEndpoint.findAll(databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        if (access == null) {
            assertEquals(1, response.getBody().size());
        } else {
            assertEquals(2, response.getBody().size());
        }
    }

    protected void create_generic(Long databaseId, Database database, UUID userId, Principal principal,
                                  DatabaseAccess access) throws DatabaseNotFoundException, UserNotFoundException,
            DatabaseConnectionException, ViewMalformedException, QueryMalformedException, NotAllowedException,
            AccessDeniedException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenThrow(AccessDeniedException.class);
        }
        when(viewService.create(databaseId, request, principal))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewBriefDto> response = viewEndpoint.create(databaseId, request, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        assertEquals(VIEW_1_NAME, response.getBody().getName());
    }

    protected void find_generic(Long databaseId, Long viewId, Database database, UUID userId,
                                Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException, AccessDeniedException {

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenThrow(AccessDeniedException.class);
        }
        when(viewService.findById(databaseId, viewId, principal))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewDto> response = viewEndpoint.find(databaseId, viewId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        assertEquals(VIEW_1_NAME, response.getBody().getName());
    }

    protected void delete_generic(Long databaseId, Long viewId, Database database, UUID userId,
                                  Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException, AccessDeniedException {

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenThrow(AccessDeniedException.class);
        }
        doNothing()
                .when(viewService)
                .delete(databaseId, viewId, principal);

        /* test */
        final ResponseEntity<?> response = viewEndpoint.delete(databaseId, viewId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    protected void data_generic(Long databaseId, Long viewId, Database database, UUID userId,
                                Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException, DatabaseConnectionException,
            QueryMalformedException, QueryStoreException, TableMalformedException, ColumnParseException,
            ImageNotSupportedException, ContainerNotFoundException, PaginationException, ViewMalformedException,
            AccessDeniedException {
        final Long page = 0L;
        final Long size = 2L;

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(databaseId, userId))
                    .thenThrow(AccessDeniedException.class);
        }
        when(viewService.findById(databaseId, viewId, principal))
                .thenReturn(VIEW_1);
        when(queryService.viewFindAll(databaseId, VIEW_1, page, size, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = viewEndpoint.data(databaseId, viewId, principal, page, size);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_DTO, response.getBody());
    }

    protected void count_generic(Long databaseId, Long viewId, Database database, View view,
                                 Principal principal) throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, ViewNotFoundException {

        /* mock */
        if (database != null) {
            when(databaseService.find(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .find(databaseId);
        }
        if (view != null) {
            when(viewService.findById(databaseId, viewId, principal))
                    .thenReturn(VIEW_1);
        } else {
            doThrow(ViewNotFoundException.class)
                    .when(viewService)
                    .findById(databaseId, viewId, principal);
        }
        when(queryService.viewCount(databaseId, VIEW_1, principal))
                .thenReturn(5L);

        /* test */
        final ResponseEntity<Long> response = viewEndpoint.count(databaseId, viewId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody());
    }

}
