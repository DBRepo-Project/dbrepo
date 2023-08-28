package at.tuwien.api.database.table;

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
public class TableKeyDto {

    @NotNull
    private Long containerId;

    @NotNull
    private Long databaseId;

    @NotNull
    private Long id;
}
