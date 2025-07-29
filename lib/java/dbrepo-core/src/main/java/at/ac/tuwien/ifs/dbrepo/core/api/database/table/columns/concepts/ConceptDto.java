package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnBriefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ConceptDto {

    @NotNull
    @Schema(description = "The concept id", example = "8cabc011-4bdf-44d4-9d33-b2648e2ddbf1")
    private UUID id;

    @NotBlank
    @Schema(description = "The concept URI", example = "http://www.wikidata.org/entity/Q202444")
    private String uri;

    @Schema(description = "The concept name, taken as label from the ontology", example = "given name")
    private String name;

    @Schema(description = "The concept description giving a user-friendly explanation", example = "physical property of matter that quantitatively expresses the common notions of hot and cold\n")
    private String description;

    @NotNull
    private List<ColumnBriefDto> columns;
}
