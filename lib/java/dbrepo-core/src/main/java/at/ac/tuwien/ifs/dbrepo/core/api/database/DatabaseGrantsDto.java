
package at.ac.tuwien.ifs.dbrepo.core.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseGrantsDto {

    @NotNull
    @Schema(example = "[\"SELECT\"]")
    private Set<String> grants;

    @Schema(example = "read")
    private GrantTypeDto type;

}
