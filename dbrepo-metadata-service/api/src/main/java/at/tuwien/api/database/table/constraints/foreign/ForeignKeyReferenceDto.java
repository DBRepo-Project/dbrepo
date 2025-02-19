package at.tuwien.api.database.table.constraints.foreign;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ForeignKeyReferenceDto {

    @Schema(example = "f2b740ec-0b13-4d07-88a9-529d354bba6a")
    private UUID id;

    @NotNull
    @JsonProperty("foreign_key")
    private ForeignKeyBriefDto foreignKey;

    @NotNull
    private ColumnBriefDto column;

    @NotNull
    @JsonProperty("referenced_column")
    private ColumnBriefDto referencedColumn;
}
