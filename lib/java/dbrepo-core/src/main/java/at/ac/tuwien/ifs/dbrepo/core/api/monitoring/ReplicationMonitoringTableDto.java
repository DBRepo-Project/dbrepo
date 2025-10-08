package at.ac.tuwien.ifs.dbrepo.core.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

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
public class ReplicationMonitoringTableDto {

    @NotNull
    @Schema(description = "Table ID")
    private UUID id;

    @NotNull
    @Schema(description = "Table name")
    private String name;

    @NotNull
    @JsonProperty("internal_name")
    @Schema(description = "Internal table name")
    private String internalName;

    @NotNull
    @JsonProperty("tuple_count")
    @Schema(description = "Total tuples in source (lifetime for versioned)")
    private Long tupleCount;

    @JsonProperty("replicas")
    @Schema(description = "Counts per replica")
    private List<ReplicationMonitoringReplicaDto> replicas;
}


