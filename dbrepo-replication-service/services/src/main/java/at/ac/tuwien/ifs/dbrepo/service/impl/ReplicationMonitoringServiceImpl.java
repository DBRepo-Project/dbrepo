package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationHealthDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationServiceHealthDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationMonitoringService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationOutboxSummaryDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationStatusDto;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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
        final ReplicationOutboxSummaryDto outbox = summarizeOutbox();
        final ReplicationHealthDto health = ReplicationHealthDto.builder()
                .status(overallStatus(metadataService, dataService, outbox))
                .metadataService(metadataService)
                .dataService(dataService)
                .replicationService(replicationService)
                .build();
        return new ReplicationStatusDto(health, outbox);
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

    private ReplicationOutboxSummaryDto summarizeOutbox() {
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

    private long count(List<ReplicationOutboxEntry> entries, ReplicationOutboxStatus status) {
        return entries.stream()
                .filter(entry -> status.equals(entry.getStatus()))
                .count();
    }

    private boolean isActionable(ReplicationOutboxEntry entry) {
        return ReplicationOutboxStatus.PENDING.equals(entry.getStatus())
                || ReplicationOutboxStatus.FAILED.equals(entry.getStatus());
    }

    private String overallStatus(ReplicationServiceHealthDto metadataService,
                                 ReplicationServiceHealthDto dataService,
                                 ReplicationOutboxSummaryDto outbox) {
        if (!STATUS_UP.equalsIgnoreCase(metadataService.getStatus())
                || !STATUS_UP.equalsIgnoreCase(dataService.getStatus())) {
            return STATUS_DOWN;
        }
        if (outbox.pending() > 0 || outbox.failed() > 0) {
            return STATUS_DEGRADED;
        }
        return STATUS_UP;
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
