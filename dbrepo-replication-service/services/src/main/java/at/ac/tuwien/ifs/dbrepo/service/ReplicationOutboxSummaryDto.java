package at.ac.tuwien.ifs.dbrepo.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ReplicationOutboxSummaryDto(
        @JsonProperty("total") long total,
        @JsonProperty("pending") long pending,
        @JsonProperty("failed") long failed,
        @JsonProperty("succeeded") long succeeded,
        @JsonProperty("oldest_pending_at") Instant oldestPendingAt,
        @JsonProperty("next_attempt_at") Instant nextAttemptAt) {
}
