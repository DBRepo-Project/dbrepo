package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LicenseDto {

    @NotNull
    @Schema(example = "MIT")
    private String identifier;

    @NotBlank
    @Schema(example = "https://opensource.org/licenses/MIT")
    private String uri;

}