package at.tuwien.api.database.query;

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
    @Schema(example = "where")
    private FilterTypeDto type;

    @NotNull
    @JsonProperty("column_id")
    @Schema(example = "14128033-54b5-4818-a489-21b0dded86e2")
    private UUID columnId;

    @NotNull
    @JsonProperty("operator_id")
    @Schema(example = "67c5b54d-2eb0-4f42-8dc1-a504562e9f32")
    private UUID operatorId;

    @NotNull
    @Schema(example = "1")
    private String value;

}
