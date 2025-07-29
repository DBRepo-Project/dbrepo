package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class ConceptSaveDto {

    @NotBlank
    @Schema(description = "The concept URI", example = "http://www.wikidata.org/entity/Q202444")
    private String uri;

    @Schema(description = "The concept name, taken as label from the ontology", example = "given name")
    private String name;

    @Schema(description = "The concept description giving a user-friendly explanation", example = "physical property of matter that quantitatively expresses the common notions of hot and cold\n")
    private String description;

}
