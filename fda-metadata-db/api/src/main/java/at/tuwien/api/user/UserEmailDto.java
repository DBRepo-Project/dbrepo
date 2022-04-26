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
public class UserEmailDto {

    @NotNull
    @Parameter(name = "user email")
    private String email;

}
