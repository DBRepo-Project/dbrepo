package at.tuwien.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
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
    @ApiModelProperty(name = "name", example = "Weather World")
    private String name;

    @NotBlank
    @ApiModelProperty(name = "repository", example = "postgres")
    private String repository;

    @NotBlank
    @ApiModelProperty(name = "tag", example = "latest")
    private String tag = "latest";

    @NotNull
    @JsonProperty("is_public")
    @ApiModelProperty(name = "public", example = "true")
    private Boolean isPublic;

}
