package at.ac.tuwien.ifs.dbrepo.core.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class LicenseDto {

    @NotNull
    @Schema(description = "The id", example = "MIT")
    private String identifier;

    @NotBlank
    @Schema(description = "The link to the license such as an SPDX identifier", example = "https://spdx.org/licenses/MIT.html")
    private String uri;

    @Schema(description = "A user-friendly short abstract of key details of the license", example = "A short and simple permissive license with conditions only requiring preservation of copyright and license notices. Licensed works, modifications, and larger works may be distributed under different terms and without source code.")
    private String description;

}