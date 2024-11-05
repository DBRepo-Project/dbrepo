package at.tuwien.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ContainerCreateDto {

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotNull
    @JsonProperty("image_id")
    @Schema(description = "Image ID")
    private Long imageId;

    @NotBlank
    @Schema(description = "Hostname of container")
    private String host;

    @Schema(description = "Port of container")
    private Integer port;

    @JsonProperty("ui_host")
    private String uiHost;

    @JsonProperty("ui_port")
    private Integer uiPort;

    @NotNull
    @Schema(example = "50")
    private Long quota;

    @NotBlank
    @JsonProperty("privileged_username")
    @Schema(description = "Username of privileged user", example = "root")
    private String privilegedUsername;

    @NotBlank
    @JsonProperty("privileged_password")
    @Schema(description = "Password of privileged user")
    private String privilegedPassword;
}
