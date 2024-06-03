package at.tuwien.api.database.table.constraints.primary;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.columns.ColumnBriefDto;
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

    private Long id;

    @NotNull
    @ToString.Exclude
    private TableBriefDto table;

    @NotNull
    @ToString.Exclude
    private ColumnBriefDto column;
}
