package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableCsvInformationDto {

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull
    private List<ColumnTypeDto> columns;

    @NotBlank
    @JsonProperty("file_location")
    private String fileLocation;

}
