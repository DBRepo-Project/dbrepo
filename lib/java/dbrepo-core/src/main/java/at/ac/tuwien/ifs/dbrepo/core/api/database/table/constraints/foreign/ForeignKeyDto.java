package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ForeignKeyDto {

    @Schema(description = "The foreign key id", example = "f2b740ec-0b13-4d07-88a9-529d354bba6a")
    private UUID id;

    @NotNull
    @Schema(description = "The foreign key name", example = "fk_name")
    private String name;

    @NotNull
    private List<ForeignKeyReferenceDto> references;

    @NotNull
    private TableBriefDto table;

    @NotNull
    @JsonProperty("referenced_table")
    private TableBriefDto referencedTable;

    @JsonProperty("on_update")
    @Schema(description = "The integrity action when updating tuples", example = "cascade")
    private ReferenceTypeDto onUpdate;

    @JsonProperty("on_delete")
    @Schema(description = "The integrity action when deleting tuples", example = "cascade")
    private ReferenceTypeDto onDelete;
}
