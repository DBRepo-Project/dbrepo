package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts;

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
public class UnitSaveDto {

    @NotBlank
    @Schema(description = "The unit URI", example = "http://www.ontology-of-units-of-measure.org/resource/om-2/CelsiusTemperature")
    private String uri;

    @Schema(description = "The unit name, taken as label from the ontology", example = "Degree Celsius")
    private String name;

    @Schema(description = "The unit description giving a user-friendly explanation")
    private String description;

}
