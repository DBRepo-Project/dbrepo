package at.tuwien.api.user;

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
public class UserResetDto {

    @NotNull
    @Parameter(name = "user password")
    private String password;

    @NotNull
    @Parameter(name = "token")
    private String token;

}
