package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateTableColumnDto {

    @NotBlank
    @Schema(description = "The column name", example = "id")
    private String name;

    @JsonProperty("index_length")
    @Schema(description = "The index length", example = "null")
    private Long indexLength;

    @NotNull
    @Schema(description = "The data type", example = "serial")
    private ColumnTypeDto type;

    @Schema(description = "The size determines the number of digits before the comma: x=size-d where size >= d", example = "null")
    private Long size;

    @Schema(description = "The digits behind the comma", example = "null")
    private Long d;

    @NotNull
    @JsonProperty("null_allowed")
    @Schema(description = "If set to true, the column value can be null", example = "true")
    private Boolean nullAllowed;

    @Size(max = 2048)
    @Schema(description = "The column comment", example = "null")
    private String description;

    @JsonProperty("concept_uri")
    @Schema(description = "The column concept", example = "null")
    private String conceptUri;

    @JsonProperty("unit_uri")
    @Schema(description = "The column unit", example = "null")
    private String unitUri;

    @Schema(description = "enum values, only considered when type = ENUM", example = "null")
    private List<String> enums;

    @Schema(description = "set values, only considered when type = SET", example = "nul")
    private List<String> sets;

}
