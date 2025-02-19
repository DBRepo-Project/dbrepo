package at.tuwien.api.database;

import at.tuwien.api.CacheableDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ViewDto extends CacheableDto {

    @NotNull
    @Schema(example = "787439d0-e85e-400c-a7e6-996a023bfad9")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "2b5b2b03-fdd0-40d6-afe0-e5d02fd839e4")
    private UUID vdbid;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotNull
    private List<IdentifierDto> identifiers;

    @NotBlank
    @Schema(example = "air_quality")
    @JsonProperty("internal_name")
    private String internalName;

    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @JsonProperty("initial_view")
    @Schema(example = "true", description = "True if it is the default view for the database")
    private Boolean isInitialView;

    @NotNull
    @Schema(example = "SELECT `id` FROM `air_quality` ORDER BY `value` DESC")
    private String query;

    @NotNull
    @JsonProperty("query_hash")
    @Schema(example = "7de03e818900b6ea6d58ad0306d4a741d658c6df3d1964e89ed2395d8c7e7916")
    private String queryHash;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DatabaseDto database;

    @NotNull
    private UserBriefDto owner;

    @NotNull
    private List<ViewColumnDto> columns;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

}
