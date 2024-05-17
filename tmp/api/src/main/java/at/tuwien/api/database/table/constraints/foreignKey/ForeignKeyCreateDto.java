package at.tuwien.api.database.table.constraints.foreignKey;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    private List<String> columns;

    @NotNull
    @JsonProperty("referenced_table")
    private String referencedTable;

    @NotNull
    @JsonProperty("referenced_columns")
    private List<String> referencedColumns;

    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;
}
