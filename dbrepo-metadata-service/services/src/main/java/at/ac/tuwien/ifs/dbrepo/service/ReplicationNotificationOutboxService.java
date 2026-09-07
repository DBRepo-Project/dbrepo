package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReplicationNotificationOutboxService {

    ReplicationNotificationOutbox enqueue(ReplicationNotificationType notificationType, HttpMethod httpMethod,
                                          String path, Object payload, UUID aggregateId);

    List<ReplicationNotificationOutbox> findAll();

    List<ReplicationNotificationOutbox> findDue(Instant now, int limit);

    Optional<ReplicationNotificationOutbox> findById(UUID id);

    void markSucceeded(UUID id);

    void markFailed(UUID id, String error, Duration retryDelay, int maxAttempts);
}
