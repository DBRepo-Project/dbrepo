package at.tuwien.api.database.query;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnBriefDto;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ExecuteStatementDto {

    @NotBlank(message = "statement is required")
    @Parameter(name = "sql query")
    private String statement;

    @NotNull(message = "list of tables is required")
    @Parameter(name = "tables mentioned in the query")
    private List<TableBriefDto> tables;

    @NotNull(message = "list of columns is required")
    @Parameter(name = "columns mentioned in the query")
    private List<List<ColumnBriefDto>> columns;
}
