package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CreateUserDto {

    @NotBlank
    @Pattern(regexp = "^[a-z0-9]{3,}$")
    @Schema(example = "user")
    private String username;

    @NotBlank
    @Email
    @Schema(example = "user@example.com")
    private String email;

    @NotNull
    @ToString.Exclude
    private String password;

}
