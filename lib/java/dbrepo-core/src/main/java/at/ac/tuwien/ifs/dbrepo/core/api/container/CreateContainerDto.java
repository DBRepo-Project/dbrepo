package at.ac.tuwien.ifs.dbrepo.core.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateContainerDto {

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotNull
    @JsonProperty("image_id")
    @Schema(example = "2360f3c4-85e0-4fac-a7c6-73b296b9dde2", description = "Image ID")
    private UUID imageId;

    @NotBlank
    @Schema(example = "data-db2", description = "Hostname of container")
    private String host;

    @Schema(example = "3306", description = "Port of container")
    private Integer port;

    @JsonProperty("ui_host")
    @Schema(example = "example.com")
    private String uiHost;

    @JsonProperty("ui_port")
    @Schema(example = "3306")
    private Integer uiPort;

    @NotNull
    @Schema(example = "50")
    private Long quota;

    @NotBlank
    @JsonProperty("privileged_username")
    @Schema(example = "root", description = "Username of privileged user")
    private String privilegedUsername;

    @NotBlank
    @JsonProperty("privileged_password")
    @Schema(example = "dbrepo", description = "Password of privileged user")
    private String privilegedPassword;
}
