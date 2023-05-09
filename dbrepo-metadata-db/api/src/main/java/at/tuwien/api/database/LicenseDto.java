package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LicenseDto {

    @NotNull
    @Schema(example = "MIT")
    private String identifier;

    @NotBlank
    @Schema(example = "https://opensource.org/licenses/MIT")
    private String uri;

}