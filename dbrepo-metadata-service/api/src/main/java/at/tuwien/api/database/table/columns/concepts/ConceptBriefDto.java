package at.tuwien.api.database.table.columns.concepts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class ConceptBriefDto {

    @NotNull
    @Schema(example = "8cabc011-4bdf-44d4-9d33-b2648e2ddbf1")
    private UUID id;

    @NotBlank
    @Schema(example = "http://www.wikidata.org/entity/Q202444")
    private String uri;

    @Schema(example = "given name")
    private String name;

    @Schema(example = "name typically used to differentiate people from the same family, clan, or other social group who have a common last name")
    private String description;

}
