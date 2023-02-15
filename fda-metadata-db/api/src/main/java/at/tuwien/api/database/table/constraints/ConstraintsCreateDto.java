package at.tuwien.api.database.table.constraints;

import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyCreateDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConstraintsCreateDto {

    private List<List<String>> uniques = null;

    @JsonProperty("foreign_keys")
    private List<ForeignKeyCreateDto> foreignKeys = null;

    private List<String> checks = null;

}
