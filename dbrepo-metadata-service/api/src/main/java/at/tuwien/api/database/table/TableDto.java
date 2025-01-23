package at.tuwien.api.database.table;

import at.tuwien.api.CacheableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TableDto extends CacheableDto {

    @NotNull
    @Schema(example = "3")
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "2")
    private Long tdbid;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "a")
    private String alias;

    private List<IdentifierDto> identifiers;

    @NotNull
    @JsonProperty("is_versioned")
    @Schema(example = "true")
    private Boolean isVersioned;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    private UserBriefDto owner;

    @NotBlank
    @JsonProperty("queue_name")
    @Schema(example = "air_quality")
    private String queueName;

    @JsonProperty("queue_type")
    @Schema(example = "quorum")
    private String queueType;

    @NotBlank
    @JsonProperty("routing_key")
    @Schema(example = "dbrepo.1.2")
    private String routingKey;

    @Size(max = 2048)
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @JsonProperty("num_rows")
    @Schema(example = "5")
    private Long numRows;

    @JsonProperty("data_length")
    @Schema(example = "16384", description = "in bytes")
    private Long dataLength;

    @JsonProperty("max_data_length")
    @Schema(example = "0", description = "in bytes")
    private Long maxDataLength;

    @JsonProperty("avg_row_length")
    @Schema(example = "3276", description = "in bytes")
    private Long avgRowLength;

    @NotNull
    private List<ColumnDto> columns;

    @NotNull
    private ConstraintsDto constraints;

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
