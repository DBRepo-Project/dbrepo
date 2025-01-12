package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserUpdateDto {

    @Schema(example = "Josiah")
    private String firstname;

    @Schema(example = "Carberry")
    private String lastname;

    @Schema(example = "Brown University")
    private String affiliation;

    @Schema(example = "0000-0002-1825-0097")
    private String orcid;

    @NotNull
    @Schema(example = "dark")
    private String theme;

    @NotNull
    @Schema(example = "en")
    private String language;

}
