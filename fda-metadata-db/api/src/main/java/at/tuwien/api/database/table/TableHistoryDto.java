package at.tuwien.api.database.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableHistoryDto {

    @JsonProperty("inserted_at")
    @NotNull(message = "inserted timestamp is required")
    @Parameter(name = "inserted timestamp")
    private Instant insertedAt;

    @JsonProperty("deleted_at")
    @Parameter(name = "deleted timestamp")
    private Instant deletedAt;

    @NotNull(message = "primary key map is required")
    @Parameter(name = "primary key map")
    private Map<String, Object> keys;

}
