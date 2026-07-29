package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;
import java.util.UUID;

/**
 * DTO representing a tuple that is missing on a specific replica site.
 * Used during startup synchronization to identify and replicate missing data.
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@Schema(description = "Represents a tuple missing on a specific replica site")
public class MissingTupleDto {

    @JsonProperty("replication_key")
    @Schema(description = "Unique replication key identifying this tuple across sites")
    private String replicationKey;

    @JsonProperty("table_id")
    @Schema(description = "Table ID where this tuple belongs")
    private UUID tableId;

    @JsonProperty("data")
    @Schema(description = "The tuple data as key-value pairs")
    private Map<String, Object> data;
}
