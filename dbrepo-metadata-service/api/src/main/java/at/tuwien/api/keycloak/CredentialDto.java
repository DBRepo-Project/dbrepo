package at.tuwien.api.keycloak;

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
public class CredentialDto {

    @NotNull
    private CredentialTypeDto type;

    @Schema(example = "s3cr3t")
    private String value;

    @Schema(example = "false")
    private Boolean temporary;

}
