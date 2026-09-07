package at.ac.tuwien.ifs.dbrepo.service.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class FileReplicationOutboxService implements ReplicationOutboxService {

    private static final TypeReference<List<ReplicationOutboxEntry>> ENTRY_LIST =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final Path outboxPath;

    public FileReplicationOutboxService(ObjectMapper objectMapper,
                                        @Value("${dbrepo.replication.outbox.path:/var/lib/dbrepo/replication/outbox.json}")
                                        String outboxPath) {
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
        this.outboxPath = Path.of(outboxPath);
    }

    @Override
    public synchronized ReplicationOutboxEntry enqueue(ReplicationOutboxOperationType operationType,
                                                       String targetSiteUrl, HttpMethod httpMethod, Object payload,
                                                       UUID localDatabaseId, UUID localTableId, UUID remoteDatabaseId,
                                                       UUID remoteTableId, String lastError) {
        final Instant now = Instant.now();
        final ReplicationOutboxEntry entry = ReplicationOutboxEntry.builder()
                .id(UUID.randomUUID())
                .operationType(operationType)
                .status(ReplicationOutboxStatus.PENDING)
                .targetSiteUrl(targetSiteUrl)
                .httpMethod(httpMethod.name())
                .localDatabaseId(localDatabaseId)
                .localTableId(localTableId)
                .remoteDatabaseId(remoteDatabaseId)
                .remoteTableId(remoteTableId)
                .payloadJson(writePayload(payload))
                .attempts(0)
                .lastError(lastError)
                .createdAt(now)
                .updatedAt(now)
                .nextAttemptAt(now)
                .build();
        final List<ReplicationOutboxEntry> entries = readEntries();
        entries.add(entry);
        writeEntries(entries);
        return entry;
    }

    @Override
    public synchronized List<ReplicationOutboxEntry> findAll() {
        return readEntries();
    }

    @Override
    public synchronized List<ReplicationOutboxEntry> findDue(Instant now, int limit) {
        return readEntries().stream()
                .filter(entry -> ReplicationOutboxStatus.PENDING.equals(entry.getStatus()))
                .filter(entry -> entry.getNextAttemptAt() == null || !entry.getNextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(ReplicationOutboxEntry::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized Optional<ReplicationOutboxEntry> findById(UUID id) {
        return readEntries().stream()
                .filter(entry -> id.equals(entry.getId()))
                .findFirst();
    }

    @Override
    public synchronized void markSucceeded(UUID id) {
        update(id, entry -> {
            entry.setStatus(ReplicationOutboxStatus.SUCCEEDED);
            entry.setUpdatedAt(Instant.now());
            entry.setNextAttemptAt(null);
        });
    }

    @Override
    public synchronized void markFailed(UUID id, String error, Duration retryDelay, int maxAttempts) {
        update(id, entry -> {
            final int attempts = entry.getAttempts() + 1;
            entry.setAttempts(attempts);
            entry.setLastError(error);
            entry.setUpdatedAt(Instant.now());
            if (attempts >= maxAttempts) {
                entry.setStatus(ReplicationOutboxStatus.FAILED);
                entry.setNextAttemptAt(null);
                return;
            }
            entry.setStatus(ReplicationOutboxStatus.PENDING);
            entry.setNextAttemptAt(Instant.now().plus(retryDelay));
        });
    }

    private void update(UUID id, EntryUpdater updater) {
        final List<ReplicationOutboxEntry> entries = readEntries();
        entries.stream()
                .filter(entry -> id.equals(entry.getId()))
                .findFirst()
                .ifPresent(updater::update);
        writeEntries(entries);
    }

    private List<ReplicationOutboxEntry> readEntries() {
        if (!Files.exists(outboxPath)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(outboxPath.toFile(), ENTRY_LIST);
        } catch (IOException e) {
            log.error("Failed to read replication outbox {}: {}", outboxPath, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private void writeEntries(List<ReplicationOutboxEntry> entries) {
        try {
            final Path parent = outboxPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            final Path tmp = outboxPath.resolveSibling(outboxPath.getFileName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), entries);
            try {
                Files.move(tmp, outboxPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, outboxPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to write replication outbox {}: {}", outboxPath, e.getMessage(), e);
        }
    }

    private String writePayload(Object payload) {
        try {
            return payload == null ? null : objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to serialize replication outbox payload", e);
        }
    }

    private interface EntryUpdater {
        void update(ReplicationOutboxEntry entry);
    }
}
