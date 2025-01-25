package at.tuwien.api.database.table.constraints.foreign;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(example = "4")
    private Long id;

    @NotNull
    @Schema(example = "fk_name")
    private String name;

    @NotNull
    private List<ForeignKeyReferenceDto> references;

    @NotNull
    private TableBriefDto table;

    @NotNull
    @JsonProperty("referenced_table")
    private TableBriefDto referencedTable;

    @JsonProperty("on_update")
    @Schema(example = "restrict")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    @Schema(example = "restrict")
    private ReferenceTypeDto onDelete;
}
