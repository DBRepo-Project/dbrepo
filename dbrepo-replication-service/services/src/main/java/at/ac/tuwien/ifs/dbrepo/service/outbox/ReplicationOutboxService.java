package at.ac.tuwien.ifs.dbrepo.service.outbox;

import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReplicationOutboxService {

    ReplicationOutboxEntry enqueue(ReplicationOutboxOperationType operationType, String targetSiteUrl,
                                   HttpMethod httpMethod, Object payload, UUID localDatabaseId, UUID localTableId,
                                   UUID remoteDatabaseId, UUID remoteTableId, String lastError);

    List<ReplicationOutboxEntry> findAll();

    List<ReplicationOutboxEntry> findDue(Instant now, int limit);

    Optional<ReplicationOutboxEntry> findById(UUID id);

    ReplicationOutboxEntry cancel(UUID id, String reason, String actor);

    void markSucceeded(UUID id);

    void defer(UUID id, String reason, Duration retryDelay);

    default void defer(UUID id, String reason, Duration retryDelay, int maxAttempts) {
        markFailed(id, reason, retryDelay, maxAttempts, true);
    }

    void markFailed(UUID id, String error, Duration retryDelay, int maxAttempts);

    void markFailed(UUID id, String error, Duration retryDelay, int maxAttempts, boolean recoverable);
}
