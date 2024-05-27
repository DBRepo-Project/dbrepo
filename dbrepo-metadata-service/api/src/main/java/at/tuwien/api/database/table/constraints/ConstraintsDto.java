package at.tuwien.api.database.table.constraints;

import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    private Set<String> checks;

    @JsonProperty("primary_key")
    private Set<PrimaryKeyDto> primaryKey;
}
