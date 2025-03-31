package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnBriefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class PrimaryKeyDto {

    @Schema(example = "d984f9d7-e8a7-4b81-b59a-862db1871f18")
    private UUID id;

    @NotNull
    private TableBriefDto table;

    @NotNull
    private ColumnBriefDto column;
}
