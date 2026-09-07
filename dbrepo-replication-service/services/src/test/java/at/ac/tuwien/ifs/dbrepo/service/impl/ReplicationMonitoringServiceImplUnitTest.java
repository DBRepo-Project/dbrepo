package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.service.ReplicationStatusDto;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReplicationMonitoringServiceImplUnitTest {

    @Test
    public void getStatus_withPendingOutbox_isDegraded() {
        final RestTemplate metadataRestTemplate = mock(RestTemplate.class);
        final RestTemplate dataRestTemplate = mock(RestTemplate.class);
        final ReplicationOutboxService outboxService = mock(ReplicationOutboxService.class);
        final ReplicationMonitoringServiceImpl service = new ReplicationMonitoringServiceImpl(metadataRestTemplate,
                dataRestTemplate, outboxService);
        final Instant oldestPendingAt = Instant.parse("2026-01-01T00:00:00Z");
        final Instant nextAttemptAt = Instant.parse("2026-01-01T00:01:00Z");

        when(metadataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(dataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(outboxService.findAll())
                .thenReturn(List.of(
                        ReplicationOutboxEntry.builder()
                                .id(UUID.randomUUID())
                                .status(ReplicationOutboxStatus.PENDING)
                                .createdAt(oldestPendingAt)
                                .nextAttemptAt(nextAttemptAt)
                                .build(),
                        ReplicationOutboxEntry.builder()
                                .id(UUID.randomUUID())
                                .status(ReplicationOutboxStatus.FAILED)
                                .createdAt(Instant.parse("2026-01-01T00:02:00Z"))
                                .build(),
                        ReplicationOutboxEntry.builder()
                                .id(UUID.randomUUID())
                                .status(ReplicationOutboxStatus.SUCCEEDED)
                                .createdAt(Instant.parse("2026-01-01T00:03:00Z"))
                                .build()));

        final ReplicationStatusDto status = service.getStatus();

        assertEquals("DEGRADED", status.health().getStatus());
        assertEquals("UP", status.health().getMetadataService().getStatus());
        assertEquals("UP", status.health().getDataService().getStatus());
        assertEquals(3, status.outbox().total());
        assertEquals(1, status.outbox().pending());
        assertEquals(1, status.outbox().failed());
        assertEquals(1, status.outbox().succeeded());
        assertEquals(oldestPendingAt, status.outbox().oldestPendingAt());
        assertEquals(nextAttemptAt, status.outbox().nextAttemptAt());
    }

    @Test
    public void getStatus_withDownDependency_isDown() {
        final RestTemplate metadataRestTemplate = mock(RestTemplate.class);
        final RestTemplate dataRestTemplate = mock(RestTemplate.class);
        final ReplicationOutboxService outboxService = mock(ReplicationOutboxService.class);
        final ReplicationMonitoringServiceImpl service = new ReplicationMonitoringServiceImpl(metadataRestTemplate,
                dataRestTemplate, outboxService);

        when(metadataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenThrow(new ResourceAccessException("connection refused"));
        when(dataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(outboxService.findAll())
                .thenReturn(List.of());

        final ReplicationStatusDto status = service.getStatus();

        assertEquals("DOWN", status.health().getStatus());
        assertEquals("DOWN", status.health().getMetadataService().getStatus());
        assertEquals("UP", status.health().getDataService().getStatus());
        assertNotNull(status.health().getMetadataService().getError());
        assertEquals(0, status.outbox().total());
    }
}
