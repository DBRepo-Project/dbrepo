package at.ac.tuwien.ifs.dbrepo.core.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ReplicationMonitoringSiteDto {

    @NotBlank
    @JsonProperty("site_url")
    @Schema(description = "Base URL of the replica site", example = "https://replica1.example.com")
    private String siteUrl;

    @JsonProperty("status")
    @Schema(description = "Overall health classification for the site", example = "healthy")
    private String status;

    @JsonProperty("replication_service_reachable")
    @Schema(description = "True if the remote replication service responded successfully")
    private Boolean replicationServiceReachable;

    @JsonProperty("metadata_service_reachable")
    @Schema(description = "True if the remote metadata service responded successfully")
    private Boolean metadataServiceReachable;

    @JsonProperty("data_service_reachable")
    @Schema(description = "True if the remote data service responded successfully")
    private Boolean dataServiceReachable;

    @JsonProperty("broker_reachable")
    @Schema(description = "True if the remote message broker responded successfully")
    private Boolean brokerReachable;

    @JsonProperty("latency_ms")
    @Schema(description = "Latency in milliseconds observed during the health probe", example = "42")
    private Long latencyMs;

    @JsonProperty("last_checked")
    @Schema(description = "Timestamp when the health probe was executed", example = "2025-01-23T12:09:01Z")
    private Instant lastChecked;

    @JsonProperty("message")
    @Schema(description = "Descriptive message for the current site status", example = "Missing tuples detected on multiple tables")
    private String message;
}


