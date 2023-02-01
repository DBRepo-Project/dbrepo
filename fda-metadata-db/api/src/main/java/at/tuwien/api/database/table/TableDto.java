package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @NotNull
    private UserBriefDto creator;

    @NotBlank
    @JsonProperty("queue_name")
    @Schema(example = "dbrepo/air_quality/air_quality")
    private String queueName;

    @NotBlank
    @JsonProperty("routing_key")
    @Schema(example = "dbrepo/air_quality/air_quality/1")
    private String routingKey;

    @NotBlank
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<ColumnDto> columns;

}
