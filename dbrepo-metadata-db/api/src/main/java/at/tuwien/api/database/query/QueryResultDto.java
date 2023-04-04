package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private List<Map<String, Object>> result;

    @NotNull(message = "query id is required")
    private Long id;

    @Schema(example = "1")
    @JsonProperty("result_number")
    private Long resultNumber;

}
