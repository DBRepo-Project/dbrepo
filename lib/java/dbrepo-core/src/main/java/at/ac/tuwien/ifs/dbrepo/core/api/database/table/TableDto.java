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
    @Schema(example = "d346f844-b84c-490f-9aec-725a2dc8f820")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "fc29f89c-86a8-4020-9e36-4d954736c6cc")
    private UUID databaseId;

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

    @NotNull
    @Schema(example = "2022-01-01 08:00:00.000")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant created;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

}
