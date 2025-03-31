package at.ac.tuwien.ifs.dbrepo.core.api.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserAttributesDto {

    @Schema(example = "dark")
    @JsonProperty("THEME")
    private String[] theme;

    @Schema(example = "en")
    @JsonProperty("LANGUAGE")
    private String[] language;

    @Schema(example = "https://ror.org/04d836q62")
    @JsonProperty("AFFILIATION")
    private String[] affiliation;

    @Schema(example = "https://orcid.org/0000-0003-4216-302X")
    @JsonProperty("ORCID")
    private String[] orcid;

}
