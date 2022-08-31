package at.tuwien.api.container.image;

import io.swagger.v3.oas.annotations.Parameter;
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
    @Parameter(required = true, example = "1")
    private Long iid;

    @NotBlank
    @Parameter(required = true, example = "POSTGRES_USER")
    private String key;

    @NotBlank
    @Parameter(required = true, example = "postgres")
    private String value;

    @NonNull
    @Parameter(required = true, example = "USERNAME")
    private ImageEnvItemTypeDto type;

}
