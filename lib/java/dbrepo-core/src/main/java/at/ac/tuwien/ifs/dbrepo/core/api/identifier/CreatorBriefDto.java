package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

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
public class CreatorBriefDto {

    @NotNull
    @Schema(example = "e41f94a6-2b94-4a12-ac0e-678684e1c070")
    private UUID id;

    @NotBlank
    @JsonProperty("creator_name")
    @Schema(example = "Carberry, Josiah")
    private String creatorName;

    @JsonProperty("name_type")
    @Schema(example = "Personal")
    private NameTypeDto nameType;

    @JsonProperty("name_identifier")
    @Schema(example = "0000-0002-1825-0097")
    private String nameIdentifier;

    @JsonProperty("name_identifier_scheme")
    @Schema(example = "ORCID")
    private NameIdentifierSchemeTypeDto nameIdentifierScheme;

    @Schema(example = "Brown University")
    private String affiliation;

    @JsonProperty("affiliation_identifier")
    @Schema(example = "https://ror.org/05gq02987")
    private String affiliationIdentifier;

    @JsonProperty("affiliation_identifier_scheme")
    @Schema(example = "ROR")
    private AffiliationIdentifierSchemeTypeDto affiliationIdentifierScheme;

}
