package at.tuwien.endpoint;

import at.tuwien.endpoints.AccessEndpoint;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.AccessService;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessEndpointUnitTest extends AbstractUnitTest {

    @Autowired
    private AccessEndpoint accessEndpoint;

    @MockBean
    private AccessService accessService;

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void create_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, RemoteUnavailableException, DatabaseMalformedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getUserById(USER_4_ID))
                .thenReturn(USER_4_PRIVILEGED_DTO);

        /* test */
        accessEndpoint.create(DATABASE_1_ID, USER_4_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void create_alreadyAccess_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_PRIVILEGED_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void create_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void create_userNotFound_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(UserNotFoundException.class)
                .when(metadataServiceGateway)
                .getUserById(USER_1_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
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
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void update_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException, UserNotFoundException,
            NotAllowedException, QueryMalformedException, DatabaseMalformedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_PRIVILEGED_DTO);

        /* test */
        accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
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
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void update_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void update_userNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(UserNotFoundException.class)
                .when(metadataServiceGateway)
                .getUserById(USER_1_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void revoke_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, RemoteUnavailableException, DatabaseMalformedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_PRIVILEGED_DTO);

        /* test */
        accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
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
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void revoke_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void revoke_userNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            UserNotFoundException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(UserNotFoundException.class)
                .when(metadataServiceGateway)
                .getUserById(USER_1_ID);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID);
        });
    }

}
