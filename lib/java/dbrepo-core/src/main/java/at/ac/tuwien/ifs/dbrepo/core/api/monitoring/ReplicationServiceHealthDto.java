package at.ac.tuwien.ifs.dbrepo.core.api.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ReplicationServiceHealthDto {

    @Schema(description = "Logical name of the service", example = "metadata-service")
    private String name;

    @JsonProperty("status")
    @Schema(description = "Service health status (e.g. UP, DOWN)", example = "UP")
    private String status;

    @JsonProperty("http_status")
    @Schema(description = "HTTP status code returned by the service's health endpoint", example = "200")
    private Integer httpStatus;

    @JsonProperty("duration_ms")
    @Schema(description = "Time in milliseconds the health call took", example = "12")
    private Long durationMs;

    @JsonProperty("error")
    @Schema(description = "Optional error message if the health call failed", nullable = true)
    private String error;
}
