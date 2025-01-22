package at.tuwien.api.database.table.columns.concepts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UnitBriefDto {

    @NotNull
    @Schema(example = "34")
    private Long id;

    @NotBlank
    @Schema(example = "http://www.wikidata.org/entity/Q1422583")
    private String uri;

    @Schema(example = "importance")
    private String name;

    @Schema(example = "subjective magnitude of value, meaning, or purpose")
    private String description;
}
