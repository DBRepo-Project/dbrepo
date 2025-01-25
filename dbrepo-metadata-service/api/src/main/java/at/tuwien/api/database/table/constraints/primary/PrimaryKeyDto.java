package at.tuwien.api.database.table.constraints.primary;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnBriefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class PrimaryKeyDto {

    @Schema(example = "8")
    private Long id;

    @NotNull
    private TableBriefDto table;

    @NotNull
    private ColumnBriefDto column;
}
