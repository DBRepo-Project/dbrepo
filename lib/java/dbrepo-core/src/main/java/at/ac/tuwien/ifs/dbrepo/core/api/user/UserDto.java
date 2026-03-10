package at.ac.tuwien.ifs.dbrepo.core.api.user;

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
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserDto {

    @Schema(example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    private UUID id;

    @Schema(example = "Josiah Carberry")
    private String name;

    @NotBlank
    @Schema(example = "username")
    private String username;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Schema(example = "s3cr3t1nf0rm4t10n")
    private String password;

    @JsonProperty("qualified_name")
    @Schema(example = "Josiah Carberry — @jcarberry")
    private String qualifiedName;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
    private String lastname;

    @NotNull
    @EqualsAndHashCode.Exclude
    private UserAttributesDto attributes;

}
