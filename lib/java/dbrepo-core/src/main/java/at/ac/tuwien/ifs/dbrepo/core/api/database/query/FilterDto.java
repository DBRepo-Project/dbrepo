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
public class FilterDto {

    @NotNull
    @Schema(description = "The filter type", example = "where")
    private FilterTypeDto type;

    @NotNull
    @JsonProperty("column_id")
    @Schema(description = "The column id", example = "14128033-54b5-4818-a489-21b0dded86e2")
    private UUID columnId;

    @NotNull
    @JsonProperty("operator_id")
    @Schema(description = "The operator id", example = "67c5b54d-2eb0-4f42-8dc1-a504562e9f32")
    private UUID operatorId;

    @Schema(description = "The filter value", example = "1")
    private String value;

}
