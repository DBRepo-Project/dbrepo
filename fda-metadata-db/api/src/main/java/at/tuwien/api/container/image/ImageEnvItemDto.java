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
public class ImageEnvItemDto {

    @NotNull
    private Long iid;

    @NotBlank
    @Schema(example = "MARIADB_ROOT_PASSWORD")
    private String key;

    @NotBlank
    @Schema(example = "mariadb")
    private String value;

    @NotNull
    @Schema(example = "PRIVILEGED_PASSWORD")
    private ImageEnvItemTypeDto type;

}
