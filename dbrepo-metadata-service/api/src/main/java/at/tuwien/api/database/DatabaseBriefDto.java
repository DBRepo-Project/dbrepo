package at.tuwien.api.database;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseBriefDto {

    @NotNull(message = "database id is required")
    private Long id;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank(message = "internal name is required")
    @JsonProperty("internal_name")
    @Field(name = "internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "Air Quality in Austria")
    private String description;

    private IdentifierBriefDto identifier;

    @JsonProperty("is_public")
    @Field(name = "is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @Schema(example = "mariadb:10.5")
    private String engine;

    @NotNull
    private UserBriefDto owner;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    private ContainerBriefDto container;

    private UserBriefDto creator;

    @NotNull
    @Field(type = FieldType.Date)
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @NotNull
    private ImageBriefDto image;

}
