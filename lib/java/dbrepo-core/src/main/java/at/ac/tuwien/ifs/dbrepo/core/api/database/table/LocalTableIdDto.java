package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for returning the local table ID when looking up by replica table ID.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Local table ID response for replica table ID lookup")
public class LocalTableIdDto {

    @Schema(description = "The local table ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID localTableId;

    @Schema(description = "The replica table ID that was used for lookup", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID replicaTableId;
}
