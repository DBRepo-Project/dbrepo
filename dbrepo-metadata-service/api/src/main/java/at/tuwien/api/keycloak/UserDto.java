package at.tuwien.api.keycloak;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserDto {

    @NotNull
    private UUID id;

    @NotNull
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @NotNull
    @JsonProperty("createdTimestamp")
    @Schema(example = "1693048334898")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    private Instant created;

    @NotNull
    @Schema(example = "true")
    private Boolean enabled;

    @NotNull
    @Schema(example = "false")
    private Boolean totp;

    @NotNull
    @JsonProperty("emailVerified")
    @Schema(example = "false")
    private Boolean emailVerified;

    @NotNull
    @Schema(example = "jcarberry@brown.edu")
    private String email;

    @NotNull
    private UserAttributesDto attributes;

    @NotNull
    @JsonProperty("notBefore")
    @Schema(example = "0")
    private Long notBefore;

}
