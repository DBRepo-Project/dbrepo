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
public class ImageBriefDto {

    @NotNull
    @Parameter(required = true, example = "1")
    private Long id;

    @NotBlank
    @Parameter(required = true, example = "mariadb")
    private String repository;

    @ToString.Exclude
    @Parameter(required = true, example = "base64:aaaa")
    private String logo;

    @NotBlank
    @Parameter(required = true, example = "10.5")
    private String tag;

}
