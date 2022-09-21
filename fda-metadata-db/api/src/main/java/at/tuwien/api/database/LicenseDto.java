package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
    @Schema(name = "https://opensource.org/licenses/MIT")
    private String uri;

}