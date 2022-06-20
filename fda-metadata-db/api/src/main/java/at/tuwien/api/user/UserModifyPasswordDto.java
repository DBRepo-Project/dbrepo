package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserModifyPasswordDto {

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @Parameter(name = "user password")
    private String password;

}
