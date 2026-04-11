package at.ac.tuwien.ifs.dbrepo.core.api.database;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateViewDto {

    @NotBlank
    @Size(min = 1, max = 63)
    @Schema(description = "The name", example = "Air Quality")
    private String name;

    @NotNull
    private SubsetDto query;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The visibility. If true, the data will be displayed publicly and will be searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_materialized")
    @Schema(description = "The behavior. If true, the view behaves like a table with persisted results.", example = "true")
    private Boolean isMaterialized;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The insights; If true, the schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

}
