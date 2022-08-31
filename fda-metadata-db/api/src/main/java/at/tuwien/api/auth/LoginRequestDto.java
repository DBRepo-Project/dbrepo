package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "password hash")
    private String password;

}
