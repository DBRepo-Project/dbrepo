package at.tuwien.api.database.table.constraints.foreignKey;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForeignKeyDto {

    private List<ColumnDto> columns;

    @JsonProperty("referenced_table")
    private TableBriefDto referencedTable;

    @JsonProperty("referenced_columns")
    private List<ColumnDto> referencedColumns;

    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;
}
