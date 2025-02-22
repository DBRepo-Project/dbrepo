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
public class ConceptDto {

    @NotNull
    @Schema(example = "8cabc011-4bdf-44d4-9d33-b2648e2ddbf1")
    private UUID id;

    @NotBlank
    private String uri;

    private String name;

    private String description;

    @NotNull
    private List<ColumnBriefDto> columns;
}
