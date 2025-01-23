package at.tuwien.api.database;

import at.tuwien.api.CacheableDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

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
    @Schema(example = "4")
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1")
    private Long vdbid;

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

    @NotNull
    private UserBriefDto owner;

    @NotNull
    private List<ViewColumnDto> columns;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

    @ToString.Exclude
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @ToString.Exclude
    @Schema(example = "data-db")
    private String host;

    @ToString.Exclude
    @Schema(example = "3306")
    private Integer port;

    @ToString.Exclude
    @Schema(example = "username")
    private String username;

    @ToString.Exclude
    @JsonIgnore
    private String password;

    @ToString.Exclude
    @Schema(example = "air_quality")
    private String database;

}
