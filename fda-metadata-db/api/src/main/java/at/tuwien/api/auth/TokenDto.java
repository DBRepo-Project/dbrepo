package at.tuwien.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenDto {

    @NotBlank
    @JsonProperty("access_token")
    @Schema(example = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJBbFdvalRsa1dBSVVoYTJLWjFvOEluWEtNbVAzUTg0STZiMFFHYkR6aEpvIn0.eyJleHAiOjE2ODAyNjgyNjgsImlhdCI6MTY4MDI2ODIwOCwianRpIjoiNjkwNjRlNTQtODNhNS00NGYxLWE3OTItNWFjOWU4OTA5YTlkIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJzdWIiOiI4MjQ2OWMyMS0yYjNjLTRmMDctODg1Yi1hMzViMGQ5YTJhNjYiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJhZG1pbi1jbGkiLCJzZXNzaW9uX3N0YXRlIjoiNDQ1ODA3ZWUtMjg3Ni00NjFkLWE4ZjMtNGQyN2IzMGMyMWZhIiwiYWNyIjoiMSIsInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsInNpZCI6IjQ0NTgwN2VlLTI4NzYtNDYxZC1hOGYzLTRkMjdiMzBjMjFmYSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiZmRhIn0.IOQxqWvlPDV9WuFOeLVG-ayexbK8OqylPABghEMSbMpmNlQhSAjbjaMY31uU-uADZRHB-mC8bmRS5PoWNtanuhz0lORDCeissFsbv0UL9Q42CaxG75vFAAD5WsdIHIr-dtEjEiXYtu-qwdg83griAUeO119TTdgldyPxo4jWzNw0ui6W7r4LqP4fSk31iJfxR5urgs5k6Ctzg-fXCORT31-nKz_YJQwLoPO9j4afX_1mnCXY5qFGMSrmPKzB0CArZfUpa_4nqt4Y768yOC3gigAyCjXtvXKkgCmARPSRjERGDdTb6SGbAwRDiVHVy9wy7XZwOcCFMEra9H7mV0Mx2A")
    private String accessToken;

    @NotBlank
    @JsonProperty("refresh_token")
    @Schema(example = "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI4YTczZGYwZS03NzMwLTRiZDEtOGVhOC1mZTdjZWViNmMxYWMifQ.eyJleHAiOjE2ODAyNzAwMDgsImlhdCI6MTY4MDI2ODIwOCwianRpIjoiNWYyNDIwNDItNmJmZi00ZTQ2LTg2NTAtNDBhY2E3YjVkZjMyIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvcmVhbG1zL21hc3RlciIsInN1YiI6IjgyNDY5YzIxLTJiM2MtNGYwNy04ODViLWEzNWIwZDlhMmE2NiIsInR5cCI6IlJlZnJlc2giLCJhenAiOiJhZG1pbi1jbGkiLCJzZXNzaW9uX3N0YXRlIjoiNDQ1ODA3ZWUtMjg3Ni00NjFkLWE4ZjMtNGQyN2IzMGMyMWZhIiwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwic2lkIjoiNDQ1ODA3ZWUtMjg3Ni00NjFkLWE4ZjMtNGQyN2IzMGMyMWZhIn0.-GltWGkIaKUJ4AqRYnGHblTr0ygZm2CsRQB6zz5ePm4")
    private String refreshToken;

    @NotBlank
    @JsonProperty("token_type")
    @Schema(example = "Bearer")
    private String tokenType;

    @NotNull
    @JsonProperty("expires_in")
    @Schema(example = "60")
    private Long expiresIn;

    @NotNull
    @JsonProperty("session_state")
    @Schema(example = "445807ee-2876-461d-a8f3-4d27b30c21fa")
    private String sessionState;

    @NotNull
    @JsonProperty("scope")
    @Schema(example = "profile email")
    private String scope;

    @NotNull
    @JsonProperty("refresh_expires_in")
    @Schema(example = "1800")
    private Long refreshExpiresIn;

    @NotNull
    @JsonProperty("not-before-policy")
    @Schema(example = "0")
    private Long notBeforePolicy;

}
