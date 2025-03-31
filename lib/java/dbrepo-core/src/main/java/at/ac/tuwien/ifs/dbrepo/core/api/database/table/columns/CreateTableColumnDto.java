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
    @Schema(example = "Date")
    private String name;

    @JsonProperty("index_length")
    private Long indexLength;

    @NotNull
    @Schema(example = "varchar")
    private ColumnTypeDto type;

    @Schema(example = "255")
    private Long size;

    @Schema(example = "0")
    private Long d;

    @Size(max = 2048)
    @Schema(example = "Formatted as YYYY-MM-dd")
    private String description;

    @NotNull
    @JsonProperty("null_allowed")
    @Schema(example = "true")
    private Boolean nullAllowed;

    @JsonProperty("concept_uri")
    private String conceptUri;

    @JsonProperty("unit_uri")
    private String unitUri;

    @Schema(description = "enum values, only considered when type = ENUM")
    private List<String> enums;

    @Schema(description = "set values, only considered when type = SET")
    private List<String> sets;

}
