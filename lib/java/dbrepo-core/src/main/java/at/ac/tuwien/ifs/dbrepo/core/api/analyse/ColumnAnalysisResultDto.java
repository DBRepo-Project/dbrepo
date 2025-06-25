package at.ac.tuwien.ifs.dbrepo.core.api.analyse;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
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
public class ColumnAnalysisResultDto {

    @NotNull
    @Schema(example = "age")
    private String name;

    @NotNull
    @Schema(example = "BIGINT")
    private ColumnTypeDto datatype;

    @Schema(example = "20")
    private Integer size;

    @Schema(example = "10")
    private Integer d;

    @JsonProperty("null_allowed")
    @Schema(example = "true")
    private Boolean nullAllowed;

    private List<String> enums;

    private List<String> sets;

}
