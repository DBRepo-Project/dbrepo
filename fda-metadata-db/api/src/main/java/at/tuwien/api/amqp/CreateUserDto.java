package at.tuwien.api.amqp;

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
public class CreateUserDto {

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @Parameter(name = "password hash")
    private String password;

}
