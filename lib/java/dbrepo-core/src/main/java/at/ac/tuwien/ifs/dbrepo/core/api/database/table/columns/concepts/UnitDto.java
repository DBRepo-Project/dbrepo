package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnBriefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UnitDto {

    @NotNull
    @Schema(description = "The unit id", example = "ba1935e8-6817-488f-af0a-f54389af9001")
    private UUID id;

    @NotBlank
    @Schema(description = "The unit URI", example = "http://www.ontology-of-units-of-measure.org/resource/om-2/CelsiusTemperature")
    private String uri;

    @Schema(description = "The unit name, taken as label from the ontology", example = "Degree Celsius")
    private String name;

    @Schema(description = "The unit description giving a user-friendly explanation")
    private String description;

    @NotNull
    private List<ColumnBriefDto> columns;
}
