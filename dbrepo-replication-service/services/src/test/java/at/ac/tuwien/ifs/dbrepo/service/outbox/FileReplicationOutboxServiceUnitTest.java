package at.ac.tuwien.ifs.dbrepo.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @ParameterizedTest
    @ValueSource(strings = {"broken-json", "null", "{}", "[null]", ""})
    public void corruptOutboxMustNotBeReportedEmptyOrOverwritten(String content) throws Exception {
        final Path path = tempDir.resolve("outbox.json");
        Files.writeString(path, content);
        final FileReplicationOutboxService service = service();

        assertThrows(UncheckedIOException.class, service::findAll);
        assertThrows(UncheckedIOException.class, () -> enqueue(service));

        assertEquals(content, Files.readString(path));
    }

    @Test
    public void unreadableOutboxMustNotBeReportedEmpty() throws Exception {
        Files.createDirectory(tempDir.resolve("outbox.json"));
        assertThrows(UncheckedIOException.class, () -> service().findAll());
    }

    @Test
    public void failedEnqueueMustPreserveExistingBacklogAndPropagate() throws Exception {
        final FileReplicationOutboxService service = service();
        final ReplicationOutboxEntry original = enqueue(service);
        final String persisted = Files.readString(tempDir.resolve("outbox.json"));
        Files.createDirectory(tempDir.resolve("outbox.json.tmp"));

        assertThrows(UncheckedIOException.class, () -> enqueue(service));

        assertEquals(persisted, Files.readString(tempDir.resolve("outbox.json")));
        assertEquals(original.getId(), service().findAll().getFirst().getId());
    }

    @Test
    public void failedStatusWriteMustRemainPendingAfterRestart() throws Exception {
        final FileReplicationOutboxService service = service();
        final ReplicationOutboxEntry entry = enqueue(service);
        Files.createDirectory(tempDir.resolve("outbox.json.tmp"));

        assertThrows(UncheckedIOException.class, () -> service.markSucceeded(entry.getId()));

        assertEquals(ReplicationOutboxStatus.PENDING, service().findById(entry.getId()).orElseThrow().getStatus());
        Files.delete(tempDir.resolve("outbox.json.tmp"));
        service.markSucceeded(entry.getId());
        assertEquals(ReplicationOutboxStatus.SUCCEEDED, service().findById(entry.getId()).orElseThrow().getStatus());
    }

    private ReplicationOutboxEntry enqueue(FileReplicationOutboxService service) {
        return service.enqueue(ReplicationOutboxOperationType.DATABASE_CREATE, "https://peer.example",
                HttpMethod.POST, Map.of("name", "test"), UUID.randomUUID(), null, null, null, "timeout");
    }
}
