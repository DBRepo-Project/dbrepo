package at.tuwien.api.database.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCsvUpdateDto {

    @NotNull(message = "data is required")
    @Parameter(name = "data")
    private Map<String, Object> data;

    @NotNull(message = "primary key columns are required")
    @Parameter(name = "keys")
    private Map<String, Object> keys;

}
