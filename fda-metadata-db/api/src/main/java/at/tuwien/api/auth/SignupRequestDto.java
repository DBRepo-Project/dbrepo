package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {

    @NotNull
    @Pattern(regexp = "^[a-z0-9]{3,}$")
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @Email
    @Parameter(name = "user email")
    private String email;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "password hash")
    private String password;

}
