package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Schema(name = "database id", example = "1")
    private Long id;

    @NotBlank
    @Schema(name = "database name", example = "Weather Australia")
    private String name;

    @NotBlank
    @Schema(name = "database exchange")
    private String exchange;

    @NotNull
    @Schema(name = "database creator")
    private UserBriefDto creator;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(name = "database internal name", example = "weather_australia")
    private String internalName;

    @Schema(name = "database subjects")
    private List<String> subjects;

    @Schema(name = "database language", example = "EN")
    private LanguageTypeDto language;

    @Schema(name = "database license", example = "MIT2")
    private LicenseDto license;

    @NotBlank
    @Schema(name = "database description", example = "Weather Australia 2009-2021")
    private String description;

    @Schema(name = "database publisher", example = "TU Wien")
    private String publisher;

    @Schema(name = "database contact person")
    private UserDto contact;

    @JsonProperty("publication_year")
    @Schema(name = "database publication year")
    private Integer publicationYear;

    @JsonProperty("publication_month")
    @Schema(name = "database publication month")
    private Integer publicationMonth;

    @JsonProperty("publication_day")
    @Schema(name = "database publication day")
    private Integer publicationDay;

    @Schema(name = "tables")
    private List<TableBriefDto> tables;

    @JsonProperty("is_public")
    @Schema(name = "database public")
    private Boolean isPublic;

    @Schema(name = "database container image")
    private ImageDto image;

    @Schema(name = "container")
    private ContainerDto container;

    @Schema(name = "database creation time", example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @Parameter(name = "database deletion time", example = "2020-08-04 11:13:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant deleted;

}
