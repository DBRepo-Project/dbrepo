package at.tuwien.api.database.table;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableHistoryDto {

    @NotNull(message = "event timestamp is required")
    @Parameter(name = "event timestamp")
    private Instant timestamp;

    @NotNull(message = "event name is required")
    @Parameter(name = "event name")
    private String event;

    @NotNull(message = "total number is required")
    @Parameter(name = "total number")
    private Long total;

}
