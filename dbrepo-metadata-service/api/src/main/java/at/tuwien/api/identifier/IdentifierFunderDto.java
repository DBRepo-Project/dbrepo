package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierFunderDto {

    private Long id;

    @NotBlank
    @JsonProperty("funder_name")
    @Field(name = "funder_name")
    @Schema(example = "European Commission")
    private String funderName;

    @JsonProperty("funder_identifier")
    @Field(name = "funder_identifier")
    @Schema(example = "http://doi.org/10.13039/501100000780")
    private String funderIdentifier;

    @JsonProperty("funder_identifier_type")
    @Field(name = "funder_identifier_type")
    @Schema(example = "Crossref Funder ID")
    private IdentifierFunderTypeDto funderIdentifierType;

    @JsonProperty("scheme_uri")
    @Field(name = "scheme_uri")
    @Schema(example = "http://doi.org/")
    private String schemeUri;

    @JsonProperty("award_number")
    @Field(name = "award_number")
    @Schema(example = "824087")
    private String awardNumber;

    @JsonProperty("award_title")
    @Field(name = "award_title")
    @Schema(example = "EOSC-Life")
    private String awardTitle;

}


