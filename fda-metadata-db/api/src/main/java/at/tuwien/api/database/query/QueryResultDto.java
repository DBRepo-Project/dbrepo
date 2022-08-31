package at.tuwien.api.database.query;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class QueryResultDto {

    @NotNull(message = "result set is required")
    @Parameter(name = "query result")
    private List<Map<String, Object>> result;

    @NotNull(message = "query id is required")
    @Parameter(name = "query id")
    private Long id;

    @Parameter(name = "result number")
    private Long resultNumber;

}
