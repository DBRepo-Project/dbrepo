package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "tableindex", createIndex = false)
public class TableDto {

    @JsonIgnore
    private Long containerId;

    @JsonIgnore
    private Long databaseId;

    @NotNull
    private Long id;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank(message = "internalName is required")
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @NotNull(message = "creator is required")
    private UserBriefDto creator;

    @NotNull(message = "owner is required")
    private UserBriefDto owner;

    @NotBlank(message = "queueName is required")
    @JsonProperty("queue_name")
    @Schema(example = "dbrepo/air_quality/air_quality")
    private String queueName;

    @NotBlank(message = "routingKey is required")
    @JsonProperty("routing_key")
    @Schema(example = "dbrepo/air_quality/air_quality/1")
    private String routingKey;

    @NotBlank(message = "description is required")
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull(message = "isPublic is required")
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
