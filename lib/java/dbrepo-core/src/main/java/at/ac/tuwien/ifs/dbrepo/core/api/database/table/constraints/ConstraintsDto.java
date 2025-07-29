package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary.PrimaryKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ConstraintsDto {

    private List<UniqueDto> uniques;

    @JsonProperty("foreign_keys")
    private List<ForeignKeyDto> foreignKeys;

    @Schema(description = "The list of check constraints", example = "[]")
    private Set<String> checks;

    @JsonProperty("primary_key")
    private Set<PrimaryKeyDto> primaryKey;
}
