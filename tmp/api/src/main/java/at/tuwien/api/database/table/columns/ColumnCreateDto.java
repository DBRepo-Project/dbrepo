package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ColumnCreateDto {

    @NotBlank
    @Schema(example = "Date")
    private String name;

    @JsonProperty("index_length")
    private Long indexLength;

    @NotNull
    @Schema(example = "string")
    private ColumnTypeDto type;

    @Schema(example = "255")
    private Long size;

    @Schema(example = "0")
    private Long d;

    @NotNull
    @JsonProperty("null_allowed")
    @Schema(example = "true")
    private Boolean nullAllowed;

    @Schema(description = "date format id")
    private Long dfid;

    @Schema(description = "enum values, only considered when type = ENUM")
    private List<String> enums;

    @Schema(description = "set values, only considered when type = SET")
    private List<String> sets;

}
