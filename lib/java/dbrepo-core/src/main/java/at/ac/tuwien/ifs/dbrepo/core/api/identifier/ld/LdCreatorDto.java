package at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld;

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
public class LdCreatorDto {

    @NotNull
    @Schema(description = "The name", example = "Bar, Foo")
    private String name;

    @NotNull
    @JsonProperty("@type")
    @Schema(description = "The type", example = "Person")
    private String type;

    @Schema(description = "The unambiguous reference to a person's identifier", example = "https://orcid.org/0000-0002-1825-0097")
    private String sameAs;

    @Schema(description = "The firstname", example = "Foo")
    private String givenName;

    @Schema(description = "The lastname", example = "Bar")
    private String familyName;

}
