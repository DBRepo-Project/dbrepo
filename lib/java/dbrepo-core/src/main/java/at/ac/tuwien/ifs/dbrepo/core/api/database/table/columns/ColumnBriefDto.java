package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ColumnBriefDto {

    @NotNull
    @Schema(description = "The column id", example = "a453e444-e00d-41ca-902c-11e9c54b39f1")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The database id", example = "911f9052-c58c-4e1c-b3f2-66af2107be16")
    private UUID databaseId;

    @NotNull
    @JsonProperty("table_id")
    @Schema(description = "The table id", example = "bfffa915-a547-4466-9c65-ddc0d38fdb08")
    private UUID tableId;

    @NotBlank
    @Size(max = 64)
    @Schema(description = "The column name", example = "Given Name")
    private String name;

    @NotBlank
    @Size(max = 64)
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly internal name", example = "given_name")
    private String internalName;

    @Schema(description = "The data source alias name", example = "firstname")
    private String alias;

    @NotNull
    @JsonProperty("type")
    @Schema(example = "varchar")
    private ColumnTypeDto columnType;

}
