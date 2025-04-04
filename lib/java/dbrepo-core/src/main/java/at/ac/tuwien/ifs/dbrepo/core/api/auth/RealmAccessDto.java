package at.ac.tuwien.ifs.dbrepo.core.api.auth;

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
public class RealmAccessDto {

    @NotNull
    @Schema(description = "list of roles associated to the user", example = "[\"create-container\",\"create-database\"]")
    private String[] roles;

}
