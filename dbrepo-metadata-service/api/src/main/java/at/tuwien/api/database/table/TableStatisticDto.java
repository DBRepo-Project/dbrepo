package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableStatisticDto {

    @NotNull
    @JsonProperty("rows")
    private Long rows;

    @NotNull
    private Map<String, ColumnStatisticDto> columns;
}
