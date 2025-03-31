package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ExecuteStatementDto {

    @NotBlank
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String statement;

}
