package at.tuwien.api.database.query;

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

    @NotNull
    private List<Map<String, Object>> result;

    @NotNull
    private List<Map<String, Integer>> headers;

    @NotNull
    private Long id;

}
