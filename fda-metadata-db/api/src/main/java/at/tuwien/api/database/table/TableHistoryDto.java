package at.tuwien.api.database.table;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant timestamp;

    @NotNull(message = "event name is required")
    private String event;

    @NotNull(message = "total number is required")
    @Schema(example = "1")
    private Long total;

}
