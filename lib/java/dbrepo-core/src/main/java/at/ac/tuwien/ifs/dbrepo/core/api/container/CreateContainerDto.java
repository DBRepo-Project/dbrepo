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
    @Schema(description = "The user-friendly container name", example = "Air Quality")
    private String name;

    @NotNull
    @JsonProperty("image_id")
    @Schema(description = "The image id used for the container database engine", example = "2360f3c4-85e0-4fac-a7c6-73b296b9dde2")
    private UUID imageId;

    @NotBlank
    @Schema(description = "The container hostname", example = "mariadb")
    private String host;

    @NotNull
    @Schema(description = "The container port", example = "3306")
    private Integer port;

    @NotNull
    @Schema(example = "50")
    private Long quota;

    @NotBlank
    @JsonProperty("privileged_username")
    @Schema(description = "The username of the privileged user", example = "root")
    private String privilegedUsername;

    @NotBlank
    @JsonProperty("privileged_password")
    @Schema(description = "The password of the privileged user", example = "dbrepo")
    private String privilegedPassword;
}
