package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

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
public class DatabaseDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "air_quality")
    private String exchange;

    @NotNull
    private UserBriefDto creator;

    private IdentifierDto identifier;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "weather_australia")
    private String internalName;

    @Schema(example = "Weather Australia 2009-2021")
    private String description;

    private List<TableBriefDto> tables;

    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    private ImageDto image;

    private ContainerDto container;

    private List<DatabaseAccessDto> accesses;

    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @Schema(example = "2020-08-04 11:13:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant deleted;

}
