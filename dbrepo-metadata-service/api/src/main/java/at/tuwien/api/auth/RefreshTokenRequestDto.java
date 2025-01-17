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
public class RefreshTokenRequestDto {

    @NotNull
    @JsonProperty("refresh_token")
    @Schema(example = "refresh_token")
    private String refreshToken;

}
