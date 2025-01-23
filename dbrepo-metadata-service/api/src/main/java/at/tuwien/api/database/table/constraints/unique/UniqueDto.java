package at.tuwien.api.database.table.constraints.unique;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnBriefDto;
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
public class UniqueDto {

    @NotNull
    @Schema(example = "5")
    private Long id;

    @NotNull
    @Schema(example = "uk_name")
    private String name;

    @NotNull
    private TableBriefDto table;

    @NotNull
    private List<ColumnBriefDto> columns;
}
