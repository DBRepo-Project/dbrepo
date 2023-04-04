package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
