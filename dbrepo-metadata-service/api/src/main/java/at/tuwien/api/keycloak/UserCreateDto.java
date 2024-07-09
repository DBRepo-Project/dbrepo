package at.tuwien.api.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class UserCreateDto {

    @NotNull
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @NotNull
    @Schema(example = "true")
    private Boolean enabled;

    @NotNull
    @Schema(example = "jcarberry@brown.edu")
    private String email;

    @NotNull
    private List<CredentialDto> credentials;

    private List<String> realmRoles;

    private List<String> groups;

}
