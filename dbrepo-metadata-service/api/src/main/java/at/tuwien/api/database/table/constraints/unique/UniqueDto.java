package at.tuwien.api.database.table.constraints.unique;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnBriefDto;
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
public class UniqueDto {

    @NotNull
    @Schema(example = "d984f9d7-e8a7-4b81-b59a-862db1871f13")
    private UUID id;

    @NotNull
    @Schema(example = "uk_name")
    private String name;

    @NotNull
    private TableBriefDto table;

    @NotNull
    private List<ColumnBriefDto> columns;
}
