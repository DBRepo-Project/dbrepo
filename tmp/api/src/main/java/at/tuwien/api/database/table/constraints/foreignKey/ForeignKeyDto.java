package at.tuwien.api.database.table.constraints.foreignKey;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnDto;
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
public class ForeignKeyDto {

    @NonNull
    private String name;

    @NonNull
    private List<ColumnDto> columns;

    @NonNull
    @JsonProperty("referenced_table")
    private TableBriefDto referencedTable;

    @NonNull
    @JsonProperty("referenced_columns")
    private List<ColumnDto> referencedColumns;

    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;
}
