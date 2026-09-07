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

    void markSucceeded(UUID id);

    void markFailed(UUID id, String error, Duration retryDelay, int maxAttempts);
}
