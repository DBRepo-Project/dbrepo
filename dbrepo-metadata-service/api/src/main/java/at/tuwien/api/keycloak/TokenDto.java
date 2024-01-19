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
    private String scope;

}
