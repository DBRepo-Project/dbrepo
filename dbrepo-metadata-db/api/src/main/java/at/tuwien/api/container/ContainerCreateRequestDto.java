package at.tuwien.api.container;

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
public class ContainerCreateRequestDto {

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "mariadb")
    private String repository;

    @NotBlank
    @Schema(example = "10.5")
    private String tag;

}
