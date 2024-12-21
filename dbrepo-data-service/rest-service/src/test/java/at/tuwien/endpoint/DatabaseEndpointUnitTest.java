package at.tuwien.endpoint;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.user.internal.PrivilegedUserDto;
import at.tuwien.endpoints.DatabaseEndpoint;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import at.tuwien.service.CredentialService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.SubsetService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseEndpointUnitTest extends AbstractUnitTest {

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @MockBean
    private SubsetService queryService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private CredentialService credentialService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseUnavailableException, RemoteUnavailableException,
            QueryStoreCreateException, ContainerNotFoundException, DatabaseMalformedException,
            MetadataServiceException, SQLException {

        /* mock */
        when(credentialService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(databaseService.create(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(queryService)
                .createQueryStore(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_PRIVILEGED_DTO), any(PrivilegedUserDto.class), any(AccessTypeDto.class));

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() throws RemoteUnavailableException, ContainerNotFoundException,
            SQLException, QueryStoreCreateException, DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(credentialService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        when(databaseService.create(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(queryService)
                .createQueryStore(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_PRIVILEGED_DTO), any(PrivilegedUserDto.class), any(AccessTypeDto.class));

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_unavailable_fails() throws RemoteUnavailableException, ContainerNotFoundException,
            SQLException, DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(credentialService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(databaseService)
                .create(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_containerNotFound_fails() throws RemoteUnavailableException, ContainerNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(ContainerNotFoundException.class)
                .when(credentialService)
                .getContainer(CONTAINER_1_ID);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_queryStore_fails() throws RemoteUnavailableException, ContainerNotFoundException, SQLException,
            DatabaseMalformedException, QueryStoreCreateException, MetadataServiceException {

        /* mock */
        doThrow(ContainerNotFoundException.class)
                .when(credentialService)
                .getContainer(CONTAINER_1_ID);
        when(databaseService.create(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(QueryStoreCreateException.class)
                .when(queryService)
                .createQueryStore(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_succeeds() throws DatabaseUnavailableException, RemoteUnavailableException,
            DatabaseMalformedException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_unavailable_fails() throws RemoteUnavailableException, DatabaseMalformedException,
            DatabaseNotFoundException, MetadataServiceException, SQLException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(databaseService)
                .update(DATABASE_1_PRIVILEGED_DTO, USER_1_UPDATE_PASSWORD_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void update_noRole_fails() throws RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_databaseNotFound_fails() throws RemoteUnavailableException, DatabaseNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(credentialService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_password_fails() throws RemoteUnavailableException, DatabaseNotFoundException, SQLException,
            DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(credentialService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(DatabaseMalformedException.class)
                .when(databaseService)
                .update(DATABASE_1_PRIVILEGED_DTO, USER_1_UPDATE_PASSWORD_DTO);

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

}
