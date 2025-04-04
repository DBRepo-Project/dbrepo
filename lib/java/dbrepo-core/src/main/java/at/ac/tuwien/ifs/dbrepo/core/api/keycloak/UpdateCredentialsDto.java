package at.ac.tuwien.ifs.dbrepo.core.api.keycloak;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UpdateCredentialsDto {

    @NotNull
    private List<CredentialDto> credentials;

}
