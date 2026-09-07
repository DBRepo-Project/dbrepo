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
        final ReplicationServiceHealthDto replicationService = ReplicationServiceHealthDto.builder()
                .name("replication-service")
                .status(STATUS_UP)
                .durationMs(0L)
                .build();
        final ReplicationOutboxSummaryDto outbox = summarizeLocalOutbox();
        final ReplicationOutboxStatusDto outboxes = new ReplicationOutboxStatusDto(outbox,
                summarizeMetadataOutbox(), summarizeDataOutboxes());
        final ReplicationHealthDto health = ReplicationHealthDto.builder()
                .status(overallStatus(metadataService, dataService, outboxes))
                .metadataService(metadataService)
                .dataService(dataService)
                .replicationService(replicationService)
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
        } catch (Exception e) {
            return ReplicationServiceHealthDto.builder()
                    .name(name)
                    .status(STATUS_DOWN)
                    .httpStatus(httpStatus(e))
                    .durationMs(elapsedMillis(started))
                    .error(e.getMessage())
                    .build();
        }
    }

    private String healthStatus(ResponseEntity<Map> response) {
        if (response.getBody() != null) {
            final Object status = response.getBody().get("status");
            if (status instanceof String value && !value.isBlank()) {
                return value.toUpperCase(Locale.ROOT);
            }
        }
        return response.getStatusCode().is2xxSuccessful() ? STATUS_UP : STATUS_DOWN;
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
                                 ReplicationOutboxStatusDto outboxes) {
        if (!STATUS_UP.equalsIgnoreCase(metadataService.getStatus())
                || !STATUS_UP.equalsIgnoreCase(dataService.getStatus())) {
            return STATUS_DOWN;
        }
        if (hasBacklog(outboxes.replicationService())
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
