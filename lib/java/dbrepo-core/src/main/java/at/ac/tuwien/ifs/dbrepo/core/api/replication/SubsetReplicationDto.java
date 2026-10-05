package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

/** Canonical subset state, not a command to execute SQL or to write base data. */
public record SubsetReplicationDto(
        @NotNull @JsonProperty("query_id") UUID queryId,
        @NotBlank @Size(max = 512) @JsonProperty("origin_site") String originSite,
        @NotBlank @Size(max = 512) @JsonProperty("sender_site") String senderSite,
        @NotNull @JsonProperty("sender_database_id") UUID senderDatabaseId,
        @NotBlank @Size(max = 65535) String query,
        @NotBlank @Size(max = 65535) @JsonProperty("query_normalized") String queryNormalized,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING)
        @JsonProperty("selected_at") Instant selectedAt,
        @NotNull @JsonProperty("is_persisted") Boolean persisted,
        @NotNull @Pattern(regexp = "v2:[0-9a-f]{64}") @JsonProperty("result_hash") String resultHash,
        @NotNull @PositiveOrZero @JsonProperty("result_count") Long resultCount,
        @Positive long revision,
        @Size(max = 262144) @JsonProperty("execution_context") String executionContext) {

    public SubsetReplicationDto(UUID queryId, String originSite, String senderSite, UUID senderDatabaseId,
                                String query, String queryNormalized, Instant selectedAt, Boolean persisted,
                                String resultHash, Long resultCount, long revision) {
        this(queryId, originSite, senderSite, senderDatabaseId, query, queryNormalized, selectedAt, persisted,
                resultHash, resultCount, revision, null);
    }
}
