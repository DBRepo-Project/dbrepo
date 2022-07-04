package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.identifier.CreatorDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Parameter(name = "database id", example = "1")
    private Long id;

    @NotBlank
    @Parameter(name = "database name", example = "Weather Australia")
    private String name;

    @NotNull
    @Parameter(name = "database creator")
    private UserDto creator;

    @NotBlank
    @JsonProperty("internal_name")
    @Parameter(name = "database internal name", example = "weather_australia")
    private String internalName;

    @Parameter(name = "database subjects")
    private List<String> subjects;

    @Parameter(name = "database language", example = "EN")
    private LanguageTypeDto language;

    @Parameter(name = "database license", example = "MIT2")
    private LicenseDto license;

    @NotBlank
    @Parameter(name = "database description", example = "Weather Australia 2009-2021")
    private String description;

    @Parameter(name = "database publisher", example = "TU Wien")
    private String publisher;

    @JsonProperty("publication_year")
    @Parameter(name = "database publication year")
    private Short publicationYear;

    @Parameter(name = "database contact person")
    private UserDto contact;

    @NotBlank
    @Parameter(name = "database exchange", example = "fda.c1.d1")
    private String exchange;

    @NotNull
    @Parameter(name = "tables")
    private List<TableDto> tables;

    @JsonProperty("is_public")
    @Parameter(name = "database public")
    private Boolean isPublic;

    @NotBlank
    @Parameter(name = "database container image")
    private ImageDto image;

    @NotBlank
    @Parameter(name = "container")
    private ContainerDto container;

    @Parameter(name = "database creation time", example = "2020-08-04 11:12:00")
    private Instant created;

    @Parameter(name = "database deletion time", example = "2020-08-04 11:13:00")
    private Instant deleted;

}
