package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableDto {

    @NotNull
    @Parameter(name = "table id", example = "1")
    private Long id;

    @NotBlank
    @Parameter(name = "table name", example = "Weather Australia")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Parameter(name = "table internal name", example = "weather_australia")
    private String internalName;

    @NotBlank
    @Parameter(name = "topic name", example = "fda.c1.d1.t1")
    private String topic;

    @NotBlank
    @Parameter(name = "table description", example = "Predict next-day rain in Australia")
    private String description;

    @NotNull
    @Parameter(name = "table csv separator", example = ",")
    private Character separator = ',';

    @NotNull
    @Parameter(name = "csv quote character when importing", required = true, example = "\"")
    private Character quote;

    @NotBlank
    @JsonProperty("null_element")
    @Parameter(name = "table csv null element", example = "NA")
    private String nullElement = null;

    @JsonProperty("skip_lines")
    @Parameter(name = "table csv contains a header row", example = "0")
    private Long skipLines = 0L;

    @JsonProperty("true_element")
    @Parameter(name = "table csv element for boolean true", example = "1")
    private String trueElement = "1";

    @JsonProperty("false_element")
    @Parameter(name = "table csv element for boolean false", example = "0")
    private String falseElement = "0";

    @Parameter(name = "table creation time")
    private Instant created;

    @NotNull
    @Parameter(name = "table columns")
    private ColumnDto[] columns;

}
