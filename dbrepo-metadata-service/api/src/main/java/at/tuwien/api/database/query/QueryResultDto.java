package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class QueryResultDto {

    @NotNull(message = "result set is required")
    private List<Map<String, Object>> result;

    @NotNull(message = "headers is required")
    private List<Map<String, Integer>> headers;

    @NotNull(message = "query id is required")
    private Long id;

}
