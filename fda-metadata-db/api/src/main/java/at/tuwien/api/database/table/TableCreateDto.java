package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnCreateDto;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCreateDto {

    @NotBlank
    @Parameter(name = "name", example = "Weather Australia")
    private String name;

    @NotBlank
    @Parameter(name = "table description", required = true, example = "Predict next-day rain in Australia")
    private String description;

    @NotNull
    @Parameter(name = "table columns", required = true)
    private List<ColumnCreateDto> columns;

}
