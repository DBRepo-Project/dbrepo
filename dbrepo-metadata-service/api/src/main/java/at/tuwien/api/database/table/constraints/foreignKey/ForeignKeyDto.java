package at.tuwien.api.database.table.constraints.foreignKey;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnDto;
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
public class ForeignKeyDto {

    private String name;

    @org.springframework.data.annotation.Transient
    private List<ColumnDto> columns;

    @JsonProperty("referenced_table")
    @org.springframework.data.annotation.Transient
    private TableBriefDto referencedTable;

    @JsonProperty("referenced_columns")
    @org.springframework.data.annotation.Transient
    private List<ColumnDto> referencedColumns;

    @JsonProperty("on_update")
    @Field(name = "on_update", type = FieldType.Keyword)
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    @Field(name = "on_delete", type = FieldType.Keyword)
    private ReferenceTypeDto onDelete;
}
