package at.tuwien.api.container.image;

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
public class ImageBriefDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "mariadb")
    private String repository;

    @NotBlank
    @Schema(example = "10.5")
    private String tag;

}
