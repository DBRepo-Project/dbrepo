package at.tuwien.api.database;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

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
@Document(indexName = "view")
public class ViewDto {

    @NotNull
    private Long id;

    @NotNull
    @Field(name = "database_id")
    @JsonProperty("database_id")
    private Long vdbid;

    @NotNull
    @org.springframework.data.annotation.Transient
    private DatabaseDto database;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "air_quality")
    @Field(name = "air_quality")
    @JsonProperty("internal_name")
    private String internalName;

    @JsonProperty("is_public")
    @Field(name = "is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @JsonProperty("initial_view")
    @Field(name = "initial_view")
    @Schema(example = "true", description = "True if it is the default view for the database")
    private Boolean isInitialView;

    @NotNull
    @Schema(example = "SELECT `id` FROM `air_quality` ORDER BY `value` DESC")
    private String query;

    @NotNull
    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonIgnore
    @Field(name = "created_by")
    private UUID createdBy;

    @NotNull
    private UserDto creator;

    @NotNull(message = "columns are required")
    private List<ColumnDto> columns;

    @JsonProperty("last_modified")
    @org.springframework.data.annotation.Transient
    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
