package at.tuwien.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class KeycloakErrorDto {

    @NotNull
    @Schema(example = "invalid_grant")
    private String error;

    @NotNull
    @JsonProperty("error_description")
    private String errorDescription;

}
