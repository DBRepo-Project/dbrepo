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
    @Schema(example = "file.csv")
    private String location;

    @NotNull
    @Schema(example = "true", description = "If true, the first line contains the column names, otherwise it contains only data")
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
