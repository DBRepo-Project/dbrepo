package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnBriefDto {

    @NotNull(message = "id is required")
    private Long id;

    @NotBlank(message = "name is required")
    @Schema(example = "date")
    private String name;

    @NotBlank(message = "internal name is required")
    @JsonProperty("internal_name")
    @Schema(example = "mdb_date")
    private String internalName;

    @NotNull
    @JsonProperty("column_type")
    @Schema(example = "date")
    private ColumnTypeDto columnType;

}
