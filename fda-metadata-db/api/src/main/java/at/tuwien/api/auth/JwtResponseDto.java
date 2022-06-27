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
    @ToString.Exclude
    @Parameter(name = "jwt")
    private String token;

    @Parameter(name = "user type")
    private String type;

    @Parameter(name = "id")
    private Long id;

    @Parameter(name = "user name")
    private String username;

    @Parameter(name = "user email")
    private String email;

    @Parameter(name = "user roles")
    private List<String> roles;

}
