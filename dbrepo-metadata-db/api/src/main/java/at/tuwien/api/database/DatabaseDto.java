package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
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
@Document(indexName = "database")
public class DatabaseDto {

    @Id
    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("exchange_name")
    @Field(name = "exchange_name")
    @Schema(example = "dbrepo.air_quality")
    private String exchangeName;

    private IdentifierDto identifier;

    @NotBlank
    @JsonProperty("internal_name")
    @Field(name = "internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "Air Quality")
    private String description;

    private List<TableBriefDto> tables;

    private List<ViewBriefDto> views;

    @JsonProperty("is_public")
    @Field(name = "is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    private ImageDto image;

    private ContainerDto container;

    private List<DatabaseAccessDto> accesses;

    @NotNull
    private UserBriefDto creator;

    @NotNull
    private UserBriefDto owner;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @Field(type = FieldType.Date)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
