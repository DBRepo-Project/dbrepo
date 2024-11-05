package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

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
public class ImportDto {

    @NotBlank(message = "location is required")
    @Schema(example = "file.csv")
    private String location;

    @NotNull
    @Schema(description = "If true, the first line contains the column names, otherwise it contains only data")
    private Boolean header;

    @NotNull
    @Schema(example = ",")
    private Character separator;

    @Schema(example = "\"")
    private Character quote;

    @JsonProperty("line_termination")
    @Schema(example = "\\r\\n")
    private String lineTermination;
}
