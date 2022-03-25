package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnBriefDto {

    @NotNull(message = "id is required")
    @Parameter(name = "id", example = "1", required = true)
    private Long id;

    @NotBlank(message = "name is required")
    @Parameter(name = "name", example = "Date", required = true)
    private String name;

    @NotBlank(message = "internal name is required")
    @JsonProperty("internal_name")
    @Parameter(name = "internal name", example = "mdb_date", required = true)
    private String internalName;

}
