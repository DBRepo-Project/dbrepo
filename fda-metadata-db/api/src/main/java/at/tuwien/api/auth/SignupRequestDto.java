package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequestDto {

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @Email
    @Parameter(name = "user email")
    private String email;

    @NotNull
    @Parameter(name = "password hash")
    private String password;

}
