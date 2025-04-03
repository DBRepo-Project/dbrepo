package at.tuwien.api.doi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TimeRepresentationDto {

    @JsonProperty("date-parts")
    @Schema(example = "[[2025,1,18]]")
    private List<List<Integer>> dateParts;

    @JsonProperty("date-time")
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    private Instant dateTime;

    private Long timestamp;

}
