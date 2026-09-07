package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxService;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TupleReplicationNotificationDispatcher {

    private final RestTemplate replicationRestTemplate;
    private final MetadataService metadataService;
    private final TupleReplicationOutboxService outboxService;

    @Value("${dbrepo.replication.tupleOutbox.retryDelaySeconds:30}")
    private long retryDelaySeconds;

    @Value("${dbrepo.replication.tupleOutbox.maxRetryDelaySeconds:900}")
    private long maxRetryDelaySeconds;

    @Value("${dbrepo.replication.tupleOutbox.maxAttempts:20}")
    private int maxAttempts;

    @Value("${dbrepo.replication.tupleOutbox.batchSize:25}")
    private int batchSize;

    @Value("${dbrepo.replication.tupleOutbox.processingTimeoutSeconds:300}")
    private long processingTimeoutSeconds;

    public TupleReplicationNotificationDispatcher(@Qualifier("replicationRestTemplate") RestTemplate replicationRestTemplate,
                                                  MetadataService metadataService,
                                                  TupleReplicationOutboxService outboxService) {
        this.replicationRestTemplate = replicationRestTemplate;
        this.metadataService = metadataService;
        this.outboxService = outboxService;
    }

    @Async
    public void dispatchAsync(Database database, UUID id) {
        dispatch(database, id);
    }

    public int dispatchDue() {
        int sent = 0;
        try {
            final List<Database> databases = metadataService.getDatabases();
            for (Database database : databases) {
                if (database.getReplicaUrls() == null || database.getReplicaUrls().isEmpty()) {
                    continue;
                }
                sent += dispatchDue(database);
            }
        } catch (Exception e) {
            log.error("Failed to dispatch pending tuple replication notifications: {}", e.getMessage(), e);
        }
        return sent;
    }

    public int dispatchDue(Database database) {
        int sent = 0;
        try {
            for (TupleReplicationOutboxEntry entry : outboxService.claimDue(database, batchSize,
                    processingTimeout())) {
                if (sendAndMark(database, entry)) {
                    sent++;
                }
            }
        } catch (Exception e) {
            log.error("Failed to dispatch pending tuple replication notifications for database {}: {}",
                    database.getInternalName(), e.getMessage(), e);
        }
        return sent;
    }

    public boolean dispatch(Database database, UUID id) {
        try {
            return outboxService.claim(database, id, processingTimeout())
                    .map(entry -> sendAndMark(database, entry))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Failed to claim tuple replication notification {} in database {}: {}", id,
                    database.getInternalName(), e.getMessage(), e);
            return false;
        }
    }

    private boolean sendAndMark(Database database, TupleReplicationOutboxEntry entry) {
        try {
            send(entry);
            outboxService.markSucceeded(database, entry.getId());
            log.info("Sent {} tuple replication notification for key in database {}", entry.getHttpMethod(),
                    database.getInternalName());
            return true;
        } catch (Exception e) {
            log.error("Failed to send {} tuple replication notification in database {}: {}", entry.getHttpMethod(),
                    database.getInternalName(), e.getMessage(), e);
            try {
                outboxService.markFailed(database, entry.getId(), e.getMessage(),
                        retryDelayFor(entry.getAttempts() + 1), Math.max(1, maxAttempts));
            } catch (Exception markFailedException) {
                log.error("Failed to update tuple replication outbox entry {} in database {}: {}", entry.getId(),
                        database.getInternalName(), markFailedException.getMessage(), markFailedException);
            }
            return false;
        }
    }

    private void send(TupleReplicationOutboxEntry entry) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final ResponseEntity<Void> response = replicationRestTemplate.exchange("/api/replication/data",
                entry.getHttpMethod(), new HttpEntity<>(entry.getPayloadJson(), headers), Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Tuple replication notification returned " + response.getStatusCode());
        }
    }

    private Duration retryDelayFor(int attempt) {
        final long baseSeconds = Math.max(1, retryDelaySeconds);
        final long cappedMaxSeconds = Math.max(baseSeconds, maxRetryDelaySeconds);
        final int exponent = Math.min(Math.max(0, attempt - 1), 10);
        final long multiplier = 1L << exponent;
        return Duration.ofSeconds(Math.min(cappedMaxSeconds, baseSeconds * multiplier));
    }

    private Duration processingTimeout() {
        return Duration.ofSeconds(Math.max(1, processingTimeoutSeconds));
    }
}
