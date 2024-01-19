package at.tuwien.api.database.table.constraints;

import at.tuwien.api.database.table.constraints.foreignKey.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ConstraintsDto {

    @Field(name = "uniques", type = FieldType.Object)
    private List<UniqueDto> uniques;

    @JsonProperty("foreign_keys")
    @Field(name = "foreign_keys", type = FieldType.Object)
    private List<ForeignKeyDto> foreignKeys;

    @org.springframework.data.annotation.Transient
    private List<String> checks;
}
