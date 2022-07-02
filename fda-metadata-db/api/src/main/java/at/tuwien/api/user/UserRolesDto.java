package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRolesDto {

    @NotNull
    @Parameter(name = "user roles")
    private List<String> roles;

}
