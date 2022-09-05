package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseModifyDto {

    @Parameter(name = "database subjects", example = "[\"test\"]")
    private List<String> subjects;

    @NotBlank
    @Parameter(name = "database description", example = "Sample")
    private String description;

    @Parameter(name = "database publisher", example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Parameter(name = "database publication year")
    private Integer publicationYear;

    @Min(1)
    @Max(12)
    @JsonProperty("publication_month")
    @Parameter(name = "database publication month")
    private Integer publicationMonth;

    @Min(1)
    @Max(31)
    @JsonProperty("publication_day")
    @Parameter(name = "database publication day")
    private Integer publicationDay;

    @Parameter(name = "database license")
    private LicenseDto license;

    @Parameter(name = "database language", example = "EN")
    private LanguageTypeDto language;

    @JsonProperty("contact_person")
    @Parameter(name = "database contact person")
    private String contactPerson;

}
