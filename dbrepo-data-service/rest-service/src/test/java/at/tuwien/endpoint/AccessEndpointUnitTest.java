package at.tuwien.endpoint;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.endpoints.AccessEndpoint;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.CredentialService;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessEndpointUnitTest extends AbstractUnitTest {

    @Autowired
    private AccessEndpoint accessEndpoint;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private AccessService accessService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws UserNotFoundException, NotAllowedException, DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_4_ID))
                .thenReturn(USER_4_DTO);

        /* test */
        final ResponseEntity<Void> response = accessEndpoint.create(DATABASE_1_ID, USER_4_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_alreadyAccess_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_unavailable_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException, SQLException, DatabaseMalformedException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_4_ID))
                .thenReturn(USER_4_DTO);
        doThrow(SQLException.class)
                .when(accessService)
                .create(DATABASE_1_DTO, USER_4_DTO, AccessTypeDto.READ);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_4_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(credentialService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_userNotFound_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        doThrow(UserNotFoundException.class)
                .when(credentialService)
                .getUser(USER_4_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_4_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            NotAllowedException, DatabaseUnavailableException, DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        final ResponseEntity<Void> response = accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException, SQLException,
            UserNotFoundException, DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);
        doThrow(SQLException.class)
                .when(accessService)
                .update(DATABASE_1_DTO, USER_1_DTO, AccessTypeDto.READ);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_noAccess_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_4_ID))
                .thenReturn(USER_4_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_4_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void update_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(credentialService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_userNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        doThrow(UserNotFoundException.class)
                .when(credentialService)
                .getUser(USER_1_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_succeeds() throws UserNotFoundException, NotAllowedException, DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, DatabaseMalformedException, MetadataServiceException,
            SQLException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);
        doNothing()
                .when(accessService)
                .delete(any(DatabaseDto.class), any(UserDto.class));

        /* test */
        final ResponseEntity<Void> response = accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_noAccess_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_4_ID))
                .thenReturn(USER_4_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_4_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void revoke_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(credentialService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_userNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        doThrow(UserNotFoundException.class)
                .when(credentialService)
                .getUser(USER_1_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void revoke_unavailable_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException, MetadataServiceException, SQLException, DatabaseMalformedException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);
        when(credentialService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);
        doThrow(SQLException.class)
                .when(accessService)
                .delete(DATABASE_1_DTO, USER_1_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
        });
    }

}
