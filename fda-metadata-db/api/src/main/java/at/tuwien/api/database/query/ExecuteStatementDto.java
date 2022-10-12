package at.tuwien.api.database.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ExecuteStatementDto {

    @NotBlank(message = "statement is required")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String statement;

}
