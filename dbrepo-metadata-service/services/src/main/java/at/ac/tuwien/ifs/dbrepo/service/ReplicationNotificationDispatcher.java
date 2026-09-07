package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationNotificationDispatcher {

    private final RestTemplate replicationRestTemplate;
    private final ReplicationNotificationOutboxService outboxService;

    @Value("${dbrepo.replication.notificationOutbox.retryDelaySeconds:30}")
    private long retryDelaySeconds;

    @Value("${dbrepo.replication.notificationOutbox.maxRetryDelaySeconds:900}")
    private long maxRetryDelaySeconds;

    @Value("${dbrepo.replication.notificationOutbox.maxAttempts:20}")
    private int maxAttempts;

    @Value("${dbrepo.replication.notificationOutbox.batchSize:25}")
    private int batchSize;

    public ReplicationNotificationDispatcher(@Qualifier("replicationRestTemplate") RestTemplate replicationRestTemplate,
                                             ReplicationNotificationOutboxService outboxService) {
        this.replicationRestTemplate = replicationRestTemplate;
        this.outboxService = outboxService;
    }

    @Async
    public void dispatchAsync(UUID id) {
        dispatch(id);
    }

    public int dispatchDue() {
        int sent = 0;
        for (ReplicationNotificationOutbox entry : outboxService.findDue(Instant.now(), batchSize)) {
            if (dispatch(entry.getId())) {
                sent++;
            }
        }
        return sent;
    }

    public boolean dispatch(UUID id) {
        final ReplicationNotificationOutbox entry = outboxService.findById(id).orElse(null);
        if (entry == null) {
            return false;
        }
        if (ReplicationNotificationStatus.SUCCEEDED.equals(entry.getStatus())) {
            return true;
        }
        try {
            send(entry);
            outboxService.markSucceeded(id);
            log.info("Sent replication notification {} for {}", entry.getNotificationType(), entry.getAggregateId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send replication notification {} for {}: {}", entry.getNotificationType(),
                    entry.getAggregateId(), e.getMessage(), e);
            outboxService.markFailed(id, e.getMessage(), retryDelayFor(entry.getAttempts() + 1),
                    Math.max(1, maxAttempts));
            return false;
        }
    }

    private void send(ReplicationNotificationOutbox entry) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final ResponseEntity<Void> response = replicationRestTemplate.exchange(entry.getPath(),
                HttpMethod.valueOf(entry.getHttpMethod()), new HttpEntity<>(entry.getPayload(), headers), Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Replication notification returned " + response.getStatusCode());
        }
    }

    private Duration retryDelayFor(int attempt) {
        final long baseSeconds = Math.max(1, retryDelaySeconds);
        final long cappedMaxSeconds = Math.max(baseSeconds, maxRetryDelaySeconds);
        final int exponent = Math.min(Math.max(0, attempt - 1), 10);
        final long multiplier = 1L << exponent;
        return Duration.ofSeconds(Math.min(cappedMaxSeconds, baseSeconds * multiplier));
    }
}
