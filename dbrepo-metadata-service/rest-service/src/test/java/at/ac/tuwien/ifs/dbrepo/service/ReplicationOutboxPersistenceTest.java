package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.metadata.ReplicationNotificationOutboxRepository;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationStatus;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReplicationOutboxPersistenceTest {
    @Autowired private ReplicationNotificationOutboxService service;
    @Autowired private ReplicationNotificationOutboxRepository repository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private ReplicationNotificationDispatcher dispatcher;

    @Test
    void terminalStatusesPersistWithoutNextAttemptAndKeepEvidence() {
        for (boolean succeeded : new boolean[]{true, false}) {
            final var entry = service.enqueue(ReplicationNotificationType.TABLE_CREATE, HttpMethod.POST,
                    "/api/replication/table", Map.of("id", "preserved"), UUID.randomUUID());
            repository.flush();
            if (succeeded) service.markSucceeded(entry.getId());
            else service.markFailed(entry.getId(), "offline", Duration.ofSeconds(1), 1);
            repository.flush();
            entityManager.clear();
            final var saved = repository.findById(entry.getId()).orElseThrow();
            assertEquals(succeeded ? ReplicationNotificationStatus.SUCCEEDED : ReplicationNotificationStatus.FAILED,
                    saved.getStatus());
            assertNull(saved.getNextAttemptAt());
            assertEquals("{\"id\":\"preserved\"}", saved.getPayload());
        }
    }

    @Test
    void exhaustedRecoverableFailureStaysVisibleAndDueAcrossReload() {
        final var entry = service.enqueue(ReplicationNotificationType.TABLE_CREATE, HttpMethod.POST,
                "/api/replication/table", Map.of("id", "preserved"), UUID.randomUUID());
        for (int i = 0; i < 25; i++) {
            service.markFailed(entry.getId(), "connection refused", Duration.ofMinutes(15), 20, true);
        }
        repository.flush();
        entityManager.clear();
        final var saved = repository.findById(entry.getId()).orElseThrow();
        assertEquals(ReplicationNotificationStatus.FAILED, saved.getStatus());
        assertEquals(25, saved.getAttempts());
        assertFalse(service.findDue(Instant.now(), 100).stream().anyMatch(e -> e.getId().equals(entry.getId())));
        assertTrue(service.findDue(saved.getNextAttemptAt(), 100).stream().anyMatch(e -> e.getId().equals(entry.getId())));
        service.markSucceeded(entry.getId());
        service.markFailed(entry.getId(), "late failure", Duration.ofMinutes(15), 20, true);
        repository.flush();
        entityManager.clear();
        assertEquals(ReplicationNotificationStatus.SUCCEEDED, repository.findById(entry.getId()).orElseThrow().getStatus());
    }
}
