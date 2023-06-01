package at.tuwien.api.container.image;

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
public class ImageBriefDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "docker.io/library")
    private String registry;

    @NotBlank
    @Schema(example = "mariadb")
    private String repository;

    @NotBlank
    @Schema(example = "10.5")
    private String tag;

}
