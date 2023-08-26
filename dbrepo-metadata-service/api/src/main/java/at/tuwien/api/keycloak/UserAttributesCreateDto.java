package at.tuwien.api.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserAttributesCreateDto {

    @NotNull
    @JsonProperty("theme_dark")
    @Schema(example = "false")
    private Boolean themeDark;

    @Schema(example = "https://orcid.org/0000-0002-1825-0097")
    private String orcid;

    @Schema(example = "Brown University")
    private String affiliation;

}
