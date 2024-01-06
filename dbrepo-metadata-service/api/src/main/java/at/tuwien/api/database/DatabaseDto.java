package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@Document(indexName = "database", writeTypeHint = WriteTypeHint.FALSE)
public class DatabaseDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    @Field(name = "name", type = FieldType.Keyword)
    private String name;

    @NotBlank
    @JsonProperty("exchange_name")
    @Schema(example = "dbrepo")
    @Field(name = "exchange_name", type = FieldType.Keyword)
    private String exchangeName;

    @JsonProperty("exchange_type")
    @Schema(example = "topic")
    @Field(name = "exchange_type", type = FieldType.Keyword)
    private String exchangeType;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    @Field(name = "internal_name", type = FieldType.Keyword)
    private String internalName;

    @Schema(example = "Air Quality")
    @Field(name = "description", type = FieldType.Text)
    private String description;

    @Field(name = "tables", type = FieldType.Nested)
    private List<TableDto> tables;

    @Field(name = "views", type = FieldType.Nested)
    private List<ViewDto> views;

    @JsonProperty("is_public")
    @Schema(example = "true")
    @Field(name = "is_public", type = FieldType.Boolean)
    private Boolean isPublic;

    @Field(name = "container", type = FieldType.Nested)
    private ContainerDto container;

    private List<DatabaseAccessDto> accesses;

    @Field(name = "identifiers", type = FieldType.Nested)
    private List<IdentifierDto> identifiers;

    @Field(name = "subsets", type = FieldType.Nested)
    private List<IdentifierDto> subsets;

    @NotNull
    private UserDto creator;

    @NotNull
    @Field(name = "contact", type = FieldType.Nested)
    private UserDto contact;

    @NotNull
    @Field(name = "owner", type = FieldType.Nested)
    private UserDto owner;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @Field(name = "created", type = FieldType.Date)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
