package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
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
    @Schema(description = "The table name", example = "Air Quality")
    private String name;

    @Size(max = 180)
    @Schema(description = "The table comment", example = "Air Quality in Austria")
    private String description;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The table visibility; if true, the table will be displayed publicly and is searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The table insights; if true, the table schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    private List<CreateTableColumnDto> columns;

    @NotNull
    private CreateTableConstraintsDto constraints;

    @Schema(description = "The creation location URL", example = "http://localhost:8080")
    private String creationLocation;

}
