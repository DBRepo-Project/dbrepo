package at.ac.tuwien.ifs.dbrepo.core.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ReplicationMonitoringReplicaDto {

    @NotNull
    @Schema(description = "Replica site URL", example = "http://site-b.example.com")
    @JsonProperty("site_url")
    private String siteUrl;

    @Schema(description = "Replica database ID, if known")
    @JsonProperty("remote_database_id")
    private UUID remoteDatabaseId;

    @NotNull
    @Schema(description = "Count of tuples replicated to this replica", example = "42")
    @JsonProperty("replicated_count")
    private Long replicatedCount;

    @NotNull
    @Schema(description = "Count of tuples missing on this replica relative to source", example = "3")
    @JsonProperty("missing_count")
    private Long missingCount;

    @JsonProperty("missing_fraction")
    @Schema(description = "Missing tuples relative to the source count", example = "0.1")
    private Double missingFraction;

    @JsonProperty("status")
    @Schema(description = "Replica health status for this table", example = "healthy")
    private String status;

    @JsonProperty("lag_seconds")
    @Schema(description = "Replication lag in seconds relative to the primary", example = "12")
    private Long lagSeconds;

    @JsonProperty("latest_primary_timestamp")
    @Schema(description = "Latest known timestamp on the primary used as reference", nullable = true)
    private Instant latestPrimaryTimestamp;

    @JsonProperty("latest_replica_timestamp")
    @Schema(description = "Latest known timestamp replicated to this site", nullable = true)
    private Instant latestReplicaTimestamp;

    @JsonProperty("anomalies")
    @Schema(description = "Replica-specific anomalies detected for this table", nullable = true)
    private List<String> anomalies;
}


