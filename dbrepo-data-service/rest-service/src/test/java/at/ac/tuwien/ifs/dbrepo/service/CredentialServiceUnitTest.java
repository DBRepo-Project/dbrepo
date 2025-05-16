package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.impl.CacheServiceImpl;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CredentialServiceUnitTest extends BaseTest {

    @Autowired
    private CacheServiceImpl credentialService;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* cache */
        credentialService.invalidateAll();
    }

    @Test
    public void getDatabase_notCached_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final DatabaseDto response = credentialService.getDatabase(DATABASE_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void getDatabase_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_DTO)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        credentialService.getDatabase(DATABASE_1_ID);

        /* test */
        final DatabaseDto response = credentialService.getDatabase(DATABASE_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void getDatabase_invalidCacheReload_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException, InterruptedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_2_DTO) /* needs to be different id for test case */
                .thenReturn(DATABASE_1_DTO);

        /* pre-condition */
        final DatabaseDto tmp = credentialService.getDatabase(DATABASE_1_ID);
        assertNotEquals(DATABASE_1_ID, tmp.getId());
        Thread.sleep(5000);

        /* test */
        final DatabaseDto response = credentialService.getDatabase(DATABASE_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void getContainer_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException {

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO);

        /* test */
        final ContainerDto response = credentialService.getContainer(CONTAINER_1_ID);
        assertNotNull(response);
        assertEquals(CONTAINER_1_ID, response.getId());
    }

    @Test
    public void getContainer_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException {

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_DTO)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        credentialService.getContainer(CONTAINER_1_ID);

        /* test */
        final ContainerDto response = credentialService.getContainer(CONTAINER_1_ID);
        assertNotNull(response);
        assertEquals(CONTAINER_1_ID, response.getId());
    }

    @Test
    public void getContainer_invalidCacheReload_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            InterruptedException, ContainerNotFoundException {

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_2_DTO) /* needs to be different id for test case */
                .thenReturn(CONTAINER_1_DTO);

        /* pre-condition */
        final ContainerDto tmp = credentialService.getContainer(CONTAINER_1_ID);
        assertNotEquals(CONTAINER_1_ID, tmp.getId());
        Thread.sleep(5000);

        /* test */
        final ContainerDto response = credentialService.getContainer(CONTAINER_1_ID);
        assertNotNull(response);
        assertEquals(CONTAINER_1_ID, response.getId());
    }

    @Test
    public void getUser_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException {

        /* mock */
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        final UserDto response = credentialService.getUser(USER_1_ID);
        assertNotNull(response);
        assertEquals(USER_1_ID, response.getId());
    }

    @Test
    public void getUser_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException {

        /* mock */
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        credentialService.getUser(USER_1_ID);

        /* test */
        final UserDto response = credentialService.getUser(USER_1_ID);
        assertNotNull(response);
        assertEquals(USER_1_ID, response.getId());
    }

    @Test
    public void getUser_invalidCacheReload_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            InterruptedException, UserNotFoundException {

        /* mock */
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_2_DTO) /* needs to be different id for test case */
                .thenReturn(USER_1_DTO);

        /* pre-condition */
        final UserDto tmp = credentialService.getUser(USER_1_ID);
        assertNotEquals(USER_1_ID, tmp.getId());
        Thread.sleep(5000);

        /* test */
        final UserDto response = credentialService.getUser(USER_1_ID);
        assertNotNull(response);
        assertEquals(USER_1_ID, response.getId());
    }

    @Test
    public void getAccess_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO);

        /* test */
        final DatabaseAccessDto response = credentialService.getAccess(DATABASE_1_ID, USER_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getHdbid());
        assertEquals(USER_1_ID, response.getHuserid());
    }

    @Test
    public void getAccess_succeeds() throws RemoteUnavailableException, MetadataServiceException, NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        credentialService.getAccess(DATABASE_1_ID, USER_1_ID);

        /* test */
        final DatabaseAccessDto response = credentialService.getAccess(DATABASE_1_ID, USER_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getHdbid());
        assertEquals(USER_1_ID, response.getHuserid());
    }

    @Test
    public void getAccess_invalidCacheReload_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            InterruptedException, NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID))
                .thenReturn(DATABASE_2_USER_2_READ_ACCESS_DTO) /* needs to be different id for test case */
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS_DTO);

        /* pre-condition */
        final DatabaseAccessDto tmp = credentialService.getAccess(DATABASE_1_ID, USER_1_ID);
        assertNotEquals(DATABASE_1_ID, tmp.getHdbid());
        assertNotEquals(USER_1_ID, tmp.getHuserid());
        Thread.sleep(5000);

        /* test */
        final DatabaseAccessDto response = credentialService.getAccess(DATABASE_1_ID, USER_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getHdbid());
        assertEquals(USER_1_ID, response.getHuserid());
    }

    @Test
    public void getTable_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            TableNotFoundException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO);

        /* test */
        final TableDto response = credentialService.getTable(DATABASE_1_ID, TABLE_1_ID);
        assertNotNull(response);
        assertEquals(TABLE_1_ID, response.getId());
    }

    @Test
    public void getTable_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            TableNotFoundException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_DTO)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        credentialService.getTable(DATABASE_1_ID, TABLE_1_ID);

        /* test */
        final TableDto response = credentialService.getTable(DATABASE_1_ID, TABLE_1_ID);
        assertNotNull(response);
        assertEquals(TABLE_1_ID, response.getId());
    }

    @Test
    public void getTable_invalidCacheReload_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            InterruptedException, TableNotFoundException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_2_DTO) /* needs to be different id for test case */
                .thenReturn(TABLE_1_DTO);

        /* pre-condition */
        final TableDto tmp = credentialService.getTable(DATABASE_1_ID, TABLE_1_ID);
        assertNotEquals(TABLE_1_ID, tmp.getId());
        Thread.sleep(5000);

        /* test */
        final TableDto response = credentialService.getTable(DATABASE_1_ID, TABLE_1_ID);
        assertNotNull(response);
        assertEquals(TABLE_1_ID, response.getId());
    }

    @Test
    public void getView_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            ViewNotFoundException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO);

        /* test */
        final ViewDto response = credentialService.getView(DATABASE_1_ID, VIEW_1_ID);
        assertNotNull(response);
        assertEquals(VIEW_1_ID, response.getId());
    }

    @Test
    public void getView_succeeds() throws RemoteUnavailableException, MetadataServiceException, ViewNotFoundException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_DTO)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        credentialService.getView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        final ViewDto response = credentialService.getView(DATABASE_1_ID, VIEW_1_ID);
        assertNotNull(response);
        assertEquals(VIEW_1_ID, response.getId());
    }

    @Test
    public void getView_invalidCacheReload_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            InterruptedException, ViewNotFoundException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_2_DTO) /* needs to be different id for test case */
                .thenReturn(VIEW_1_DTO);

        /* pre-condition */
        final ViewDto tmp = credentialService.getView(DATABASE_1_ID, VIEW_1_ID);
        assertNotEquals(VIEW_1_ID, tmp.getId());
        Thread.sleep(5000);

        /* test */
        final ViewDto response = credentialService.getView(DATABASE_1_ID, VIEW_1_ID);
        assertNotNull(response);
        assertEquals(VIEW_1_ID, response.getId());
    }

}
