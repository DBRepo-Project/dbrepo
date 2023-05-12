package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LoginRequestDto {

    @NotNull
    @Schema(example = "user")
    private String username;

    @NotNull
    @ToString.Exclude
    private String password;

}
