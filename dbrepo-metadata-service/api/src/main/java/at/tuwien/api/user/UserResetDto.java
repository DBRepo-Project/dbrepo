package at.tuwien.api.user;

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
public class UserResetDto {

    @NotNull
    private String password;

    @NotNull
    private String token;

}
