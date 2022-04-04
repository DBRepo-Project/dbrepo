package at.tuwien.api.database.table;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCsvDto {

    @NotNull(message = "data is required")
    @Parameter(name = "data")
    private Map<String, Object> data;

}
