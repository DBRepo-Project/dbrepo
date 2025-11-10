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
public class ViewBriefDto {

    @NotNull
    @Schema(description = "The id", example = "787439d0-e85e-400c-a7e6-996a023bfad9")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The database id", example = "2b5b2b03-fdd0-40d6-afe0-e5d02fd839e4")
    private UUID vdbid;

    @NotBlank
    @Schema(description = "The user-friendly name", example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly name", example = "air_quality")
    private String internalName;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The visibility; if true, The will be displayed publicly and is searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The insights; if true, The schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

    @JsonProperty("initial_view")
    @Schema(description = "If true, the view is default for the database", example = "true")
    private Boolean isInitialView;

    @NotNull
    @Schema(description = "The SQL statement used to create the view", example = "SELECT `id` FROM `air_quality` ORDER BY `value` DESC")
    private String query;

    @NotNull
    @JsonProperty("query_hash")
    @Schema(description = "The sha256-hash of the query", example = "7de03e818900b6ea6d58ad0306d4a741d658c6df3d1964e89ed2395d8c7e7916")
    private String queryHash;

    @JsonProperty("owned_by")
    @Schema(description = "The owner username", example = "foobar")
    private String ownedBy;

}
