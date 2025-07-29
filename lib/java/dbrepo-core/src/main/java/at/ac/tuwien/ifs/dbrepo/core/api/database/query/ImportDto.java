package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ImportDto {

    @NotBlank
    @Schema(description = "The key of the S3 binary object in the storage service", example = "file.csv")
    private String location;

    @NotNull
    @Schema(description = "If true, the first line contains the column names, otherwise it contains only data", example = "true")
    private Boolean header;

    @NotNull
    @Schema(description = "The column delimiter of the dataset", example = ",")
    private Character separator;

    @Schema(description = "The quote symbol around values in the dataset",example = "\"")
    private Character quote;

    @JsonProperty("line_termination")
    @Schema(description = "The newline symbol", example = "\\n")
    private String lineTermination;
}
