package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.constraints.ConstraintsCreateDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableCreateDto {

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull
    private List<ColumnCreateDto> columns;

    private ConstraintsCreateDto constraints;
}
