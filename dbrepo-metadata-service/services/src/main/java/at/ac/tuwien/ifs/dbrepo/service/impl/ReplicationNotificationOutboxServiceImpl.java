package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationStatus;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import at.ac.tuwien.ifs.dbrepo.metadata.ReplicationNotificationOutboxRepository;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationNotificationOutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReplicationNotificationOutboxServiceImpl implements ReplicationNotificationOutboxService {

    private final ReplicationNotificationOutboxRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReplicationNotificationOutbox enqueue(ReplicationNotificationType notificationType, HttpMethod httpMethod,
                                                 String path, Object payload, UUID aggregateId) {
        try {
            return repository.save(ReplicationNotificationOutbox.builder()
                    .notificationType(notificationType)
                    .status(ReplicationNotificationStatus.PENDING)
                    .httpMethod(httpMethod.name())
                    .path(path)
                    .aggregateId(aggregateId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .attempts(0)
                    .nextAttemptAt(Instant.now())
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize replication notification", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplicationNotificationOutbox> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplicationNotificationOutbox> findDue(Instant now, int limit) {
        return repository.findDue(ReplicationNotificationStatus.PENDING, now, PageRequest.of(0, limit));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReplicationNotificationOutbox> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public void markSucceeded(UUID id) {
        repository.findById(id).ifPresent(entry -> {
            entry.setStatus(ReplicationNotificationStatus.SUCCEEDED);
            entry.setLastModified(Instant.now());
            entry.setNextAttemptAt(null);
            repository.save(entry);
        });
    }

    @Override
    @Transactional
    public void markFailed(UUID id, String error, Duration retryDelay, int maxAttempts) {
        repository.findById(id).ifPresent(entry -> {
            final int attempts = entry.getAttempts() + 1;
            entry.setAttempts(attempts);
            entry.setLastError(error);
            entry.setLastModified(Instant.now());
            if (attempts >= maxAttempts) {
                entry.setStatus(ReplicationNotificationStatus.FAILED);
                entry.setNextAttemptAt(null);
            } else {
                entry.setStatus(ReplicationNotificationStatus.PENDING);
                entry.setNextAttemptAt(Instant.now().plus(retryDelay));
            }
            repository.save(entry);
        });
    }
}
