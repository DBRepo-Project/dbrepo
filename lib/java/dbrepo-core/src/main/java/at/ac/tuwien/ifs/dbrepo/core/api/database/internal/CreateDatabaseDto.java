package at.ac.tuwien.ifs.dbrepo.core.api.database.internal;

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
public class CreateDatabaseDto {

    @NotNull
    @JsonProperty("container_id")
    @Schema(description = "The container id", example = "83ea2326-f8f6-4263-baf8-cdf88a54efc7")
    private UUID containerId;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly name of the database", example = "weather")
    private String internalName;

    @NotBlank
    @JsonProperty("privileged_username")
    @Schema(description = "The username of the privileged user", example = "root")
    private String privilegedUsername;

    @NotBlank
    @JsonProperty("privileged_password")
    @Schema(description = "The password of the privileged user", example = "dbrepo")
    private String privilegedPassword;

    @NotBlank
    @JsonProperty("readonly_username")
    @Schema(description = "The username of the user that can only read from the database. This user is used to access the data from the dashboard.", example = "readonly")
    private String readonlyUsername;

    @NotBlank
    @JsonProperty("readonly_password")
    @Schema(description = "The password of the user that can only read from the database. This user is used to access the data from the dashboard.", example = "readonly")
    private String readonlyPassword;

    @NotNull
    @JsonProperty("user_id")
    @Schema(description = "The user id", example = "0e695ea5-9249-4a75-a77a-eeac3ec1c2c0")
    private UUID userId;

    @NotBlank
    @Schema(description = "The username of the user that owns the database", example = "foobar")
    private String username;

    @NotBlank
    @Schema(description = "The password of the user that owns the database", example = "s3cr3t")
    private String password;

}
