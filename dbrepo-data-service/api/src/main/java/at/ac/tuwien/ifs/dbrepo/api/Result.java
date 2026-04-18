package at.ac.tuwien.ifs.dbrepo.api;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class Result {

    @NotNull
    private Set<String> headers;

    @NotNull
    private List<Map<String, Object>> data;

}
