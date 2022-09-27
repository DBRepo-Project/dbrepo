package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
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
<<<<<<< HEAD
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "air_quality")
    private String exchange;

    @NotNull
=======
    @Schema(example = "1")
    private Long id;

    @NotBlank
    @Schema(example = "Weather Australia")
    private String name;

    @NotBlank
    private String exchange;

    @NotNull
    @Schema
>>>>>>> dev
    private UserBriefDto creator;

    @NotBlank
    @JsonProperty("internal_name")
<<<<<<< HEAD
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(description = "database subjects")
    private List<String> subjects;

    @Schema(example = "en")
    private LanguageTypeDto language;

    private LicenseDto license;

    @Schema(example = "Air Quality in Austria")
=======
    @Schema(example = "weather_australia")
    private String internalName;

    private List<String> subjects;

    @Schema(example = "EN")
    private LanguageTypeDto language;

    @Schema(example = "MIT2")
    private LicenseDto license;

    @Schema(example = "Weather Australia 2009-2021")
>>>>>>> dev
    private String description;

    @Schema(example = "TU Wien")
    private String publisher;

<<<<<<< HEAD
    private UserDto contact;

    @JsonProperty("publication_year")
    @Schema(description = "database publication year", example = "2022")
    private Integer publicationYear;

    @JsonProperty("publication_month")
    @Schema(description = "database publication month", example = "12")
    private Integer publicationMonth;

    @JsonProperty("publication_day")
    @Schema(description = "database publication day", example = "15")
=======
    @JsonProperty("publication_year")
    private Integer publicationYear;

    @JsonProperty("publication_month")
    private Integer publicationMonth;

    @JsonProperty("publication_day")
>>>>>>> dev
    private Integer publicationDay;

    private List<TableBriefDto> tables;

    @JsonProperty("is_public")
<<<<<<< HEAD
    @Schema(description = "database publicity", example = "true")
=======
>>>>>>> dev
    private Boolean isPublic;

    private ImageDto image;

    private ContainerDto container;

    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

<<<<<<< HEAD
    @Schema(example = "2020-08-04 11:13:00")
=======
    @Parameter(example = "2020-08-04 11:13:00")
>>>>>>> dev
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant deleted;

}
