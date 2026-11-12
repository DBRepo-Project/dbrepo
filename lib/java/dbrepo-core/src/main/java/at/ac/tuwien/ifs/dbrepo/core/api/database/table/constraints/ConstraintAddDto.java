package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ReferenceTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ConstraintAddDto {

    @NotBlank
    private String type;

    private List<String> columns;

    @JsonProperty("referenced_table")
    private String referencedTable;

    @JsonProperty("referenced_columns")
    private List<String> referencedColumns;

    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;

    private String check;

    private String column;

}
