package at.tuwien.api.semantics;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OntologyBriefDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Ontology URI")
    private String uri;

    @NotBlank
    @Schema(example = "Ontology prefix")
    private String prefix;

    @NotNull
    @Schema(example = "true")
    private Boolean sparql;

    @NotNull
    @Schema(example = "true")
    private Boolean rdf;

}
