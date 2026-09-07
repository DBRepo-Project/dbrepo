package at.ac.tuwien.ifs.dbrepo.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileReplicationOutboxServiceUnitTest {

    @TempDir
    Path tempDir;

    @Test
    public void enqueueShouldPersistPendingEntry() {
        final FileReplicationOutboxService service = service();
        final UUID databaseId = UUID.randomUUID();

        final ReplicationOutboxEntry entry = service.enqueue(ReplicationOutboxOperationType.DATABASE_CREATE,
                "http://site-a", HttpMethod.POST, Map.of("id", databaseId.toString()), databaseId, null, null, null,
                "connection refused");

        assertNotNull(entry.getId());
        assertEquals(ReplicationOutboxStatus.PENDING, entry.getStatus());
        assertEquals(1, service.findAll().size());
        assertEquals(1, service.findDue(Instant.now(), 10).size());
    }

    @Test
    public void markSucceededShouldRemoveEntryFromDueRetries() {
        final FileReplicationOutboxService service = service();
        final ReplicationOutboxEntry entry = service.enqueue(ReplicationOutboxOperationType.VIEW_CREATE,
                "http://site-a", HttpMethod.POST, Map.of("name", "view"), UUID.randomUUID(), null,
                UUID.randomUUID(), null, "timeout");

        service.markSucceeded(entry.getId());

        assertEquals(ReplicationOutboxStatus.SUCCEEDED, service.findById(entry.getId()).orElseThrow().getStatus());
        assertTrue(service.findDue(Instant.now(), 10).isEmpty());
    }

    @Test
    public void markFailedShouldScheduleRetryUntilMaxAttempts() {
        final FileReplicationOutboxService service = service();
        final ReplicationOutboxEntry entry = service.enqueue(ReplicationOutboxOperationType.DATA_CREATE,
                "http://site-a", HttpMethod.POST, Map.of("key", "tuple-1"), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "timeout");

        service.markFailed(entry.getId(), "still down", Duration.ofMinutes(5), 2);

        final ReplicationOutboxEntry pending = service.findById(entry.getId()).orElseThrow();
        assertEquals(ReplicationOutboxStatus.PENDING, pending.getStatus());
        assertEquals(1, pending.getAttempts());
        assertFalse(service.findDue(Instant.now(), 10).contains(pending));

        service.markFailed(entry.getId(), "still down", Duration.ofMinutes(5), 2);

        final ReplicationOutboxEntry failed = service.findById(entry.getId()).orElseThrow();
        assertEquals(ReplicationOutboxStatus.FAILED, failed.getStatus());
        assertEquals(2, failed.getAttempts());
    }

    private FileReplicationOutboxService service() {
        return new FileReplicationOutboxService(new ObjectMapper().findAndRegisterModules(),
                tempDir.resolve("outbox.json").toString());
    }
}
