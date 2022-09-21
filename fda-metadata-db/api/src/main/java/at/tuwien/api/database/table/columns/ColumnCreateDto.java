package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnCreateDto {

    @NotBlank
    @Schema(example = "Date")
    private String name;

    @NotNull
    @JsonProperty("primary_key")
    @Schema(example = "false")
    private Boolean primaryKey;

    @NotNull
    @Schema(example = "string")
    private ColumnTypeDto type;

    @NotNull
    @JsonProperty("null_allowed")
    @Schema(example = "true")
    private Boolean nullAllowed;

    @Schema(description = "date format id")
    private Long dfid;

    @NotNull
    @Schema(example = "false")
    private Boolean unique;

    @JsonProperty("check_expression")
    private String checkExpression;

    @JsonProperty("foreign_key")
    private String foreignKey = null;

    @Parameter(description = "foreign key reference, only considered when foreignKey != null")
    private String references = null;

    @JsonProperty("enum_values")
    @Parameter(description = "enum values, only considered when type = ENUM")
    private String[] enumValues = null;

}
