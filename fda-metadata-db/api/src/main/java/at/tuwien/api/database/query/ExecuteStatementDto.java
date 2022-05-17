package at.tuwien.api.database.query;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;

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
}
