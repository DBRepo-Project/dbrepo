package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ExecuteStatementDto {

    @NotBlank(message = "statement is required")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String statement;

    @Schema(description = "Execute query for data at this timestamp", example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant timestamp;

}
