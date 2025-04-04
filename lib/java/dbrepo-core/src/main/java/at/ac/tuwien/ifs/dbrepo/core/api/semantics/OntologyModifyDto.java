package at.ac.tuwien.ifs.dbrepo.core.api.semantics;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class OntologyModifyDto {

    @NotBlank
    @Schema(example = "Ontology URI")
    private String uri;

    @NotBlank
    @Schema(example = "Ontology prefix")
    private String prefix;

    @JsonProperty("sparql_endpoint")
    @Schema(example = "Ontology SPARQL endpoint")
    private String sparqlEndpoint;

    @JsonProperty("rdf_path")
    @Schema(example = "rdf/om-2.0.rdf")
    private String rdfPath;

}
