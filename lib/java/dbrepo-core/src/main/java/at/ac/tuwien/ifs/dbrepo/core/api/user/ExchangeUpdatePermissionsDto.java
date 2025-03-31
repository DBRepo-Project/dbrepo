package at.ac.tuwien.ifs.dbrepo.core.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class ExchangeUpdatePermissionsDto {

    @NotBlank
    @Schema(example = "airquality")
    private String exchange;

    @NotBlank
    @Schema(example = ".*")
    private String write;

    @NotBlank
    @Schema(example = ".*")
    private String read;

}
