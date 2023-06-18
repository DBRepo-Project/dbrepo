package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

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

    @NotNull
    @Field(name = "database_id")
    @JsonProperty("database_id")
    private Long databaseId;

    @NotNull
    private Long id;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank(message = "internalName is required")
    @Field(name = "internal_name")
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @NotNull(message = "creator is required")
    private UserBriefDto creator;

    @NotNull(message = "owner is required")
    private UserBriefDto owner;

    @NotBlank(message = "queueName is required")
    @Field(name = "queue_name")
    @JsonProperty("queue_name")
    @Schema(example = "dbrepo/air_quality/air_quality")
    private String queueName;

    @NotBlank(message = "routingKey is required")
    @Field(name = "routing_key")
    @JsonProperty("routing_key")
    @Schema(example = "dbrepo/air_quality/air_quality/1")
    private String routingKey;

    @NotBlank(message = "description is required")
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull(message = "isPublic is required")
    @Field(name = "is_public")
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull(message = "columns are required")
    @org.springframework.data.annotation.Transient
    private List<ColumnDto> columns;

    private ConstraintsDto constraints;

}
