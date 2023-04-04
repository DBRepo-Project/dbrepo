package at.tuwien.api.database.table;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCsvDto {

    @NotNull(message = "data is required")
    private Map<String, Object> data;

}
