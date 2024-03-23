package at.tuwien.api.database.table.constraints;

import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyCreateDto;
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
public class ConstraintsCreateDto {

    private List<List<String>> uniques = null;

    @JsonProperty("foreign_keys")
    private List<ForeignKeyCreateDto> foreignKeys = null;

    private Set<String> checks = null;

}
