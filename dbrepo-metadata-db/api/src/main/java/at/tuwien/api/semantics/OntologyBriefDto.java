package at.tuwien.api.semantics;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    @Schema(example = "Ontology URI")
    private String uri;

}
