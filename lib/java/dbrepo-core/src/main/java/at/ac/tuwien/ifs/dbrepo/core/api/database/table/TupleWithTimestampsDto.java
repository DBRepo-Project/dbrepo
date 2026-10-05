package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import  io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TupleWithTimestampsDto {

    @NotNull
    @Schema(description = "The key-value data map", example = "{\"key\": \"value\"}")
    private Map<String, Object> data;

    @Schema(description = "Timestamp when the tuple was inserted")
    private Instant insertedAt;

    @Schema(description = "Timestamp when the tuple was deleted (null if still active)")
    private Instant deletedAt;

    @Schema(description = "Replication key for the tuple")
    private String replicationKey;

    @Schema(description = "Identity of the values version, shared by all sites")
    private UUID versionId;

    private Long visibilityStart;

    private Long visibilityEnd;

    @Schema(description = "Whether this event has a local version; false for superseded events and empty deletes")
    private Boolean applied;
}
