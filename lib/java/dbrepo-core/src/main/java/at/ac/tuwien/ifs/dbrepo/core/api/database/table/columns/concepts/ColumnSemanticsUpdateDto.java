package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts;

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
public class ColumnSemanticsUpdateDto {

    @JsonProperty("concept_uri")
    @Schema(description = "The URI of the concept", example = "http://www.ontology-of-units-of-measure.org/resource/om-2/CelsiusTemperature")
    private String conceptUri;

    @JsonProperty("unit_uri")
    @Schema(description = "The URI of the unit", example = "http://www.wikidata.org/entity/Q11466")
    private String unitUri;
}
