package at.tuwien.api.database.table.constraints;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConstraintsDto {

    List<List<ColumnDto>> uniques;

    @JsonProperty("foreign_keys")
    List<ForeignKeyDto> foreignKeys;

    List<String> checks;
}
