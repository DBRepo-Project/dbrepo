package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.AccessEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessEndpointUnitTest extends BaseTest {

    @Autowired
    private AccessEndpoint accessEndpoint;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private AccessService accessService;

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws UserNotFoundException, NotAllowedException, DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, DatabaseMalformedException, MetadataServiceException,
            AccessNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_4_USERNAME))
                .thenReturn(USER_4_CACHE);

        /* test */
        final ResponseEntity<Void> response = accessEndpoint.create(DATABASE_1_ID, USER_4_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_alreadyAccess_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_unavailable_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException, SQLException, DatabaseMalformedException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_4_USERNAME))
                .thenReturn(USER_4_CACHE);
        doThrow(SQLException.class)
                .when(accessService)
                .create(DATABASE_1_CACHE, USER_4_CACHE, AccessTypeDto.READ);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_4_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_userNotFound_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(UserNotFoundException.class)
                .when(metadataService)
                .getUser(USER_4_USERNAME);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_4_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            DatabaseUnavailableException, DatabaseMalformedException, MetadataServiceException,
            AccessNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);

        /* test */
        final ResponseEntity<Void> response = accessEndpoint.update(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            UserNotFoundException, DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);
        doThrow(SQLException.class)
                .when(accessService)
                .update(DATABASE_1_CACHE, USER_1_CACHE, AccessTypeDto.READ);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_noAccess_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_4_USERNAME))
                .thenReturn(USER_4_CACHE);

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_4_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void update_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_userNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(UserNotFoundException.class)
                .when(metadataService)
                .getUser(USER_1_USERNAME);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_USERNAME, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_succeeds() throws UserNotFoundException, DatabaseUnavailableException, DatabaseNotFoundException,
            RemoteUnavailableException, DatabaseMalformedException, MetadataServiceException, SQLException,
            AccessNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);
        doNothing()
                .when(accessService)
                .delete(any(Database.class), any(User.class));

        /* test */
        final ResponseEntity<Void> response = accessEndpoint.revoke(DATABASE_1_ID, USER_1_USERNAME);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_noAccess_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_4_USERNAME))
                .thenReturn(USER_4_CACHE);

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_4_USERNAME);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void revoke_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_userNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(UserNotFoundException.class)
                .when(metadataService)
                .getUser(USER_1_USERNAME);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException, SQLException, DatabaseMalformedException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);
        doThrow(SQLException.class)
                .when(accessService)
                .delete(DATABASE_1_CACHE, USER_1_CACHE);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

}
