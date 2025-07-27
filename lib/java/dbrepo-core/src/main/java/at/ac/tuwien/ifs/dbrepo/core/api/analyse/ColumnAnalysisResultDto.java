package at.ac.tuwien.ifs.dbrepo.core.api.analyse;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ColumnAnalysisResultDto {

    @NotNull
    @Schema(description = "The column name", example = "age")
    private String name;

    @NotNull
    @Schema(description = "The column data type", example = "BIGINT")
    private ColumnTypeDto datatype;

    @Schema(description = "The size determines the number of digits before the comma: x=size-d where size >= d", example = "20")
    private Integer size;

    @Schema(description = "The digits behind the comma", example = "10")
    private Integer d;

    @JsonProperty("null_allowed")
    @Schema(description = "If set to true, the column value can be null", example = "true")
    private Boolean nullAllowed;

    @JsonProperty("primary_key")
    @Schema(description = "The column is a candidate to be part of a composite primary key", example = "true")
    private Boolean primaryKey;

    @Schema(description = "The list of enumerations detected", example = "\\[\\]")
    private List<String> enums;

    @Schema(description = "The list of set values detected", example = "\\[\\]")
    private List<String> sets;

}
