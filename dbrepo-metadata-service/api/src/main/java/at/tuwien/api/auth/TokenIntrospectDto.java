package at.tuwien.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TokenIntrospectDto {

    @NotNull
    @Schema(description = "expiration timestamp", example = "1679602372")
    private Long exp;

    @NotNull
    @Schema(example = "1679602072")
    private Long iat;

    @NotNull
    @Schema(example = "6aa375aa-d5bb-4b1e-9f89-347084a739e3")
    private String jti;

    @NotNull
    @Schema(description = "issuer", example = "6aa375aa-d5bb-4b1e-9f89-347084a739e3")
    private String iss;

    @NotNull
    @Schema(description = "user id", example = "9670828b-8159-4642-be19-e77ca018e644")
    private String sub;

    @NotNull
    @Schema(description = "type", example = "Bearer")
    private String typ;

    @NotNull
    @Schema(example = "0170887f-4ffc-4bb7-9292-9334132cd430")
    private String azp;

    @NotNull
    @Schema(example = "0170887f-4ffc-4bb7-9292-9334132cd430")
    @JsonProperty("session_state")
    private String sessionState;

    @NotNull
    @Schema(example = "1")
    private Integer acr;

    @NotNull
    @JsonProperty("allowed-origins")
    @Schema(example = "[\"*\"]")
    private String[] allowedOrigins;

    @NotNull
    @JsonProperty("realm_access")
    private RealmAccessDto realmAccess;

    @NotNull
    @JsonProperty("client_id")
    @Schema(example = "dbrepo-client")
    private String clientId;

    @NotNull
    @JsonProperty("preferred_username")
    @Schema(example = "jdoe")
    private String username;

    @NotNull
    @Schema(example = "openid email profile")
    private String scope;

    @NotNull
    @Schema(example = "true")
    private Boolean active;

}
