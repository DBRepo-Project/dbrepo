package at.tuwien.api.database;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ViewDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotNull
    @Field(name = "database_id", type = FieldType.Keyword)
    @JsonProperty("database_id")
    private Long vdbid;

    @NotNull
    @org.springframework.data.annotation.Transient
    private DatabaseDto database;

    @NotBlank
    @Schema(example = "Air Quality")
    @Field(name = "name", type = FieldType.Keyword)
    private String name;

    @Field(name = "identifiers", type = FieldType.Object)
    private List<IdentifierDto> identifiers;

    @NotBlank
    @Schema(example = "air_quality")
    @Field(name = "internal_name", type = FieldType.Keyword)
    @JsonProperty("internal_name")
    private String internalName;

    @JsonProperty("is_public")
    @Field(name = "is_public", type = FieldType.Boolean)
    @Schema(example = "true")
    private Boolean isPublic;

    @JsonProperty("initial_view")
    @Field(name = "initial_view", type = FieldType.Boolean)
    @Schema(example = "true", description = "True if it is the default view for the database")
    private Boolean isInitialView;

    @NotNull
    @Schema(example = "SELECT `id` FROM `air_quality` ORDER BY `value` DESC")
    @Field(name = "query", type = FieldType.Text)
    private String query;

    @NotNull
    @JsonProperty("query_hash")
    @Schema(example = "7de03e818900b6ea6d58ad0306d4a741d658c6df3d1964e89ed2395d8c7e7916")
    @Field(name = "query_hash", type = FieldType.Keyword)
    private String queryHash;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @Field(name = "created", type = FieldType.Date)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonIgnore
    @org.springframework.data.annotation.Transient
    private UUID createdBy;

    @NotNull
    @org.springframework.data.annotation.Transient
    private UserDto creator;

    @NotNull(message = "columns are required")
    @org.springframework.data.annotation.Transient
    private List<ColumnDto> columns;

    @JsonProperty("last_modified")
    @org.springframework.data.annotation.Transient
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
