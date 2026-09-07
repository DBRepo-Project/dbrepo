package at.ac.tuwien.ifs.dbrepo.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ReplicationOutboxSummaryDto(
        @JsonProperty("total") long total,
        @JsonProperty("pending") long pending,
        @JsonProperty("failed") long failed,
        @JsonProperty("succeeded") long succeeded,
        @JsonProperty("oldest_pending_at") Instant oldestPendingAt,
        @JsonProperty("next_attempt_at") Instant nextAttemptAt,
        @JsonProperty("available") boolean available,
        @JsonProperty("error") String error) {

    public ReplicationOutboxSummaryDto(long total, long pending, long failed, long succeeded,
                                       Instant oldestPendingAt, Instant nextAttemptAt) {
        this(total, pending, failed, succeeded, oldestPendingAt, nextAttemptAt, true, null);
    }

    public static ReplicationOutboxSummaryDto unavailable(String error) {
        return new ReplicationOutboxSummaryDto(0, 0, 0, 0, null, null, false, error);
    }
}
