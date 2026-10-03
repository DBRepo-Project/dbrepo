package at.ac.tuwien.ifs.dbrepo.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
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

    private final java.util.List<FileReplicationOutboxService> opened = new java.util.ArrayList<>();

    @AfterEach
    void closeStores() throws Exception {
        for (var service : opened) service.close();
    }

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
    public void dependencyWaitSurvivesRestartAndAdvancesBackoff() throws Exception {
        final FileReplicationOutboxService service = service();
        final ReplicationOutboxEntry entry = enqueue(service);
        service.defer(entry.getId(), "Waiting for replica table mapping", Duration.ofMinutes(1));
        service.close();
        final ReplicationOutboxEntry restored = service().findById(entry.getId()).orElseThrow();
        assertEquals(ReplicationOutboxStatus.PENDING, restored.getStatus());
        assertEquals(1, restored.getAttempts());
        assertTrue(restored.getNextAttemptAt().isAfter(Instant.now()));
        assertEquals("Waiting for replica table mapping", restored.getLastError());
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
        final var service = new FileReplicationOutboxService(new ObjectMapper().findAndRegisterModules(),
                tempDir.resolve("outbox.json").toString());
        opened.add(service);
        return service;
    }

    @Test
    void exhaustedTransportFailureRemainsFailedButRecoversAfterRestart() throws Exception {
        final var service = service();
        final var entry = enqueue(service);
        for (int i = 0; i < 25; i++) {
            service.markFailed(entry.getId(), "offline", Duration.ofMinutes(15), 20, true);
        }
        service.close();
        final var restarted = service();
        final var failed = restarted.findById(entry.getId()).orElseThrow();
        assertEquals(ReplicationOutboxStatus.FAILED, failed.getStatus());
        assertEquals(25, failed.getAttempts());
        assertTrue(restarted.findDue(Instant.now(), 25).isEmpty());
        assertEquals(entry.getId(), restarted.findDue(failed.getNextAttemptAt(), 25).getFirst().getId());
        restarted.markSucceeded(entry.getId());
        restarted.markFailed(entry.getId(), "late error", Duration.ofMinutes(15), 20, true);
        assertEquals(ReplicationOutboxStatus.SUCCEEDED, restarted.findById(entry.getId()).orElseThrow().getStatus());
        assertEquals(entry.getPayloadJson(), restarted.findById(entry.getId()).orElseThrow().getPayloadJson());
    }

    @Test
    void dependencyWaitingKeepsFailedAlertAndPermanentFailureStopsAutomaticRetries() {
        final var service = service();
        final var entry = enqueue(service);
        service.markFailed(entry.getId(), "offline", Duration.ofMinutes(15), 1, true);
        service.defer(entry.getId(), "waiting for table mapping", Duration.ofMinutes(15), 20);
        final var waiting = service.findById(entry.getId()).orElseThrow();
        assertEquals(ReplicationOutboxStatus.FAILED, waiting.getStatus());
        assertEquals(2, waiting.getAttempts());
        assertNotNull(waiting.getNextAttemptAt());
        service.markFailed(entry.getId(), "404 not found", Duration.ofMinutes(15), 20, false);
        assertTrue(service.findDue(Instant.now().plus(Duration.ofDays(100)), 25).isEmpty());
        assertEquals(1, service.findAll().size());
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
        service.close();
        assertEquals(original.getId(), service().findAll().getFirst().getId());
    }

    @Test
    public void failedStatusWriteMustRemainPendingAfterRestart() throws Exception {
        final FileReplicationOutboxService service = service();
        final ReplicationOutboxEntry entry = enqueue(service);
        Files.createDirectory(tempDir.resolve("outbox.json.tmp"));

        assertThrows(UncheckedIOException.class, () -> service.markSucceeded(entry.getId()));
        service.close();
        final var restarted = service();
        assertEquals(ReplicationOutboxStatus.PENDING, restarted.findById(entry.getId()).orElseThrow().getStatus());
        Files.delete(tempDir.resolve("outbox.json.tmp"));
        restarted.markSucceeded(entry.getId());
        restarted.close();
        assertEquals(ReplicationOutboxStatus.SUCCEEDED, service().findById(entry.getId()).orElseThrow().getStatus());
    }

    @Test
    void secondWriterCannotOpenStoreUntilFirstStops() throws Exception {
        final var first = service();
        final var entry = enqueue(first);
        final var second = service();
        assertThrows(java.nio.channels.OverlappingFileLockException.class, second::initialize);
        first.close();
        second.initialize();
        assertEquals(entry.getId(), second.findAll().getFirst().getId());
        assertThrows(IllegalStateException.class, first::findAll);
    }

    @Test
    void deletedOutboxIsNotSilentlyRecreated() throws Exception {
        final var service = service();
        enqueue(service);
        Files.delete(tempDir.resolve("outbox.json"));
        assertThrows(UncheckedIOException.class, service::findAll);
        assertThrows(UncheckedIOException.class, () -> enqueue(service));
        assertFalse(Files.exists(tempDir.resolve("outbox.json")));
    }

    @Test
    void operatingSystemLockExcludesAnotherJvmAndSurvivesRestart() throws Exception {
        final var first = service();
        enqueue(first);
        assertEquals(12, probeWriter());
        first.close();
        assertEquals(0, probeWriter());
    }

    private int probeWriter() throws Exception {
        final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                WriterProbe.class.getName(), tempDir.resolve("outbox.json").toString())
                .redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS));
            return process.exitValue();
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    public static class WriterProbe {
        public static void main(String[] args) throws Exception {
            try (var service = new FileReplicationOutboxService(new ObjectMapper(), args[0])) {
                service.initialize();
                if (service.findAll().size() != 1) System.exit(13);
            } catch (IllegalStateException e) {
                System.exit(12);
            }
        }
    }

    private ReplicationOutboxEntry enqueue(FileReplicationOutboxService service) {
        return service.enqueue(ReplicationOutboxOperationType.DATABASE_CREATE, "https://peer.example",
                HttpMethod.POST, Map.of("name", "test"), UUID.randomUUID(), null, null, null, "timeout");
    }

    @Test
    void operatorCancellationIsDurableIdempotentAndCannotBeOverwrittenByLateResults() throws Exception {
        final var service = service();
        final var entry = enqueue(service);
        service.markFailed(entry.getId(), "offline", Duration.ofMinutes(15), 1, true);
        final var cancelled = service.cancel(entry.getId(), "Replica was retired", "operator");
        service.cancel(entry.getId(), "Different reason", "another-operator");
        service.markSucceeded(entry.getId());
        service.markFailed(entry.getId(), "late failure", Duration.ofMinutes(15), 20, true);
        service.defer(entry.getId(), "late dependency wait", Duration.ofMinutes(15));
        service.close();
        final var restarted = service();
        final var restored = restarted.findById(entry.getId()).orElseThrow();
        assertEquals(ReplicationOutboxStatus.CANCELLED, restored.getStatus());
        assertEquals(cancelled.getCancelledAt(), restored.getCancelledAt());
        assertEquals("operator", restored.getCancelledBy());
        assertEquals("Replica was retired", restored.getCancellationReason());
        assertEquals("offline", restored.getLastError());
        assertEquals(1, restored.getAttempts());
        assertEquals(entry.getPayloadJson(), restored.getPayloadJson());
        assertTrue(restarted.findDue(Instant.now().plus(Duration.ofDays(100)), 25).isEmpty());
        assertEquals(1, restarted.findAll().size());
    }

    @Test
    void cancellationRejectsUnknownSuccessfulAndUnauditableRequests() {
        final var service = service();
        final var entry = enqueue(service);
        assertThrows(IllegalArgumentException.class, () -> service.cancel(entry.getId(), " ", "operator"));
        assertThrows(IllegalArgumentException.class, () -> service.cancel(entry.getId(), "x".repeat(2001), "operator"));
        assertThrows(IllegalArgumentException.class, () -> service.cancel(entry.getId(), "Retired", " "));
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.cancel(UUID.randomUUID(), "Retired", "operator"));
        service.markSucceeded(entry.getId());
        assertThrows(IllegalStateException.class, () -> service.cancel(entry.getId(), "Retired", "operator"));
    }

    @Test
    void failedCancellationWriteDoesNotSuppressPendingWork() throws Exception {
        final var service = service();
        final var entry = enqueue(service);
        Files.createDirectory(tempDir.resolve("outbox.json.tmp"));
        assertThrows(UncheckedIOException.class, () -> service.cancel(entry.getId(), "Retired", "operator"));
        service.close();
        assertEquals(ReplicationOutboxStatus.PENDING, service().findById(entry.getId()).orElseThrow().getStatus());
    }
}
