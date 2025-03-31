package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String conceptUri;

    @JsonProperty("unit_uri")
    private String unitUri;
}
