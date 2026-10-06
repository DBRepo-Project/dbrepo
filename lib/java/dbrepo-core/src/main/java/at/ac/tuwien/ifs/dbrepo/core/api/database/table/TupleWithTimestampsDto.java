package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import  io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@com.fasterxml.jackson.annotation.JsonIgnoreProperties("versionId")
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

    @Schema(description = "Original ROW_START of this values version on the writing site, in UTC microseconds")
    private Instant masterSiteTs;

    private Long visibilityStart;

    private Long visibilityEnd;

    @Schema(description = "Whether this event has a local version; false for superseded events and empty deletes")
    private Boolean applied;
}
