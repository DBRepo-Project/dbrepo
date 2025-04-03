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
    @Schema(example = "[\"id\"]")
    private List<String> columns;

    @NotNull
    @Schema(example = "sensor")
    @JsonProperty("referenced_table")
    private String referencedTable;

    @NotNull
    @Schema(example = "[\"other_id\"]")
    @JsonProperty("referenced_columns")
    private List<String> referencedColumns;

    @Schema(example = "cascade")
    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @Schema(example = "cascade")
    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;
}
