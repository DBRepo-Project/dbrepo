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
    @Schema(example = "http://www.wikidata.org/")
    private String uri;

    @JsonProperty("uri_pattern")
    @Schema(example = "http://www.wikidata.org/entity/.*")
    private String uriPattern;

    @NotBlank
    @Schema(example = "wd")
    private String prefix;

    @NotNull
    @Schema(example = "true")
    private Boolean sparql;

    @NotNull
    @Schema(example = "false")
    private Boolean rdf;

    @JsonProperty("sparql_endpoint")
    @Schema(example = "https://query.wikidata.org/sparql")
    private String sparqlEndpoint;

    @JsonProperty("rdf_path")
    @Schema(example = "rdf/om-2.0.rdf")
    private String rdfPath;

    private UserBriefDto creator;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
