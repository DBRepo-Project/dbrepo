package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.Date;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseModifyDto {

    private List<String> subjects;

<<<<<<< HEAD
    @NotBlank
    @Schema(example = "Air Quality in Austria")
    private String description;

    @Schema(example = "TU Wien")
=======
    @Parameter(name = "database description", example = "Sample")
    private String description;

    @NotBlank
    @Parameter(name = "database publisher", example = "TU Wien")
>>>>>>> dev
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(description = "database publication year", example = "2022")
    private Integer publicationYear;

    @Min(1)
    @Max(12)
    @JsonProperty("publication_month")
    @Schema(description = "database publication month", example = "12")
    private Integer publicationMonth;

    @Min(1)
    @Max(31)
    @JsonProperty("publication_day")
    @Schema(description = "database publication day", example = "15")
    private Integer publicationDay;

    private LicenseDto license;

    @Schema(example = "en")
    private LanguageTypeDto language;

    @JsonProperty("contact_person")
    private String contactPerson;

}
