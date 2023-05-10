package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
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
