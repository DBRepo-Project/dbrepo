package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class SaveStatementDto {

    @NotBlank
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String statement;
}
