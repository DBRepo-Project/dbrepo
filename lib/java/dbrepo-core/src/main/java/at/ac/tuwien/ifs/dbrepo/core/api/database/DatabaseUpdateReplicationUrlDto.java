package at.ac.tuwien.ifs.dbrepo.core.api.database;

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
public class DatabaseUpdateReplicationUrlDto {

    @NotNull
    @JsonProperty("replica_url")
    @Schema(example = "http://remote-server:8080/api/database")
    private String replicaUrl;

    @NotNull
    @JsonProperty("replica_database_id")
    @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID replicaDatabaseId;

} 