package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
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
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @Schema(example = "air_quality")
    private String exchange;

    @NotNull
    private UserBriefDto creator;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(description = "database subjects")
    private List<String> subjects;

    @Schema(example = "en")
    private LanguageTypeDto language;

    private LicenseDto license;

    @Schema(example = "Air Quality in Austria")
    private String description;

    @Schema(example = "TU Wien")
    private String publisher;

    private UserDto contact;

    @JsonProperty("publication_year")
    @Schema(description = "database publication year", example = "2022")
    private Integer publicationYear;

    @JsonProperty("publication_month")
    @Schema(description = "database publication month", example = "12")
    private Integer publicationMonth;

    @JsonProperty("publication_day")
    @Schema(description = "database publication day", example = "15")
    private Integer publicationDay;

    private List<TableBriefDto> tables;

    @JsonProperty("is_public")
    @Schema(description = "database publicity", example = "true")
    private Boolean isPublic;

    private ImageDto image;

    private ContainerDto container;

    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @Schema(example = "2020-08-04 11:13:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant deleted;

}
