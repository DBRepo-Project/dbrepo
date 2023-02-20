package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnCreateDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCreateDto {

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull
    private List<ColumnCreateDto> columns;

}
