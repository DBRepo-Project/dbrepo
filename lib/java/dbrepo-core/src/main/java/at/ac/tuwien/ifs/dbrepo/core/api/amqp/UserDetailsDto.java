package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserDetailsDto {

    @NotNull
    @Schema(description = "The user name", example = "jdoe")
    private String name;

    @NotNull
    @JsonProperty("password_hash")
    @Schema(description = "The user password hash", example = "LP5aXqGKWjygzwHnTjmrv1U8M+LW5kI243X/sFTE6I3XyNi3")
    private String passwordHash;

    @NotNull
    @JsonProperty("hashing_algorithm")
    @Schema(description = "The hashing algorithm used to generate the value in password_hash", example = "rabbit_password_hashing_sha256")
    private String hashingAlgorithm;

    @NotNull
    @Schema(description = "The user tags")
    private String[] tags;

}
