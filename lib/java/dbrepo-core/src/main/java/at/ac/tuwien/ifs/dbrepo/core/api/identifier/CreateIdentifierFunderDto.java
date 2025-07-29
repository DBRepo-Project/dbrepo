package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

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
public class CreateIdentifierFunderDto {

    @NotBlank
    @JsonProperty("funder_name")
    @Schema(description = "The funder name", example = "European Commission")
    private String funderName;

    @JsonProperty("funder_identifier")
    @Schema(description = "The identifier that identifies the funder unambiguously", example = "http://doi.org/10.13039/501100000780")
    private String funderIdentifier;

    @JsonProperty("funder_identifier_type")
    @Schema(description = "The funder type, when the `funder_identifier` is a DOI, the `funder_identifier_type` field must be `Crossref Funder ID`", example = "Crossref Funder ID")
    private IdentifierFunderTypeDto funderIdentifierType;

    @JsonProperty("scheme_uri")
    @Schema(description = "The scheme URI of the `funder_identifier`", example = "http://doi.org/")
    private String schemeUri;

    @JsonProperty("award_number")
    @Schema(description = "The award number", example = "824087")
    private String awardNumber;

    @JsonProperty("award_title")
    @Schema(description = "The award title", example = "EOSC-Life")
    private String awardTitle;

}


