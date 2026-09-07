package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationStatus;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplicationOutboxEndpointUnitTest {

    @Mock
    private ReplicationService replicationService;

    private ReplicationOutboxEndpoint endpoint;

    @BeforeEach
    public void beforeEach() {
        endpoint = new ReplicationOutboxEndpoint(replicationService);
    }

    @Test
    public void list_succeeds() {
        final List<ReplicationNotificationOutbox> entries = List.of(entry());
        when(replicationService.findOutboxEntries())
                .thenReturn(entries);

        final ResponseEntity<List<ReplicationNotificationOutbox>> response = endpoint.list();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(entries, response.getBody());
        verify(replicationService).findOutboxEntries();
    }

    @Test
    public void retryDue_succeeds() {
        when(replicationService.retryDueOutboxEntries())
                .thenReturn(3);

        final ResponseEntity<Map<String, Object>> response = endpoint.retryDue();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().get("retried"));
        verify(replicationService).retryDueOutboxEntries();
    }

    @Test
    public void retry_succeeds() {
        final UUID id = UUID.randomUUID();
        when(replicationService.retryOutboxEntry(id))
                .thenReturn(true);

        final ResponseEntity<Map<String, Object>> response = endpoint.retry(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("retried"));
        verify(replicationService).retryOutboxEntry(id);
    }

    private ReplicationNotificationOutbox entry() {
        return ReplicationNotificationOutbox.builder()
                .id(UUID.randomUUID())
                .notificationType(ReplicationNotificationType.DATABASE_CREATE)
                .status(ReplicationNotificationStatus.PENDING)
                .httpMethod(HttpMethod.POST.name())
                .path("/api/replication/database")
                .aggregateId(UUID.randomUUID())
                .payload("{}")
                .attempts(0)
                .created(Instant.now())
                .nextAttemptAt(Instant.now())
                .build();
    }
}
