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

    @NotNull
    @Schema(description = "The username", example = "foobar")
    private String username;

    @NotBlank
    @Schema(description = "The password of the user that owns the database", example = "s3cr3t")
    private String password;

}
