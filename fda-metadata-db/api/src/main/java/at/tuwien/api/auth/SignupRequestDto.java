package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {

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
