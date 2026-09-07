package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.ReplicationOutboxEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplicationOutboxEndpointUnitTest extends BaseTest {

    @Mock
    private MetadataService metadataService;

    @Mock
    private ReplicationService replicationService;

    private ReplicationOutboxEndpoint endpoint;

    @BeforeEach
    public void beforeEach() {
        endpoint = new ReplicationOutboxEndpoint(metadataService, replicationService);
    }

    @Test
    public void list_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException, SQLException {
        final Database database = DATABASE_3_CACHE;
        final List<TupleReplicationOutboxEntry> entries = List.of(entry(database.getId()));
        when(metadataService.getDatabase(database.getId()))
                .thenReturn(database);
        when(replicationService.findOutboxEntries(database))
                .thenReturn(entries);

        final ResponseEntity<List<TupleReplicationOutboxEntry>> response = endpoint.list(database.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(entries, response.getBody());
        verify(replicationService).findOutboxEntries(database);
    }

    @Test
    public void retryDue_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException {
        final Database database = DATABASE_3_CACHE;
        when(metadataService.getDatabase(database.getId()))
                .thenReturn(database);
        when(replicationService.retryDueOutboxEntries(database))
                .thenReturn(2);

        final ResponseEntity<Map<String, Object>> response = endpoint.retryDue(database.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().get("retried"));
        verify(replicationService).retryDueOutboxEntries(database);
    }

    @Test
    public void retry_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException {
        final Database database = DATABASE_3_CACHE;
        final UUID id = UUID.randomUUID();
        when(metadataService.getDatabase(database.getId()))
                .thenReturn(database);
        when(replicationService.retryOutboxEntry(database, id))
                .thenReturn(true);

        final ResponseEntity<Map<String, Object>> response = endpoint.retry(database.getId(), id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("retried"));
        verify(replicationService).retryOutboxEntry(database, id);
    }

    private TupleReplicationOutboxEntry entry(UUID databaseId) {
        return TupleReplicationOutboxEntry.builder()
                .id(UUID.randomUUID())
                .databaseId(databaseId)
                .tableId(TABLE_8_ID)
                .httpMethod(HttpMethod.POST)
                .payloadJson("{}")
                .status(TupleReplicationOutboxStatus.PENDING)
                .attempts(0)
                .created(Instant.now())
                .nextAttemptAt(Instant.now())
                .build();
    }
}
