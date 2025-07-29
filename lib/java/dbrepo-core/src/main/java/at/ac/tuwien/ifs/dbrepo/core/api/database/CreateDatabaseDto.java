package at.ac.tuwien.ifs.dbrepo.core.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateDatabaseDto {

    @NotNull
    @JsonProperty("container_id")
    @Schema(description = "The container id", example = "0888e108-d521-46e2-9d3e-82099185305b")
    private UUID cid;

    @NotBlank
    @Schema(description = "The name", example = "Air Quality")
    private String name;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The visibility; if true, The will be displayed publicly and is searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The insights; if true, The schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

}
