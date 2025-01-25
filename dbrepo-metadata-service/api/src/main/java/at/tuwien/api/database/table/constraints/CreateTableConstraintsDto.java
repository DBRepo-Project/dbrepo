package at.tuwien.api.database.table.constraints;

import at.tuwien.api.database.table.constraints.foreign.CreateForeignKeyDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateTableConstraintsDto {

    @NotNull
    private List<List<String>> uniques;

    @NotNull
    @JsonProperty("foreign_keys")
    private List<CreateForeignKeyDto> foreignKeys;

    @NotNull
    private Set<String> checks;

    @NotNull
    @JsonProperty("primary_key")
    private Set<String> primaryKey;

}
