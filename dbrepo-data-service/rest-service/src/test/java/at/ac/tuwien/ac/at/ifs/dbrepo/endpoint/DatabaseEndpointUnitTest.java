package at.ac.tuwien.ac.at.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ac.at.ifs.dbrepo.endpoints.DatabaseEndpoint;
import at.ac.tuwien.ac.at.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ac.at.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ac.at.ifs.dbrepo.service.ContainerService;
import at.ac.tuwien.ac.at.ifs.dbrepo.service.DatabaseService;
import lombok.extern.log4j.Log4j2;
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
public class DatabaseEndpointUnitTest extends BaseTest {

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @MockBean
    private ContainerService containerService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private CacheService credentialService;

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseUnavailableException, RemoteUnavailableException,
            QueryStoreCreateException, ContainerNotFoundException, DatabaseMalformedException,
            MetadataServiceException, SQLException {

        /* mock */
        when(credentialService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);
        when(containerService.createDatabase(CONTAINER_1_DTO, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(containerService)
                .createQueryStore(CONTAINER_1_DTO, DATABASE_1_INTERNAL_NAME);
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_PRIVILEGED_DTO), any(UserDto.class), any(AccessTypeDto.class));

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
                .thenReturn(CONTAINER_1_DTO);
        when(containerService.createDatabase(CONTAINER_1_DTO, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doNothing()
                .when(containerService)
                .createQueryStore(CONTAINER_1_DTO, DATABASE_1_INTERNAL_NAME);
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_PRIVILEGED_DTO), any(UserDto.class), any(AccessTypeDto.class));

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
                .thenReturn(CONTAINER_1_DTO);
        doThrow(SQLException.class)
                .when(containerService)
                .createDatabase(CONTAINER_1_DTO, DATABASE_1_CREATE_INTERNAL);

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
        when(containerService.createDatabase(CONTAINER_1_DTO, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(QueryStoreCreateException.class)
                .when(containerService)
                .createQueryStore(CONTAINER_1_DTO, DATABASE_1_INTERNAL_NAME);

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
