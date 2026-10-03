package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationServiceHealthDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationStatusDto;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxStatus;
import org.springframework.core.ParameterizedTypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
        mockRemoteOutboxes(metadataRestTemplate, List.of(), List.of());

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
        assertEquals(3, status.outboxes().replicationService().total());
        assertEquals(0, status.outboxes().metadataService().total());
        assertEquals(0, status.outboxes().dataService().total());
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
        mockBrokerUp(metadataRestTemplate);
        when(dataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(outboxService.findAll())
                .thenReturn(List.of());

        final ReplicationStatusDto status = service.getStatus();

        assertEquals("DOWN", status.health().getStatus());
        assertEquals("UNAVAILABLE", status.health().getMetadataService().getStatus());
        assertEquals("UP", status.health().getDataService().getStatus());
        assertNotNull(status.health().getMetadataService().getError());
        assertEquals(0, status.outbox().total());
    }

    @Test
    public void getStatus_withNoBacklog_isUp() {
        final RestTemplate metadataRestTemplate = mock(RestTemplate.class);
        final RestTemplate dataRestTemplate = mock(RestTemplate.class);
        final ReplicationOutboxService outboxService = mock(ReplicationOutboxService.class);
        final ReplicationMonitoringServiceImpl service = new ReplicationMonitoringServiceImpl(metadataRestTemplate,
                dataRestTemplate, outboxService);

        when(metadataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(dataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(outboxService.findAll())
                .thenReturn(List.of());
        mockRemoteOutboxes(metadataRestTemplate, List.of(), List.of());

        final ReplicationStatusDto status = service.getStatus();

        assertEquals("UP", status.health().getStatus());
        assertEquals("UP", status.health().getBroker().getStatus());
        assertEquals(0, status.outboxes().replicationService().total());
        assertEquals(0, status.outboxes().metadataService().total());
        assertEquals(0, status.outboxes().dataService().total());
    }

    @Test
    public void getStatus_withUpstreamOutboxBacklog_isDegraded() {
        final RestTemplate metadataRestTemplate = mock(RestTemplate.class);
        final RestTemplate dataRestTemplate = mock(RestTemplate.class);
        final ReplicationOutboxService outboxService = mock(ReplicationOutboxService.class);
        final ReplicationMonitoringServiceImpl service = new ReplicationMonitoringServiceImpl(metadataRestTemplate,
                dataRestTemplate, outboxService);
        final UUID databaseId = UUID.randomUUID();
        final Instant oldestPendingAt = Instant.parse("2026-01-01T00:00:00Z");
        final Instant nextAttemptAt = Instant.parse("2026-01-01T00:01:00Z");

        when(metadataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(dataRestTemplate.exchange(eq("/actuator/health"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(outboxService.findAll())
                .thenReturn(List.of());
        mockRemoteOutboxes(metadataRestTemplate,
                List.of(Map.of(
                        "status", "PENDING",
                        "created", oldestPendingAt.toString(),
                        "nextAttemptAt", nextAttemptAt.toString())),
                List.of(Map.of(
                        "id", databaseId.toString())));
        when(dataRestTemplate.exchange(eq("/api/v1/database/" + databaseId + "/replication/outbox"),
                eq(HttpMethod.GET), eq(HttpEntity.EMPTY), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(Map.of(
                        "status", "FAILED",
                        "created", Instant.parse("2026-01-01T00:02:00Z").toString()))));

        final ReplicationStatusDto status = service.getStatus();

        assertEquals("DEGRADED", status.health().getStatus());
        assertEquals(1, status.outboxes().metadataService().pending());
        assertEquals(oldestPendingAt, status.outboxes().metadataService().oldestPendingAt());
        assertEquals(nextAttemptAt, status.outboxes().metadataService().nextAttemptAt());
        assertEquals(1, status.outboxes().dataService().failed());
    }

    private void mockRemoteOutboxes(RestTemplate metadataRestTemplate, List<Map<String, Object>> metadataOutbox,
                                    List<Map<String, Object>> databases) {
        mockBrokerUp(metadataRestTemplate);
        when(metadataRestTemplate.exchange(eq("/api/metadata/replication/outbox"), eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(metadataOutbox));
        when(metadataRestTemplate.exchange(eq("/api/v1/database"), eq(HttpMethod.GET), eq(HttpEntity.EMPTY),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(databases));
    }

    private void mockBrokerUp(RestTemplate metadataRestTemplate) {
        when(metadataRestTemplate.exchange("/api/metadata/broker/health", HttpMethod.GET, HttpEntity.EMPTY,
                ReplicationServiceHealthDto.class))
                .thenReturn(ResponseEntity.ok(ReplicationServiceHealthDto.builder()
                        .name("broker").status("UP").httpStatus(200).durationMs(1L).build()));
    }

    @ParameterizedTest
    @CsvSource({"UP, 200, UP", "DOWN, 503, DOWN", "UNAVAILABLE, 401, DOWN", "UNKNOWN, 404, DEGRADED"})
    void getStatus_preservesBrokerResult(String brokerStatus, int brokerHttpStatus, String overallStatus) {
        final ReplicationStatusDto result = probeResponses(withSuccess("{\"name\":\"broker\",\"status\":\""
                + brokerStatus + "\",\"http_status\":" + brokerHttpStatus + ",\"duration_ms\":7}",
                MediaType.APPLICATION_JSON), withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        assertEquals(overallStatus, result.health().getStatus());
        assertEquals(brokerStatus, result.health().getBroker().getStatus());
        assertEquals(brokerHttpStatus, result.health().getBroker().getHttpStatus());
        assertEquals(7L, result.health().getBroker().getDurationMs());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "{}", "{\"status\":\"unexpected\"}", "not-json"})
    void getStatus_missingOrInvalidBrokerStatusIsUnknown(String body) {
        final ReplicationStatusDto result = probeResponses(withSuccess(body, MediaType.APPLICATION_JSON),
                withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        assertEquals("UNKNOWN", result.health().getBroker().getStatus());
        assertEquals("DEGRADED", result.health().getStatus());
        assertNotNull(result.health().getBroker().getError());
    }

    @ParameterizedTest
    @CsvSource({"401, UNAVAILABLE, DOWN", "403, UNAVAILABLE, DOWN", "404, UNKNOWN, DEGRADED", "503, UNAVAILABLE, DOWN"})
    void getStatus_failedMetadataBrokerProbeCannotReportUp(int httpStatus, String brokerStatus, String overallStatus) {
        final ReplicationStatusDto result = probeResponses(withStatus(HttpStatus.valueOf(httpStatus)),
                withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        assertEquals(brokerStatus, result.health().getBroker().getStatus());
        assertEquals(httpStatus, result.health().getBroker().getHttpStatus());
        assertEquals(overallStatus, result.health().getStatus());
    }

    @Test
    void getStatus_brokerProbeTimeoutIsUnavailable() {
        final ReplicationStatusDto result = probeResponses(withException(new SocketTimeoutException("timed out")),
                withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        assertEquals("UNAVAILABLE", result.health().getBroker().getStatus());
        assertNull(result.health().getBroker().getHttpStatus());
        assertEquals("DOWN", result.health().getStatus());
    }

    @ParameterizedTest
    @CsvSource({"200, '{}', UNKNOWN, DEGRADED", "503, '{\"status\":\"DOWN\"}', DOWN, DOWN",
            "503, '{\"status\":\"OUT_OF_SERVICE\"}', DOWN, DOWN", "503, '{}', UNAVAILABLE, DOWN"})
    void getStatus_actuatorProbeDoesNotInventHealth(int httpStatus, String body, String serviceStatus, String overall) {
        final ReplicationStatusDto result = probeResponses(
                withSuccess("{\"name\":\"broker\",\"status\":\"UP\"}", MediaType.APPLICATION_JSON),
                withStatus(HttpStatus.valueOf(httpStatus)).body(body).contentType(MediaType.APPLICATION_JSON));

        assertEquals(serviceStatus, result.health().getMetadataService().getStatus());
        assertEquals(overall, result.health().getStatus());
    }

    private ReplicationStatusDto probeResponses(ResponseCreator brokerResponse, ResponseCreator metadataResponse) {
        final RestTemplate metadata = new RestTemplate();
        final RestTemplate data = mock(RestTemplate.class);
        final ReplicationOutboxService outbox = mock(ReplicationOutboxService.class);
        final MockRestServiceServer server = MockRestServiceServer.bindTo(metadata).build();
        server.expect(requestTo("/actuator/health")).andRespond(metadataResponse);
        server.expect(requestTo("/api/metadata/broker/health")).andRespond(brokerResponse);
        server.expect(requestTo("/api/metadata/replication/outbox"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("/api/v1/database")).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        when(data.exchange("/actuator/health", HttpMethod.GET, HttpEntity.EMPTY, Map.class))
                .thenReturn(ResponseEntity.ok(Map.of("status", "UP")));
        when(outbox.findAll()).thenReturn(List.of());

        final ReplicationStatusDto result = new ReplicationMonitoringServiceImpl(metadata, data, outbox).getStatus();
        server.verify();
        return result;
    }
}
