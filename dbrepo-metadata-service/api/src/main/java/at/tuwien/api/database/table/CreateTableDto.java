package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.CreateTableColumnDto;
import at.tuwien.api.database.table.constraints.CreateTableConstraintsDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateTableDto {

    @NotBlank
    @Size(min = 1, max = 64)
    @Schema(example = "Air Quality")
    private String name;

    @Size(max = 180)
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    private List<CreateTableColumnDto> columns;

    @NotNull
    private CreateTableConstraintsDto constraints;
}
