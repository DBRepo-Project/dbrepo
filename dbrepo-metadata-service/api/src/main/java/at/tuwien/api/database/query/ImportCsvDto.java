package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.Min;
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
public class ImportCsvDto {

    @NotBlank
    @Schema(example = "file.csv")
    private String location;

    @Min(value = 0L)
    @JsonProperty("skip_lines")
    private Long skipLines;

    @JsonProperty("false_element")
    private String falseElement;

    @JsonProperty("true_element")
    private String trueElement;

    @JsonProperty("null_element")
    @Schema(example = "NA")
    private String nullElement;

    @NotNull
    @Schema(example = ",")
    private Character separator;

    @Schema(example = "\"")
    private Character quote;

    @JsonProperty("line_termination")
    @Schema(example = "\\r\\n")
    private String lineTermination;
}
