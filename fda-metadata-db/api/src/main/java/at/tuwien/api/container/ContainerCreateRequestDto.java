package at.tuwien.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ContainerCreateRequestDto {

    @NotBlank
    @Parameter(name = "name", example = "Weather World")
    private String name;

    @NotBlank
    @Parameter(name = "repository", example = "postgres")
    private String repository;

    @NotBlank
    @Parameter(name = "tag", example = "latest")
    private String tag = "latest";

}
