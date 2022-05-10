package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnCreateDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCreateDto {

    @NotBlank
    @Parameter(name = "name", example = "Weather Australia")
    private String name;

    @NotBlank
    @Parameter(name = "table description", required = true, example = "Predict next-day rain in Australia")
    private String description;

    @Min(value = 0L)
    @JsonProperty("skip_lines")
    @Parameter(name = "number of lines to skip when importing", example = "0")
    private Long skipLines;

    @JsonProperty("false_element")
    @Parameter(name = "element denoting boolean false when importing", example = "0")
    private String falseElement;

    @JsonProperty("true_element")
    @Parameter(name = "element denoting boolean true when importing", example = "1")
    private String trueElement;

    @JsonProperty("null_element")
    @Parameter(name = "element denoting boolean null when importing", example = "NA")
    private String nullElement;

    @NotNull
    @Parameter(name = "csv separator when importing", required = true, example = ",")
    private Character separator;

    @Parameter(name = "csv quote character when importing", example = "\"")
    private Character quote;

    @NotNull
    @Parameter(name = "table columns", required = true)
    private ColumnCreateDto[] columns;

}
