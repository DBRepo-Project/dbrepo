package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
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
