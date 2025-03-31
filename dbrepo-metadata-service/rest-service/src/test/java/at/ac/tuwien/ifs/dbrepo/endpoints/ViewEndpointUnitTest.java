package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
public class ViewEndpointUnitTest extends BaseTest {

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private ViewService viewService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private UserService userService;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @Test
    @WithAnonymousUser
    public void findAll_publicAnonymous_succeeds() throws UserNotFoundException, AccessNotFoundException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, null, null, null, null);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_publicHasRole_succeeds() throws UserNotFoundException, AccessNotFoundException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_publicHasRoleHasAccess_succeeds() throws UserNotFoundException, AccessNotFoundException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_3_USER_2_READ_ACCESS);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void findAll_publicNoRole_succeeds() throws UserNotFoundException, AccessNotFoundException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
    }

    @Test
    @WithAnonymousUser
    public void create_publicAnonymous_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, "View", null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_publicHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, "View", USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_publicHasRoleHasAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, "View", USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void create_publicNoRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(DATABASE_3_ID, DATABASE_3, "View", USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-view"})
    public void create_succeeds() throws UserNotFoundException, SearchServiceException, MalformedException,
            NotAllowedException, DataServiceException, DatabaseNotFoundException, AccessNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException, TableNotFoundException, ViewExistsException, ImageNotFoundException {

        /* test */
        create_generic(DATABASE_1_ID, DATABASE_1, "View", USER_1_PRINCIPAL, USER_1_ID, USER_1, DATABASE_1_USER_1_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-view"})
    public void create_exists_fails() {

        /* test */
        assertThrows(ViewExistsException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, VIEW_1_NAME, USER_1_PRINCIPAL, USER_1_ID, USER_1, DATABASE_1_USER_1_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_publicAnonymous_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_3_ID, DATABASE_3, null, null, null, null);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"find-database-view"})
    public void find_publicHasRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void find_publicNoRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void find_publicHasRoleHasAccess_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
    }

    @Test
    @WithAnonymousUser
    public void delete_publicAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(DATABASE_3_ID, DATABASE_3, VIEW_1_ID, VIEW_1, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"delete-database-view"})
    public void delete_publicHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(DATABASE_3_ID, DATABASE_3, VIEW_1_ID, VIEW_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void delete_publicNoRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(DATABASE_3_ID, DATABASE_3, VIEW_1_ID, VIEW_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username =USER_3_USERNAME, authorities = {"delete-database-view"})
    public void delete_publicOwner_succeeds() throws NotAllowedException, DataServiceException, UserNotFoundException,
            DataServiceConnectionException, DatabaseNotFoundException, AccessNotFoundException, ViewNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException {

        /* test */
        delete_generic(DATABASE_3_ID, DATABASE_3, VIEW_5_ID, VIEW_5, USER_3_PRINCIPAL, USER_3_ID, USER_3, DATABASE_3_USER_1_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void findAll_privateAnonymous_succeeds() throws UserNotFoundException,
            AccessNotFoundException, DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, null, null, null, null);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_privateHasRole_succeeds() throws UserNotFoundException,
            AccessNotFoundException, DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"list-views"})
    public void findAll_privateHasRoleHasAccess_succeeds() throws UserNotFoundException,
            AccessNotFoundException, DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_1_USER_2_READ_ACCESS);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void findAll_privateNoRole_succeeds() throws UserNotFoundException,
            AccessNotFoundException, DatabaseNotFoundException {

        /* test */
        findAll_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
    }

    @Test
    @WithAnonymousUser
    public void create_privateAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, "View", null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_privateHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, "View", USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"create-database-view"})
    public void create_privateHasRoleHasAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, "View", USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void create_privateNoRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(DATABASE_1_ID, DATABASE_1, "View", USER_2_PRINCIPAL, USER_2_ID, USER_2, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_privateAnonymous_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_1_ID, DATABASE_1, null, null, null, null);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"find-database-view"})
    public void find_privateHasRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void find_privateNoRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void find_privateHasRoleHasAccess_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* test */
        find_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
    }

    @Test
    @WithAnonymousUser
    public void delete_privateAnonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(DATABASE_1_ID, DATABASE_1, VIEW_1_ID, VIEW_1, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"delete-database-view"})
    public void delete_privateHasRole_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(DATABASE_1_ID, DATABASE_1, VIEW_1_ID, VIEW_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME)
    public void delete_privateNoRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(DATABASE_1_ID, DATABASE_1, VIEW_1_ID, VIEW_1, USER_2_PRINCIPAL, USER_2_ID, USER_2, DATABASE_2.getAccesses().get(0));
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-database-view"})
    public void delete_privateOwner_succeeds() throws NotAllowedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, ViewNotFoundException, UserNotFoundException, DashboardServiceException,
            DashboardServiceConnectionException {

        /* test */
        delete_generic(DATABASE_1_ID, DATABASE_1, VIEW_1_ID, VIEW_1, USER_1_PRINCIPAL, USER_1_ID, USER_1, DATABASE_1_USER_1_WRITE_ALL_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            update_generic(USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void update_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            update_generic(USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"modify-view-visibility"})
    public void update_notOwner_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            update_generic(USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-view-visibility"})
    public void update_succeeds() throws NotAllowedException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, ViewNotFoundException, UserNotFoundException,
            DashboardServiceException, DashboardServiceConnectionException {

        /* test */
        update_generic(USER_1_PRINCIPAL);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void findAll_generic(UUID databaseId, Database database, Principal principal, UUID userId, User user,
                                   DatabaseAccess access) throws AccessNotFoundException, UserNotFoundException,
            DatabaseNotFoundException {

        /* mock */
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        if (principal != null) {
            when(userService.findById(userId))
                    .thenReturn(user);
        }
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenThrow(AccessNotFoundException.class);
        }

        /* test */
        final ResponseEntity<List<ViewBriefDto>> response = viewEndpoint.findAll(databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void create_generic(UUID databaseId, Database database, String viewName, Principal principal, UUID userId,
                                  User user, DatabaseAccess access) throws MalformedException, DataServiceException,
            DataServiceConnectionException, NotAllowedException, UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, SearchServiceException, SearchServiceConnectionException, TableNotFoundException,
            ImageNotFoundException, ViewExistsException, DashboardServiceException,
            DashboardServiceConnectionException {
        final CreateViewDto request = CreateViewDto.builder()
                .name(viewName)
                .query(VIEW_1_SUBSET_DTO)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        if (principal != null) {
            when(userService.findById(userId))
                    .thenReturn(user);
        }
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenThrow(AccessNotFoundException.class);
        }
        when(viewService.create(database, user, request))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewBriefDto> response = viewEndpoint.create(databaseId, request, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        assertEquals(VIEW_1_NAME, response.getBody().getName());
    }

    protected ResponseEntity<ViewDto> find_generic(UUID databaseId, Database database, Principal principal,
                                                   UUID userId, User user, DatabaseAccess access)
            throws DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException, ViewNotFoundException, NotAllowedException {

        /* mock */
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenThrow(AccessNotFoundException.class);
        }
        if (principal != null) {
            when(userService.findById(userId))
                    .thenReturn(user);
            when(viewService.findById(any(Database.class), any(UUID.class)))
                    .thenReturn(VIEW_5);
        } else {
            when(viewService.findById(any(Database.class), any(UUID.class)))
                    .thenReturn(VIEW_5);
        }

        /* test */
        final ResponseEntity<ViewDto> response = viewEndpoint.find(databaseId, VIEW_5_ID, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_5_ID, response.getBody().getId());
        return response;
    }

    protected void delete_generic(UUID databaseId, Database database, UUID viewId, View view, Principal principal,
                                  UUID userId, User user, DatabaseAccess access) throws NotAllowedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, AccessNotFoundException,
            SearchServiceException, SearchServiceConnectionException, ViewNotFoundException, UserNotFoundException,
            DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and user id {}", databaseId, userId);
            when(accessService.find(database, user))
                    .thenThrow(AccessNotFoundException.class);
        }
        doNothing()
                .when(viewService)
                .delete(view);
        doNothing()
                .when(dashboardService)
                .update(database);

        /* test */
        final ResponseEntity<?> response = viewEndpoint.delete(databaseId, viewId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    protected void update_generic(Principal principal) throws SearchServiceException, NotAllowedException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException,
            ViewNotFoundException, UserNotFoundException, DashboardServiceException,
            DashboardServiceConnectionException {
        final ViewUpdateDto request = ViewUpdateDto.builder()
                .isPublic(true)
                .isSchemaPublic(true)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(viewService.findById(DATABASE_1, VIEW_1_ID))
                .thenReturn(VIEW_1);
        when(viewService.update(VIEW_1, request))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewBriefDto> response = viewEndpoint.update(DATABASE_1_ID, VIEW_1_ID, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
