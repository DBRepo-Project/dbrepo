package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ColumnBriefDto {

    @NotNull(message = "id is required")
    private Long id;

    @JsonProperty("database_id")
    @NotNull(message = "database id is required")
    private Long databaseId;

    @JsonProperty("table_id")
    @NotNull(message = "table id is required")
    private Long tableId;

    @NotBlank(message = "name is required")
    @Schema(example = "date")
    private String name;

    @NotBlank(message = "internal name is required")
    @JsonProperty("internal_name")
    @Schema(example = "mdb_date")
    private String internalName;

    @Schema
    private String alias;

    @NotNull
    @JsonProperty("column_type")
    @Schema(example = "date")
    private ColumnTypeDto columnType;

}
