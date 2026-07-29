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
public class ReplicationMonitoringDatabaseDto {

    @NotNull
    @Schema(description = "Database ID")
    private UUID id;

    @NotNull
    @Schema(description = "Database name")
    private String name;

    @NotNull
    @JsonProperty("internal_name")
    @Schema(description = "Internal database name")
    private String internalName;

    @NotNull
    @JsonProperty("table_count")
    @Schema(description = "Number of tables")
    private Integer tableCount;

    @NotNull
    @JsonProperty("lifetime_tuple_count")
    @Schema(description = "Sum of tuples across tables (lifetime)")
    private Long lifetimeTupleCount;

    @NotNull
    @Schema(description = "Tables in this database")
    private List<ReplicationMonitoringTableDto> tables;

    @JsonProperty("sites")
    @Schema(description = "Aggregated site-level monitoring information", nullable = true)
    private List<ReplicationMonitoringSiteDto> sites;
}
