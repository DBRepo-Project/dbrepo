package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnStatisticDto;
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
public class TableStatisticDto {

    @JsonProperty("total_rows")
    @Schema(description = "The total number of rows", example = "5")
    private Long totalRows;

    @NotNull
    @JsonProperty("total_columns")
    @Schema(description = "The total number of columns", example = "2")
    private Long totalColumns;

    @JsonProperty("data_length")
    @Schema(description = "The data length in bytes", example = "16384")
    private Long dataLength;

    @JsonProperty("max_data_length")
    @Schema(description = "The maximum data length in bytes", example = "0")
    private Long maxDataLength;

    @JsonProperty("avg_row_length")
    @Schema(description = "The average data length in bytes", example = "3276")
    private Long avgRowLength;

    @NotNull
    private List<ColumnStatisticDto> columns;
}
