package at.tuwien.api.database.table.constraints.foreign;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ForeignKeyDto {

    private Long id;

    @NotNull
    private String name;

    @NotNull
    private List<ForeignKeyReferenceDto> references;

    @NotNull
    private TableBriefDto table;

    @NotNull
    @JsonProperty("referenced_table")
    private TableBriefDto referencedTable;

    @JsonProperty("on_update")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    private ReferenceTypeDto onDelete;
}
