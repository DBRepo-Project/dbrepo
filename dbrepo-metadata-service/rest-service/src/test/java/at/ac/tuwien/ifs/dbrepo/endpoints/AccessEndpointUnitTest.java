package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessEndpointUnitTest extends BaseTest {

    @MockBean
    private AccessService accessService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private UserService userService;

    @MockBean
    private DashboardService dashboardService;

    @Autowired
    private AccessEndpoint accessEndpoint;

    @Autowired
    private MetadataMapper metadataMapper;

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(null, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRoleNoAccess_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(USER_2_PRINCIPAL, USER_2, USER_4_ID, USER_4, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(USER_2_PRINCIPAL, USER_2, USER_4_ID, USER_4, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {"create-database-access"})
    public void create_notOwner_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(USER_2_PRINCIPAL, USER_2, USER_4_ID, USER_4, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-access"})
    public void create_alreadyAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(USER_1_PRINCIPAL, USER_1, USER_2_ID, USER_2, DATABASE_1_USER_2_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-access"})
    public void create_succeeds() throws DataServiceException, DataServiceConnectionException, NotAllowedException,
            DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(accessService.create(eq(DATABASE_1), eq(USER_2), any(AccessTypeDto.class)))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        generic_create(USER_1_PRINCIPAL, USER_1, USER_2_ID, USER_2, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access"})
    public void find_hasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            generic_find(DATABASE_1_ID, DATABASE_1, null, USER_2_PRINCIPAL, USER_2, USER_2_ID, USER_2);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access"})
    public void find_hasRoleHasAccess_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, NotAllowedException {

        /* test */
        generic_find(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, USER_1_PRINCIPAL, USER_1, USER_1_ID, USER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access"})
    public void find_hasRoleHasAccessForeign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_find(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, USER_1_PRINCIPAL, USER_1, USER_2_ID, USER_2);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access", "check-foreign-database-access"})
    public void find_hasRoleHasAccessForeign_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, AccessNotFoundException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_1_DETAILS, USER_1_PASSWORD, List.of(
                new SimpleGrantedAuthority("check-database-access"),
                new SimpleGrantedAuthority("check-foreign-database-access")));

        /* test */
        generic_find(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_2_READ_ACCESS, principal, USER_1, USER_2_ID, USER_2);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(null, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_hasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            generic_update(USER_1_PRINCIPAL, USER_1, USER_4_ID, USER_4, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void update_noRoleNoAccess_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(USER_4_PRINCIPAL, USER_4, USER_1_ID, USER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {"update-database-access"})
    public void update_notOwner_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(USER_4_PRINCIPAL, USER_4, USER_1_ID, USER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_succeeds() throws NotAllowedException, DataServiceException, DataServiceConnectionException,
            AccessNotFoundException, DatabaseNotFoundException, UserNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        doNothing()
                .when(accessService)
                .update(eq(DATABASE_1), eq(USER_2), any(AccessTypeDto.class));

        /* test */
        generic_update(USER_1_PRINCIPAL, USER_1, USER_2_ID, USER_2, DATABASE_1_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_ownerNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(USER_1_PRINCIPAL, USER_1, USER_1_ID, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_ownerNoWriteAllAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_update(USER_1_PRINCIPAL, USER_1, USER_LOCAL_ADMIN_ID, USER_LOCAL, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void revoke_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_revoke(null, null, USER_1_ID, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void revoke_noRoleNoAccess_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_revoke(USER_4_PRINCIPAL, USER_4, USER_1_ID, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {"delete-database-access"})
    public void revoke_notOwner_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_revoke(USER_4_PRINCIPAL, USER_4, USER_1_ID, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-database-access"})
    public void revoke_ownerNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_revoke(USER_1_PRINCIPAL, USER_1, USER_1_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-database-access"})
    public void revoke_ownerNoWriteAllAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_revoke(USER_1_PRINCIPAL, USER_1, USER_LOCAL_ADMIN_ID, USER_LOCAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-database-access"})
    public void revoke_succeeds() throws NotAllowedException, DataServiceException, DataServiceConnectionException,
            UserNotFoundException, DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        doNothing()
                .when(accessService)
                .delete(DATABASE_1, USER_2);

        /* test */
        generic_revoke(USER_1_PRINCIPAL, USER_1, USER_2_ID, USER_2);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(Principal principal, User principalUser, UUID userId, User user, DatabaseAccess access)
            throws NotAllowedException, DataServiceException, DataServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        if (access != null) {
            when(accessService.find(DATABASE_1, user))
                    .thenReturn(access);
        } else {
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(DATABASE_1, user);
        }
        if (principalUser != null) {
            when(userService.findByUsername(principal.getName()))
                    .thenReturn(principalUser);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findByUsername(anyString());
        }
        if (user != null) {
            when(userService.findById(userId))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findById(any(UUID.class));
        }
        when(dashboardService.create(DATABASE_1))
                .thenReturn(CreateDashboardResponseDto.builder()
                        .uid(DATABASE_1_DASHBOARD_UID)
                        .build());

        /* test */
        final ResponseEntity<?> response = accessEndpoint.create(DATABASE_1_ID, userId, UPDATE_DATABASE_ACCESS_READ_DTO, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    protected void generic_find(UUID databaseId, Database database, DatabaseAccess access, Principal principal,
                                User caller, UUID userId, User user) throws UserNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, NotAllowedException {

        /* mock */
        when(userService.findByUsername(principal.getName()))
                .thenReturn(caller);
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        when(userService.findById(userId))
                .thenReturn(user);
        if (access != null) {
            log.trace("mock access {} for user with id {} for database with id {}", access.getType(), userId, databaseId);
            when(accessService.find(database, user))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for user with id {} for database with id {}", userId, databaseId);
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(database, user);
        }

        /* test */
        final ResponseEntity<DatabaseAccessDto> response = accessEndpoint.find(databaseId, userId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final DatabaseAccessDto dto = response.getBody();
        assertEquals(userId, dto.getHuserid());
        assertEquals(databaseId, dto.getHdbid());
        if (access != null) {
            assertEquals(metadataMapper.accessTypeToAccessTypeDto(access.getType()), dto.getType());
        }
    }

    protected void generic_update(Principal principal, User principalUser, UUID userId, User user,
                                  DatabaseAccess access) throws NotAllowedException, DataServiceException,
            DataServiceConnectionException, AccessNotFoundException, UserNotFoundException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        if (access != null) {
            log.trace("mock access {} for user with id {} for database with id {}", access.getType(), userId, DATABASE_1_ID);
            when(accessService.find(DATABASE_1, user))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for user with id {} for database with id {}", userId, DATABASE_1_ID);
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(DATABASE_1, user);
        }
        if (userId != null) {
            when(userService.findById(userId))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findById(any(UUID.class));
        }
        if (principal != null) {
            when(userService.findByUsername(principal.getName()))
                    .thenReturn(principalUser);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findByUsername(anyString());
        }
        doNothing()
                .when(dashboardService)
                .updateAccess(DATABASE_1, user, AccessTypeDto.READ);

        /* test */
        final ResponseEntity<?> response = accessEndpoint.update(DATABASE_1_ID, userId, UPDATE_DATABASE_ACCESS_READ_DTO, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    protected void generic_revoke(Principal principal, User principalUser, UUID userId, User user)
            throws DataServiceConnectionException, NotAllowedException, DataServiceException, UserNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, SearchServiceException,
            SearchServiceConnectionException, DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        if (principal != null) {
            when(userService.findByUsername(principal.getName()))
                    .thenReturn(principalUser);
        }
        when(userService.findById(userId))
                .thenReturn(user);
        doNothing()
                .when(dashboardService)
                .updateAccess(DATABASE_1, user, null);

        /* test */
        final ResponseEntity<?> response = accessEndpoint.revoke(DATABASE_1_ID, userId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
