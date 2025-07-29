package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TableBriefDto {

    @NotNull
    @Schema(description = "The id", example = "41ed10e0-687b-4e18-8521-810f5cffbce1")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The database id", example = "a8fec026-dfaf-4b1d-8f6c-f01720d91705")
    private UUID databaseId;

    @NotBlank
    @Schema(description = "The user-friendly table name", example = "Air Quality")
    private String name;

    @Size(max = 180)
    @Schema(description = "The comment", example = "Air Quality in Austria")
    private String description;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly table name", example = "air_quality")
    private String internalName;

    @NotNull
    @JsonProperty("is_versioned")
    @Schema(description = "If true, The is using data versioning", example = "true")
    private Boolean isVersioned;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The visibility; if true, The will be displayed publicly and is searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The insights; if true, The schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    @JsonProperty("owned_by")
    @Schema(description = "The owner id", example = "78337b80-5699-45db-8111-cec86439ab6b")
    private UUID ownedBy;
}
