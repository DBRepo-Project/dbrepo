package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserDto {

    @NotNull
    @Schema(example = "true")
    private Boolean enabled;

    @NotBlank
    @Schema(example = "user")
    private String username;

    @NotBlank
    @Email
    @Schema(example = "user@example.com")
    private String email;

    private String firstName;

    private String lastName;

    @NotNull
    private List<CredentialDto> credentials;

}
