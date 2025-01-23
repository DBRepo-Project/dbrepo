package at.tuwien.api.database.table;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TupleDeleteDto {

    @NotNull
    @Schema(example = "{\"id\": 1}")
    private Map<String, Object> keys;

}
