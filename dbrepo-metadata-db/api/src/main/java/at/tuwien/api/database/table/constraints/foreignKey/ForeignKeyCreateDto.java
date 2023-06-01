package at.tuwien.api.database.table.constraints.foreignKey;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ForeignKeyCreateDto {

    private List<String> columns;

    @JsonProperty("referenced_table")
    private String referencedTable;

    @JsonProperty("referenced_columns")
    private List<String> referencedColumns;

    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;
}
