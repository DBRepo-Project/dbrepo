package at.ac.tuwien.ifs.dbrepo.service.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplicationOutboxEntry {

    private UUID id;

    private ReplicationOutboxOperationType operationType;

    private ReplicationOutboxStatus status;

    private String targetSiteUrl;

    private String httpMethod;

    private UUID localDatabaseId;

    private UUID localTableId;

    private UUID remoteDatabaseId;

    private UUID remoteTableId;

    private String payloadJson;

    private int attempts;

    private String lastError;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant nextAttemptAt;
}
