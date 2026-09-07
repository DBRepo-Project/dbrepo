package at.ac.tuwien.ifs.dbrepo.service.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TupleReplicationOutboxEntry {

    private UUID id;

    private UUID databaseId;

    private UUID tableId;

    private HttpMethod httpMethod;

    private String payloadJson;

    private TupleReplicationOutboxStatus status;

    private int attempts;

    private String lastError;

    private Instant created;

    private Instant lastModified;

    private Instant nextAttemptAt;
}
