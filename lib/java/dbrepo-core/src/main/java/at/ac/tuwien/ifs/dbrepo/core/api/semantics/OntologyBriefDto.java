package at.ac.tuwien.ifs.dbrepo.core.api.semantics;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class OntologyBriefDto {

    @NotNull
    @Schema(example = "7c491e40-082a-47b8-b82c-51d03c520466")
    private UUID id;

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

}
