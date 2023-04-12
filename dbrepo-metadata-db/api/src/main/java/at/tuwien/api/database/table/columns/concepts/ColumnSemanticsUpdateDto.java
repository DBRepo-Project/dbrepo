package at.tuwien.api.database.table.columns.concepts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnSemanticsUpdateDto {

    @JsonProperty("concept_uri")
    private String conceptUri;

    @JsonProperty("unit_uri")
    private String unitUri;
}
