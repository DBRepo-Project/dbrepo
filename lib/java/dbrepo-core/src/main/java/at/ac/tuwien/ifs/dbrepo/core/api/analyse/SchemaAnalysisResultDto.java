package at.ac.tuwien.ifs.dbrepo.core.api.analyse;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class SchemaAnalysisResultDto {

    @NotNull
    @Schema(description = "The column delimiter of the dataset", example = ",")
    private String delimiter;

    @NotNull
    @Schema(description = "The quote symbol around values in the dataset",example = "\"")
    private String quote;

    @NotNull
    @Schema(description = "The escape symbol", example = "\\")
    private String escape;

    @NotNull
    @JsonProperty("newline_delimiter")
    @Schema(description = "The newline symbol", example = "\\n")
    private String newlineDelimiter;

    @NotNull
    @Schema(description = "The comment symbol", example = "#")
    private String comment;

    @NotNull
    @JsonProperty("skip_rows")
    @Schema(description = "The number of rows to skip", example = "#")
    private Integer skipRows;

    @NotNull
    @JsonProperty("has_header")
    @Schema(description = "Detected the first line as header", example = "true")
    private Boolean hasHeader;

    @NotNull
    @Schema(description = "The list of columns analyzed", example = "[\"name\":\"VARCHAR\",\"age\":\"BIGINT\"]")
    private List<ColumnAnalysisResultDto> columns;

    @JsonProperty("date_format")
    @Schema(description = "The format of the date", example = "%d/%m/%Y")
    private String dateFormat;

    @JsonProperty("timestamp_format")
    @Schema(description = "The format of the timestamp", example = "%Y-%m-%dT%H:%M:%S.%f")
    private String timestampFormat;

    @JsonIgnore
    @Schema(description = "Prompt ready to be used to read the CSV", example = "FROM read_csv('my_file.csv', auto_detect=false, delim=',', ...)")
    private String prompt;


}
