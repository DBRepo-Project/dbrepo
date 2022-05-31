package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.Email;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserForgotDto {

    @Parameter(name = "user username")
    private String username;

    @Email
    @Parameter(name = "user email")
    private String email;

}
