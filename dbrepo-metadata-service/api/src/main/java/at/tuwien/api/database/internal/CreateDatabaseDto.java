package at.tuwien.api.database.internal;

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
    @Schema(example = "83ea2326-f8f6-4263-baf8-cdf88a54efc7")
    private UUID containerId;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "weather")
    private String internalName;

    @NotBlank
    @JsonProperty("privileged_username")
    @Schema(example = "root")
    private String privilegedUsername;

    @NotBlank
    @JsonProperty("privileged_password")
    @Schema(example = "mariadb")
    private String privilegedPassword;

    @NotNull
    @JsonProperty("user_id")
    @Schema(example = "0e695ea5-9249-4a75-a77a-eeac3ec1c2c0")
    private UUID userId;

    @NotBlank
    @Schema(example = "foobar")
    private String username;

    @NotBlank
    @Schema(example = "s3cr3t")
    private String password;

}
