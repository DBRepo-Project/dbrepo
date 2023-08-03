package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierFunderDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotBlank
    @JsonProperty("funder_name")
    @Schema(example = "European Commission")
    @Field(name = "funder_name", type = FieldType.Keyword)
    private String funderName;

    @JsonProperty("funder_identifier")
    @Schema(example = "http://doi.org/10.13039/501100000780")
    @Field(name = "funder_identifier", type = FieldType.Keyword)
    private String funderIdentifier;

    @JsonProperty("funder_identifier_type")
    @Schema(example = "Crossref Funder ID")
    @Field(name = "funder_identifier_type", type = FieldType.Keyword)
    private IdentifierFunderTypeDto funderIdentifierType;

    @JsonProperty("scheme_uri")
    @Schema(example = "http://doi.org/")
    @Field(name = "scheme_uri", type = FieldType.Keyword)
    private String schemeUri;

    @JsonProperty("award_number")
    @Schema(example = "824087")
    @Field(name = "award_number", type = FieldType.Keyword)
    private String awardNumber;

    @JsonProperty("award_title")
    @Schema(example = "EOSC-Life")
    @Field(name = "award_title", type = FieldType.Keyword)
    private String awardTitle;

}


