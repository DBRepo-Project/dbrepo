package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
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
    private String token;

    private String type;

    private Long id;

    @Schema(example = "user")
    private String username;

    @Schema(example = "user@example.com")
    private String email;

    private List<String> roles;

}
