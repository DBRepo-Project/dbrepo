package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.mdb.DatabaseAccessRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ExportEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private QueryService queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private TableRepository tableRepository;

    @Autowired
    private ExportEndpoint exportEndpoint;

    @Test
    @WithAnonymousUser
    public void export_anonymous_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"export-table-data"})
    public void export_publicHasRoleNoAccess_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, null, USER_1_PRINCIPAL, USER_1_USERNAME, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"export-table-data"})
    public void export_publicHasRoleReadAccess_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, null, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_1_USER_1_READ_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void export_publicReadWithTimestamp_succeeds() {
        final Instant timestamp = Instant.now();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, timestamp, null, null, null);
        });
    }

    @Test
    public void export_publicReadWithTimestampInFuture_succeeds() {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, DATABASE_1, timestamp, null, null, null);
        });
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void export_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_privateHasRoleNoAccess_fails() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, null, USER_2_PRINCIPAL, USER_2_USERNAME, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_HasRoleReadAccess_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, null, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_2_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_privateReadWithTimestamp_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException {
        final Instant timestamp = Instant.now();

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, timestamp, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_privateReadWithTimestampInFuture_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_1_ID, DATABASE_2, timestamp, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_2_USER_1_READ_ACCESS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void export_generic(Long containerId, Long databaseId, Long tableId, Database database, Instant timestamp,
                                  Principal principal, String username, DatabaseAccess access) throws IOException,
            DatabaseNotFoundException, UserNotFoundException, TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, QueryMalformedException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException {
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(tableRepository.find(containerId, databaseId, tableId))
                .thenReturn(Optional.of(TABLE_1));
        when(queryService.tableFindAll(containerId, databaseId, tableId, timestamp, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<InputStreamResource> response = exportEndpoint.export(containerId, databaseId, tableId,
                timestamp, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
