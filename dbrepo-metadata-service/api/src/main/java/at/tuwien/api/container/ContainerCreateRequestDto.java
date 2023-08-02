package at.tuwien.api.container;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
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
    @Schema(description = "Image ID")
    private Long imageId;

    @NotBlank
    @Schema(description = "Hostname of container")
    private String host;

    @Schema(description = "Port of container")
    private Integer port;

    @NotBlank
    @Schema(description = "Username of privileged user", example = "root")
    private String privilegedUsername;

    @NotBlank
    @Schema(description = "Password of privileged user")
    private String privilegedPassword;
}
