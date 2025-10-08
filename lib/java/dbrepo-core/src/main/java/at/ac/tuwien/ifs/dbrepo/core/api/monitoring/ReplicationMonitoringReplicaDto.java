package at.ac.tuwien.ifs.dbrepo.core.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

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
}


