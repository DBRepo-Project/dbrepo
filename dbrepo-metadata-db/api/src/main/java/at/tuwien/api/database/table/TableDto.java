package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

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
    private Long id;

    @Id
    @NotNull
    @Field(name = "container_id")
    @JsonProperty("container_id")
    private Long containerId;

    @Id
    @NotNull
    @Field(name = "database_id")
    @JsonProperty("database_id")
    private Long databaseId;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank(message = "internalName is required")
    @JsonProperty("internal_name")
    @Field(name = "internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @NotNull(message = "creator is required")
    private UserBriefDto creator;

    @NotNull(message = "owner is required")
    private UserBriefDto owner;

    @NotBlank(message = "queueName is required")
    @JsonProperty("queue_name")
    @Field(name = "queue_name")
    @Schema(example = "dbrepo.air_quality")
    private String queueName;

    @NotBlank(message = "routingKey is required")
    @JsonProperty("routing_key")
    @Field(name = "routing_key")
    @Schema(example = "dbrepo.air_quality")
    private String routingKey;

    @NotBlank(message = "description is required")
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull(message = "isPublic is required")
    @JsonProperty("is_public")
    @Field(name = "is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @Field(type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull(message = "columns are required")
    private List<ColumnDto> columns;

    private ConstraintsDto constraints;

}
