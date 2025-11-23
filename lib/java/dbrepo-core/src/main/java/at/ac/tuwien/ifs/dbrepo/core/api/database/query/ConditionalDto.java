package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

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
public class ConditionalDto {

    @NotNull
    @JsonProperty("column_id")
    @Schema(description = "The id of the column", example = "f7df2a7d-4ade-4c78-97b0-7c744d0893c7")
    private UUID columnId;

    @NotNull
    @JsonProperty("foreign_column_id")
    @Schema(description = "The id of the foreign column", example = "2eac80eb-f313-456c-b003-86855d53e080")
    private UUID foreignColumnId;

}
