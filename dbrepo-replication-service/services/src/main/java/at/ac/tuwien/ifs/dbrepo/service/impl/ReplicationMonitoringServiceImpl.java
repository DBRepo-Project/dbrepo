package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationHealthDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationServiceHealthDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationMonitoringService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationOutboxStatusDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationOutboxSummaryDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationStatusDto;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReplicationMonitoringServiceImpl implements ReplicationMonitoringService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";

    private final RestTemplate metadataServiceRestTemplate;
    private final RestTemplate dataServiceRestTemplate;
    private final ReplicationOutboxService outboxService;

    public ReplicationMonitoringServiceImpl(@Qualifier("metadataServiceRestTemplate")
                                            RestTemplate metadataServiceRestTemplate,
                                            @Qualifier("dataServiceRestTemplate") RestTemplate dataServiceRestTemplate,
                                            ReplicationOutboxService outboxService) {
        this.metadataServiceRestTemplate = metadataServiceRestTemplate;
        this.dataServiceRestTemplate = dataServiceRestTemplate;
        this.outboxService = outboxService;
    }

    @Override
    public ReplicationStatusDto getStatus() {
        final ReplicationServiceHealthDto metadataService = probe("metadata-service", metadataServiceRestTemplate);
        final ReplicationServiceHealthDto dataService = probe("data-service", dataServiceRestTemplate);
        final ReplicationServiceHealthDto broker = probeBroker();
        final ReplicationServiceHealthDto replicationService = ReplicationServiceHealthDto.builder()
                .name("replication-service")
                .status(STATUS_UP)
                .durationMs(0L)
                .build();
        final ReplicationOutboxSummaryDto outbox = summarizeLocalOutbox();
        final ReplicationOutboxStatusDto outboxes = new ReplicationOutboxStatusDto(outbox,
                summarizeMetadataOutbox(), summarizeDataOutboxes());
        final ReplicationHealthDto health = ReplicationHealthDto.builder()
                .status(overallStatus(metadataService, dataService, broker, outboxes))
                .metadataService(metadataService)
                .dataService(dataService)
                .replicationService(replicationService)
                .broker(broker)
                .build();
        return new ReplicationStatusDto(health, outbox, outboxes);
    }

    private ReplicationServiceHealthDto probe(String name, RestTemplate restTemplate) {
        final long started = System.nanoTime();
        try {
            final ResponseEntity<Map> response = restTemplate.exchange("/actuator/health", HttpMethod.GET,
                    HttpEntity.EMPTY, Map.class);
            return ReplicationServiceHealthDto.builder()
                    .name(name)
                    .status(healthStatus(response))
                    .httpStatus(response.getStatusCode().value())
                    .durationMs(elapsedMillis(started))
                    .build();
        } catch (RestClientException e) {
            String status = e instanceof ResourceAccessException ? STATUS_UNAVAILABLE : STATUS_UNKNOWN;
            if (e instanceof HttpStatusCodeException httpError) {
                status = STATUS_UNAVAILABLE;
                try {
                    final Map body = httpError.getResponseBodyAs(Map.class);
                    if (body != null && (STATUS_DOWN.equals(body.get("status"))
                            || "OUT_OF_SERVICE".equals(body.get("status")))) {
                        status = STATUS_DOWN;
                    }
                } catch (RestClientException | IllegalStateException ignored) {
                    // Failed HTTP access alone does not establish the service's own health state.
                }
            }
            return ReplicationServiceHealthDto.builder()
                    .name(name)
                    .status(status)
                    .httpStatus(httpStatus(e))
                    .durationMs(elapsedMillis(started))
                    .error("Service health check could not be completed")
                    .build();
        }
    }

    private String healthStatus(ResponseEntity<Map> response) {
        if (response.getBody() != null) {
            final Object status = response.getBody().get("status");
            if (status instanceof String value && !value.isBlank()) {
                return switch (value.toUpperCase(Locale.ROOT)) {
                    case STATUS_UP -> response.getStatusCode().is2xxSuccessful() ? STATUS_UP : STATUS_UNAVAILABLE;
                    case STATUS_DOWN, "OUT_OF_SERVICE" -> STATUS_DOWN;
                    default -> STATUS_UNKNOWN;
                };
            }
        }
        return STATUS_UNKNOWN;
    }

    private ReplicationServiceHealthDto probeBroker() {
        final long started = System.nanoTime();
        final ReplicationServiceHealthDto result = ReplicationServiceHealthDto.builder()
                .name("broker")
                .status(STATUS_UNKNOWN)
                .error("Metadata returned an unrecognized broker health response")
                .build();
        try {
            final ResponseEntity<ReplicationServiceHealthDto> response = metadataServiceRestTemplate.exchange(
                    "/api/metadata/broker/health", HttpMethod.GET, HttpEntity.EMPTY, ReplicationServiceHealthDto.class);
            final ReplicationServiceHealthDto body = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && body != null
                    && body.getStatus() != null
                    && List.of(STATUS_UP, STATUS_DOWN, STATUS_UNKNOWN, STATUS_UNAVAILABLE).contains(body.getStatus())) {
                return body;
            }
            result.setHttpStatus(response.getStatusCode().value());
        } catch (HttpStatusCodeException e) {
            result.setStatus(e.getStatusCode().value() == 404 ? STATUS_UNKNOWN : STATUS_UNAVAILABLE);
            result.setHttpStatus(e.getStatusCode().value());
            result.setError("Metadata broker health endpoint unavailable (HTTP " + e.getStatusCode().value() + ")");
        } catch (ResourceAccessException e) {
            result.setStatus(STATUS_UNAVAILABLE);
            result.setError("Metadata broker health endpoint is unreachable or timed out");
        } catch (RestClientException e) {
            result.setError("Metadata returned an unreadable broker health response");
        }
        result.setDurationMs(elapsedMillis(started));
        return result;
    }

    private ReplicationOutboxSummaryDto summarizeLocalOutbox() {
        final List<ReplicationOutboxEntry> entries = outboxService.findAll();
        final long pending = count(entries, ReplicationOutboxStatus.PENDING);
        final long failed = count(entries, ReplicationOutboxStatus.FAILED);
        final long succeeded = count(entries, ReplicationOutboxStatus.SUCCEEDED);
        final Instant oldestPendingAt = entries.stream()
                .filter(this::isActionable)
                .map(ReplicationOutboxEntry::getCreatedAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);
        final Instant nextAttemptAt = entries.stream()
                .filter(this::isActionable)
                .map(ReplicationOutboxEntry::getNextAttemptAt)
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);
        return new ReplicationOutboxSummaryDto(entries.size(), pending, failed, succeeded, oldestPendingAt,
                nextAttemptAt);
    }

    private ReplicationOutboxSummaryDto summarizeMetadataOutbox() {
        try {
            final List<Map<String, Object>> entries = fetchRemoteList(metadataServiceRestTemplate,
                    "/api/metadata/replication/outbox");
            return summarizeRemoteOutbox(entries);
        } catch (Exception e) {
            return ReplicationOutboxSummaryDto.unavailable(errorMessage(e));
        }
    }

    private ReplicationOutboxSummaryDto summarizeDataOutboxes() {
        try {
            final List<Map<String, Object>> entries = new ArrayList<>();
            for (UUID databaseId : databaseIds()) {
                entries.addAll(fetchRemoteList(dataServiceRestTemplate,
                        "/api/v1/database/" + databaseId + "/replication/outbox"));
            }
            return summarizeRemoteOutbox(entries);
        } catch (Exception e) {
            return ReplicationOutboxSummaryDto.unavailable(errorMessage(e));
        }
    }

    private List<UUID> databaseIds() {
        return fetchRemoteList(metadataServiceRestTemplate, "/api/v1/database")
                .stream()
                .map(entry -> entry.get("id"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(id -> !id.isBlank())
                .map(UUID::fromString)
                .toList();
    }

    private List<Map<String, Object>> fetchRemoteList(RestTemplate restTemplate, String path) {
        final ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(path, HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<>() {
                });
        return response.getBody() != null ? response.getBody() : List.of();
    }

    private ReplicationOutboxSummaryDto summarizeRemoteOutbox(List<Map<String, Object>> entries) {
        final long pending = count(entries, "PENDING");
        final long failed = count(entries, "FAILED");
        final long succeeded = count(entries, "SUCCEEDED");
        final Instant oldestPendingAt = entries.stream()
                .filter(this::isActionable)
                .map(entry -> instantValue(entry, "created", "created_at"))
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);
        final Instant nextAttemptAt = entries.stream()
                .filter(this::isActionable)
                .map(entry -> instantValue(entry, "nextAttemptAt", "next_attempt_at"))
                .filter(Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);
        return new ReplicationOutboxSummaryDto(entries.size(), pending, failed, succeeded, oldestPendingAt,
                nextAttemptAt);
    }

    private long count(List<ReplicationOutboxEntry> entries, ReplicationOutboxStatus status) {
        return entries.stream()
                .filter(entry -> status.equals(entry.getStatus()))
                .count();
    }

    private long count(List<Map<String, Object>> entries, String status) {
        return entries.stream()
                .filter(entry -> status.equalsIgnoreCase(statusValue(entry)))
                .count();
    }

    private boolean isActionable(ReplicationOutboxEntry entry) {
        return ReplicationOutboxStatus.PENDING.equals(entry.getStatus())
                || ReplicationOutboxStatus.FAILED.equals(entry.getStatus());
    }

    private boolean isActionable(Map<String, Object> entry) {
        final String status = statusValue(entry);
        return "PENDING".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
    }

    private String overallStatus(ReplicationServiceHealthDto metadataService,
                                 ReplicationServiceHealthDto dataService,
                                 ReplicationServiceHealthDto broker,
                                 ReplicationOutboxStatusDto outboxes) {
        final List<String> statuses = List.of(metadataService.getStatus(), dataService.getStatus(), broker.getStatus());
        if (statuses.stream().anyMatch(status -> STATUS_DOWN.equals(status) || STATUS_UNAVAILABLE.equals(status))) {
            return STATUS_DOWN;
        }
        if (statuses.stream().anyMatch(status -> !STATUS_UP.equals(status))
                || hasBacklog(outboxes.replicationService())
                || hasBacklog(outboxes.metadataService())
                || hasBacklog(outboxes.dataService())
                || !outboxes.metadataService().available()
                || !outboxes.dataService().available()) {
            return STATUS_DEGRADED;
        }
        return STATUS_UP;
    }

    private boolean hasBacklog(ReplicationOutboxSummaryDto outbox) {
        return outbox.pending() > 0 || outbox.failed() > 0;
    }

    private String statusValue(Map<String, Object> entry) {
        final Object status = entry.get("status");
        return status != null ? status.toString() : "";
    }

    private Instant instantValue(Map<String, Object> entry, String... fields) {
        for (String field : fields) {
            final Object value = entry.get(field);
            if (value instanceof Instant instant) {
                return instant;
            }
            if (value instanceof String string && !string.isBlank()) {
                try {
                    return Instant.parse(string);
                } catch (DateTimeParseException ignored) {
                    continue;
                }
            }
        }
        return null;
    }

    private String errorMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private Integer httpStatus(Exception e) {
        if (e instanceof HttpStatusCodeException statusException) {
            return statusException.getStatusCode().value();
        }
        return null;
    }

    private long elapsedMillis(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }
}
