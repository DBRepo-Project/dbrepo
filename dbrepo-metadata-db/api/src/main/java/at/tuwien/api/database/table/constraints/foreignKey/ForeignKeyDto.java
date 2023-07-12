package at.tuwien.api.database.table.constraints.foreignKey;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnDto;
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
public class ForeignKeyDto {

    private List<ColumnDto> columns;

    @Field(name = "referenced_table")
    @JsonProperty("referenced_table")
    private TableBriefDto referencedTable;

    @Field(name = "referenced_columns")
    @JsonProperty("referenced_columns")
    private List<ColumnDto> referencedColumns;

    @Field(name = "on_update")
    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @Field(name = "on_delete")
    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;
}
