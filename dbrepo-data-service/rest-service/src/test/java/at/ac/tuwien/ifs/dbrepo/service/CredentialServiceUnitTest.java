package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.ContainerCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.TableCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.ViewCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class CredentialServiceUnitTest extends BaseTest {

    @Autowired
    private MetadataService metadataService;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockitoBean
    private TableCacheRepository tableRepository;

    @MockitoBean
    private ContainerCacheRepository containerRepository;

    @MockitoBean
    private DatabaseCacheRepository databaseRepository;

    @MockitoBean
    private ViewCacheRepository viewRepository;

    @org.testcontainers.junit.jupiter.Container
    private static RedisContainerConfig.CustomRedisContainer redisContainer = RedisContainerConfig.getContainer();

    @Test
    public void getDatabase_notCached_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        final Database response = metadataService.getDatabase(DATABASE_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void getDatabase_cached_succeeds() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1_CACHE));

        /* test */
        final Database response = metadataService.getDatabase(DATABASE_1_ID);
        assertNotNull(response);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void getContainer_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException {

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_CACHE);
        when(containerRepository.save(any(Container.class)))
                .thenReturn(CONTAINER_1_CACHE);

        /* test */
        final Container response = metadataService.getContainer(CONTAINER_1_ID);
        assertNotNull(response);
        assertEquals(CONTAINER_1_ID, response.getId());
    }

    @Test
    public void getContainer_cached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException {

        /* mock */
        when(metadataServiceGateway.getContainerById(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_CACHE)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        when(containerRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.of(CONTAINER_1_CACHE));

        /* test */
        final Container response = metadataService.getContainer(CONTAINER_1_ID);
        assertNotNull(response);
        assertEquals(CONTAINER_1_ID, response.getId());
    }

    @Test
    public void getTable_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            TableNotFoundException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(tableRepository.save(any(Table.class)))
                .thenReturn(TABLE_1_CACHE);

        /* test */
        final Table response = metadataService.getTable(DATABASE_1_ID, TABLE_1_ID);
        assertNotNull(response);
        assertEquals(TABLE_1_ID, response.getId());
    }

    @Test
    public void getTable_cached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            TableNotFoundException {

        /* mock */
        doThrow(RuntimeException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_1_ID, TABLE_1_ID) /* should never be thrown */;
        when(tableRepository.findById(TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1_CACHE));

        /* test */
        final Table response = metadataService.getTable(DATABASE_1_ID, TABLE_1_ID);
        assertNotNull(response);
        assertEquals(TABLE_1_ID, response.getId());
    }

    @Test
    public void getView_notCached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            ViewNotFoundException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE);
        when(viewRepository.save(any(View.class)))
                .thenReturn(VIEW_1_CACHE);

        /* test */
        final View response = metadataService.getView(DATABASE_1_ID, VIEW_1_ID);
        assertNotNull(response);
        assertEquals(VIEW_1_ID, response.getId());
    }

    @Test
    public void getView_cached_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            ViewNotFoundException {

        /* mock */
        when(metadataServiceGateway.getViewById(DATABASE_1_ID, VIEW_1_ID))
                .thenReturn(VIEW_1_CACHE)
                .thenThrow(RuntimeException.class) /* should never be thrown */;
        when(viewRepository.findById(VIEW_1_ID))
                .thenReturn(Optional.of(VIEW_1_CACHE));

        /* test */
        final View response = metadataService.getView(DATABASE_1_ID, VIEW_1_ID);
        assertNotNull(response);
        assertEquals(VIEW_1_ID, response.getId());
    }

}
