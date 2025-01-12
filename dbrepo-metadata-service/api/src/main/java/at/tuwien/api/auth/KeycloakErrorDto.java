package at.tuwien.api.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class KeycloakErrorDto implements Serializable {

    @NotNull
    @Schema(example = "invalid_grant")
    private String error;

    @NotNull
    @JsonProperty("error_description")
    private String errorDescription;

    private String errorMessage;

}
