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
public class CreateIdentifierCreatorDto {

    @Schema(description = "The given name", example = "Josiah")
    private String firstname;

    @Schema(description = "The family name", example = "Carberry")
    private String lastname;

    @NotBlank
    @JsonProperty("creator_name")
    @Schema(description = "The full name", example = "Carberry, Josiah")
    private String creatorName;

    @JsonProperty("name_type")
    @Schema(description = "The name type", example = "Personal")
    private NameTypeDto nameType;

    @JsonProperty("name_identifier")
    @Schema(description = "The persistent identifier that identifies the creator unambiguously", example = "https://orcid.org/0000-0002-1825-0097")
    private String nameIdentifier;

    @Schema(description = "The affiliation", example = "Wesleyan University")
    private String affiliation;

    @JsonProperty("affiliation_identifier")
    @Schema(description = "The persistent identifier that identifies the affiliation unambiguously", example = "https://ror.org/04d836q62")
    private String affiliationIdentifier;

}
