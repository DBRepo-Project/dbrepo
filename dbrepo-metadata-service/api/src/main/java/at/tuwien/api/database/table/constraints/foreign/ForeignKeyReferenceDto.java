package at.tuwien.api.database.table.constraints.foreign;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ForeignKeyReferenceDto {

    private Long id;

    @NotNull
    @JsonProperty("foreign_key")
    private ForeignKeyBriefDto foreignKey;

    @NotNull
    private ColumnBriefDto column;

    @NotNull
    @JsonProperty("referenced_column")
    private ColumnBriefDto referencedColumn;
}
