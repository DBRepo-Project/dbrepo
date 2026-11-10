package at.ac.tuwien.ifs.dbrepo.core.api.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.AssertTrue;
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

    @Min(1)
    @Max(65535)
    @Schema(description = "The container port; defaults to the image port", example = "3306")
    private Integer port;

    @NotNull
    @Min(1)
    @Max(Integer.MAX_VALUE)
    @Schema(description = "Maximum number of databases", example = "50")
    private Long quota;

    @NotBlank
    @JsonProperty("privileged_username")
    @Schema(description = "The username of the privileged user", example = "root")
    private String privilegedUsername;

    @ToString.Exclude
    @NotBlank
    @JsonProperty("privileged_password")
    @Schema(description = "The password of the privileged user", example = "dbrepo")
    private String privilegedPassword;

    @NotBlank
    @JsonProperty("readonly_username")
    @Schema(description = "Dedicated read-only database user for research databases", example = "dbrepo_readonly")
    private String readonlyUsername;

    @ToString.Exclude
    @NotBlank
    @JsonProperty("readonly_password")
    @Schema(description = "Plaintext password of the dedicated read-only database user")
    private String readonlyPassword;

    @JsonProperty("ui_host")
    private String uiHost;

    @Min(1)
    @Max(65535)
    @JsonProperty("ui_port")
    private Integer uiPort;

    @AssertTrue(message = "Read-only and privileged usernames must differ")
    @JsonIgnore
    public boolean isReadonlyUserSeparate() {
        return readonlyUsername == null || !readonlyUsername.equals(privilegedUsername);
    }
}
