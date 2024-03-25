package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TableDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    @Field(name = "database_id", type = FieldType.Keyword)
    private Long tdbid;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    @Field(name = "name", type = FieldType.Keyword)
    private String name;

    @NotBlank(message = "internalName is required")
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    @Field(name = "internal_name", type = FieldType.Keyword)
    private String internalName;

    @Field(name = "identifiers", type = FieldType.Object)
    private List<IdentifierDto> identifiers;

    @NotNull
    @JsonProperty("is_versioned")
    @Schema(example = "true")
    @Field(name = "is_versioned", type = FieldType.Boolean)
    private Boolean isVersioned;

    @NotNull
    @JsonProperty("created_by")
    @org.springframework.data.annotation.Transient
    private UUID createdBy;

    @NotNull(message = "creator is required")
    @org.springframework.data.annotation.Transient
    private UserDto creator;

    @NotNull(message = "owner is required")
    @Field(name = "owner", type = FieldType.Object)
    private UserDto owner;

    @NotBlank(message = "queueName is required")
    @JsonProperty("queue_name")
    @Schema(example = "air_quality")
    @Field(name = "queue_name", type = FieldType.Keyword)
    private String queueName;

    @JsonProperty("queue_type")
    @Schema(example = "quorum")
    @Field(name = "queue_type", type = FieldType.Keyword)
    private String queueType;

    @NotBlank(message = "routingKey is required")
    @JsonProperty("routing_key")
    @Schema(example = "dbrepo.database.air_quality")
    @Field(name = "routing_key", type = FieldType.Keyword)
    private String routingKey;

    @Schema(example = "Air Quality in Austria")
    @Field(name = "description", type = FieldType.Text)
    private String description;

    @NotNull(message = "isPublic is required")
    @JsonProperty("is_public")
    @Schema(example = "true")
    @Field(name = "is_public", type = FieldType.Boolean)
    private Boolean isPublic;

    @JsonProperty("num_rows")
    @Schema(example = "5")
    @Field(name = "num_rows", type = FieldType.Long)
    private Long numRows;

    @JsonProperty("data_length")
    @Schema(example = "16384", description = "in bytes")
    @Field(name = "data_length", type = FieldType.Long)
    private Long dataLength;

    @JsonProperty("max_data_length")
    @Schema(example = "0", description = "in bytes")
    @Field(name = "max_data_length", type = FieldType.Long)
    private Long maxDataLength;

    @JsonProperty("avg_row_length")
    @Schema(example = "3276", description = "in bytes")
    @Field(name = "avg_row_length", type = FieldType.Long)
    private Long avgRowLength;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @Field(name = "created", type = FieldType.Date)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull(message = "columns are required")
    @Field(name = "columns", type = FieldType.Object)
    private List<ColumnDto> columns;

    @NotNull
    @Field(name = "constraints", type = FieldType.Object)
    private ConstraintsDto constraints;

}
