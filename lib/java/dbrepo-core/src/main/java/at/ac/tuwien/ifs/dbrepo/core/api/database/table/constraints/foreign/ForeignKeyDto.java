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

    @Schema(example = "f2b740ec-0b13-4d07-88a9-529d354bba6a")
    private UUID id;

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
