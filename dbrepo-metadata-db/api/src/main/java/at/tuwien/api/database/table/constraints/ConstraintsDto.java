package at.tuwien.api.database.table.constraints;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ConstraintsDto {

    List<List<ColumnDto>> uniques;

    @Field(name = "foreign_keys")
    @JsonProperty("foreign_keys")
    List<ForeignKeyDto> foreignKeys;

    List<String> checks;
}
