package at.tuwien.api.auth;

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
public class JwtResponseDto {

    @NotNull
    @Parameter(name = "jwt")
    private String token;

    @NotNull
    @Parameter(name = "user type")
    private String type;

    @NotNull
    @Parameter(name = "id")
    private Long id;

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @Parameter(name = "user email")
    private String email;

    @NotNull
    @Parameter(name = "user roles")
    private List<String> roles;

}
