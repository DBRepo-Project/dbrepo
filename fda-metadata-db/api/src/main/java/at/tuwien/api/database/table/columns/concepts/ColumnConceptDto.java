package at.tuwien.api.database.table.columns.concepts;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnConceptDto {

    @NotNull
    @ApiModelProperty(name = "id", example = "1", required = true)
    private Long id;

    @NotNull
    @ApiModelProperty(name = "tid", example = "1", required = true)
    private Long tid;

    @NotNull
    @ApiModelProperty(name = "cdbid", example = "1", required = true)
    private Long cdbid;

    @NotNull
    @ApiModelProperty(name = "concept", required = true)
    private ConceptDto concept;
}
