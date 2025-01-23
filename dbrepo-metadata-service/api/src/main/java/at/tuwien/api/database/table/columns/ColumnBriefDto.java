package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

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
    @Schema(example = "1")
    private Long id;

    @NotNull
    @Schema(example = "2")
    @JsonProperty("database_id")
    private Long databaseId;

    @NotNull
    @Schema(example = "3")
    @JsonProperty("table_id")
    private Long tableId;

    @NotBlank
    @Size(max = 64)
    @Schema(example = "Given Name")
    private String name;

    @NotBlank
    @Size(max = 64)
    @JsonProperty("internal_name")
    @Schema(example = "given_name")
    private String internalName;

    @Schema(example = "firstname")
    private String alias;

    @NotNull
    @JsonProperty("type")
    @Schema(example = "varchar")
    private ColumnTypeDto columnType;

}
