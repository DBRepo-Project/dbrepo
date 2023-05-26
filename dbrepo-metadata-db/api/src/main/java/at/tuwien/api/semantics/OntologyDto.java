package at.tuwien.api.semantics;

import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OntologyDto {

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

    @JsonProperty("sparql_endpoint")
    @Schema(example = "Ontology SPARQL endpoint")
    private String sparqlEndpoint;

    private UserBriefDto creator;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21.678396092Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
