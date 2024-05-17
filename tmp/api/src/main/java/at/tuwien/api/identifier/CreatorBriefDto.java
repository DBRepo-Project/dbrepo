package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreatorBriefDto {

    @JsonProperty("name_identifier")
    @Schema(example = "https://orcid.org/0000-0002-1825-0097")
    private String nameIdentifier;

    @JsonProperty("name_type")
    @Schema(example = "Personal")
    private NameTypeDto nameType;

    @NotBlank
    @JsonProperty("creator_name")
    @Schema(example = "Carberry, Josiah")
    private String creatorName;

    @Schema(example = "Wesleyan University")
    private String affiliation;

}
