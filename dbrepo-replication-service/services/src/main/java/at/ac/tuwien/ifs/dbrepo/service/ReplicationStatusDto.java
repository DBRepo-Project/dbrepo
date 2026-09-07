package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationHealthDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ReplicationStatusDto(
        @JsonProperty("health") ReplicationHealthDto health,
        @JsonProperty("outbox") ReplicationOutboxSummaryDto outbox,
        @JsonProperty("outboxes") ReplicationOutboxStatusDto outboxes) {
}
