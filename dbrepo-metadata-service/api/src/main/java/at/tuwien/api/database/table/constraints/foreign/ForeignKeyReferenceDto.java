package at.tuwien.api.database.table.constraints.foreign;

import at.tuwien.api.database.table.columns.ColumnDto;
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
    private ForeignKeyDto foreignKey;

    @NotNull
    @ToString.Exclude
    private ColumnDto column;

    @NotNull
    @ToString.Exclude
    @JsonProperty("referenced_column")
    private ColumnDto referencedColumn;
}
