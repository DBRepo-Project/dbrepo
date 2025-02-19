package at.tuwien.api.database.table.columns.concepts;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UnitDto {

    @NotNull
    @Schema(example = "ba1935e8-6817-488f-af0a-f54389af9000")
    private UUID id;

    @NotBlank
    private String uri;

    private String name;

    private String description;

    @NotNull
    private List<ColumnBriefDto> columns;
}
