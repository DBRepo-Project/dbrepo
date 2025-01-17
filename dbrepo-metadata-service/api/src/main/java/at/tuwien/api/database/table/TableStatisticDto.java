package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableStatisticDto {

    @JsonProperty("rows")
    @Schema(example = "5")
    private Long rows;

    @JsonProperty("data_length")
    @Schema(example = "16384", description = "in bytes")
    private Long dataLength;

    @JsonProperty("max_data_length")
    @Schema(example = "0", description = "in bytes")
    private Long maxDataLength;

    @JsonProperty("avg_row_length")
    @Schema(example = "3276", description = "in bytes")
    private Long avgRowLength;

    @NotNull
    private Map<String, ColumnStatisticDto> columns;
}
