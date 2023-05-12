package at.tuwien.api.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CredentialDto {

    @NotBlank
    @Schema(example = "password")
    private String type;

    @NotBlank
    @Schema(example = "abc123")
    private String value;

    @NotNull
    @Schema(example = "false")
    private Boolean temporary;

}
