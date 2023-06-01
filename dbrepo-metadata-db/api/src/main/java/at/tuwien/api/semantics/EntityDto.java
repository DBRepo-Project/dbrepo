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
public class EntityDto {

    @NotBlank
    @Schema(example = "https://www.wikidata.org/entity/Q1686799")
    private String uri;

    @NotBlank
    @Schema(example = "Apache Jena")
    private String label;

    @Schema(example = "open source semantic web framework for Java")
    private String description;

}
