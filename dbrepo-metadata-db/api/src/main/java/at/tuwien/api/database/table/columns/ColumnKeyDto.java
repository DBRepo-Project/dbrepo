package at.tuwien.api.database.table.columns;

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
public class ColumnKeyDto {

    @NotNull
    private Long containerId;

    @NotNull
    private Long databaseId;

    @NotNull
    private Long tableId;

    @NotNull
    private Long id;
}
