package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnStatisticDto;
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
    private Map<String, ColumnStatisticDto> columns;
}
