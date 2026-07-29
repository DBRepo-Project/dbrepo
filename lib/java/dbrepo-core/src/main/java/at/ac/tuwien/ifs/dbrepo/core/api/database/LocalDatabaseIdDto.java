package at.ac.tuwien.ifs.dbrepo.core.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for returning the local database ID when looking up by replica database ID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Local database ID response for replica database ID lookup")
public class LocalDatabaseIdDto {

    @Schema(description = "The local database ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID localDatabaseId;

    @Schema(description = "The replica database ID that was used for lookup", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID replicaDatabaseId;
}
