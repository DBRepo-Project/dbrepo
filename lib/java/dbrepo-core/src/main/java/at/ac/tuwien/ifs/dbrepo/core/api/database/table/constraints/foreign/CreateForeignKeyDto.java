package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class CreateForeignKeyDto {

    @NotNull
    @Schema(description = "The list of local columns that reference a foreign set of columns", example = "[\"id\"]")
    private List<String> columns;

    @NotNull
    @Schema(description = "The name of the foreign table", example = "sensor")
    @JsonProperty("referenced_table")
    private String referencedTable;

    @NotNull
    @Schema(description = "The list of foreign columns", example = "[\"other_id\"]")
    @JsonProperty("referenced_columns")
    private List<String> referencedColumns;

    @JsonProperty("on_update")
    @Schema(description = "The integrity action when updating tuples", example = "cascade")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    @Schema(description = "The integrity action when deleting tuples", example = "cascade")
    private ReferenceTypeDto onDelete;
}
