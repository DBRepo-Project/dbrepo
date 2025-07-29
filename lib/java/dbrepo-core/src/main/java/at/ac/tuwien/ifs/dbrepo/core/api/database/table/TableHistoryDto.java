package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableHistoryDto {

    @NotNull
    @Schema(description = "The timestamp", example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant timestamp;

    @NotNull
    @Schema(description = "The event type", example = "INSERT")
    private HistoryEventTypeDto event;

    @NotNull
    @Schema(description = "The total number of rows affected by the event", example = "1")
    private Long total;

}
