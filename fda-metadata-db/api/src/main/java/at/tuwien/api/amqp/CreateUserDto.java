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
    @ToString.Exclude
    @Parameter(name = "user password")
    private String password;

    @Parameter(name = "user tags")
    private String tags;

}
