package at.tuwien.api.database.table;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

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
@Document(indexName = "table")
public class TableDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotNull
    @org.springframework.data.annotation.Transient
    private DatabaseDto database;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    @Field(name = "name", type = FieldType.Keyword)
    private String name;

    @NotBlank(message = "internalName is required")
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    @Field(name = "internal_name", type = FieldType.Keyword)
    private String internalName;

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
    @Field(name = "creator", includeInParent = true, type = FieldType.Nested)
    private UserDto creator;

    @NotNull(message = "owner is required")
    @Field(name = "owner", includeInParent = true, type = FieldType.Nested)
    private UserDto owner;

    @NotBlank(message = "queueName is required")
    @JsonProperty("queue_name")
    @Schema(example = "dbrepo.air_quality")
    @Field(name = "queue_name", type = FieldType.Keyword)
    private String queueName;

    @NotBlank(message = "routingKey is required")
    @JsonProperty("routing_key")
    @Schema(example = "dbrepo.air_quality")
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

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @Field(name = "created", type = FieldType.Keyword)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull(message = "columns are required")
    @org.springframework.data.annotation.Transient
    private List<ColumnDto> columns;

    @Field(name = "constraints", includeInParent = true, type = FieldType.Nested)
    private ConstraintsDto constraints;

}
