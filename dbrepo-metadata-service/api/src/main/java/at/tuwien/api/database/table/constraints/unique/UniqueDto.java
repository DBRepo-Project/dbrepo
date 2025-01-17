
package at.tuwien.api.database.table.constraints.unique;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;

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
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private TableBriefDto table;

    @NotNull
    private List<ColumnDto> columns;
}
