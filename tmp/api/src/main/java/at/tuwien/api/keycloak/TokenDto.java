package at.tuwien.api.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TokenDto {

    @NotNull
    @JsonProperty("access_token")
    private String accessToken;

    @NotNull
    @JsonProperty("expires_in")
    private Long expiresIn;

    @NotNull
    @JsonProperty("refresh_token")
    private String refreshToken;

    @NotNull
    @JsonProperty("refresh_expires_in")
    private Long refreshExpiresIn;

    @NotNull
    @JsonProperty("id_token")
    private String idToken;

    @NotNull
    @JsonProperty("session_state")
    private String sessionState;

    @NotNull
    private String scope;

    @NotNull
    @JsonProperty("token_type")
    private String tokenType;

    @NotNull
    @JsonProperty("not-before-policy")
    private Long notBeforePolicy;

}
