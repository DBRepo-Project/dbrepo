package at.tuwien.api.database.table;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCsvDeleteDto {

    @NotNull(message = "primary key columns are required")
    private Map<String, Object> keys;

}
