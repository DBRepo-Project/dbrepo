package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import at.ac.tuwien.ifs.dbrepo.core.api.CacheableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableDto extends CacheableDto {

    @NotNull
    @Schema(description = "The id", example = "41ed10e0-687b-4e18-8521-810f5cffbce1")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The database id", example = "a8fec026-dfaf-4b1d-8f6c-f01720d91705")
    private UUID databaseId;

    @NotBlank
    @Schema(description = "The user-friendly name", example = "Air Quality")
    private String name;

    @Size(max = 180)
    @Schema(description = "The comment", example = "Air Quality in Austria")
    private String description;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly name", example = "air_quality")
    private String internalName;

    @Schema(description = "The alias", example = "a")
    private String alias;

    @Schema(description = "The list of identifiers", example = "[]")
    private List<IdentifierDto> identifiers;

    @NotNull
    @JsonProperty("is_versioned")
    @Schema(description = "If true, The is using data versioning", example = "true")
    private Boolean isVersioned;

    @NotNull
    private UserBriefDto owner;

    @NotBlank
    @JsonProperty("queue_name")
    @Schema(description = "The queue name", example = "dbrepo")
    private String queueName;

    @JsonProperty("queue_type")
    @Schema(description = "The queue type", example = "quorum")
    private String queueType;

    @NotBlank
    @JsonProperty("routing_key")
    @Schema(description = "The routing key", example = "dbrepo.a8fec026-dfaf-4b1d-8f6c-f01720d91705.41ed10e0-687b-4e18-8521-810f5cffbce1")
    private String routingKey;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The visibility; if true, The will be displayed publicly and is searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The insights; if true, The schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

    @JsonProperty("num_rows")
    @Schema(description = "The statistical number of rows", example = "5")
    private Long numRows;

    @JsonProperty("data_length")
    @Schema(description = "The data length in bytes", example = "16384")
    private Long dataLength;

    @JsonProperty("max_data_length")
    @Schema(description = "The maximum data length in bytes", example = "0")
    private Long maxDataLength;

    @JsonProperty("avg_row_length")
    @Schema(description = "The average row length in bytes", example = "3276")
    private Long avgRowLength;

    @NotNull
    private List<ColumnDto> columns;

    @NotNull
    private ConstraintsDto constraints;

    @JsonProperty("replica_urls")
    @Schema(description = "Map of replica URL to replica table ID", example = "{\"https://replica1.example.com\": \"41ed10e0-687b-4e18-8521-810f5cffbce1\"}", nullable = true)
    private Map<String, UUID> replicaUrls;

    @JsonProperty("creation_location")
    @Schema(description = "The creation location URL", example = "http://localhost:8080", nullable = true)
    private String creationLocation;

    @NotNull
    @Schema(description = "The created timestamp", example = "2022-01-01 08:00:00.000")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant created;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(description = "The timestamp The was last retrieved from the cache", example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

}
