package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

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

    @NotNull
    @JsonProperty("is_public")
    @Parameter(name = "database publicity", example = "true")
    private Boolean isPublic;

    @Parameter(name = "database subjects", example = "[\"test\"]")
    private List<String> subject;

    @NotBlank
    @Parameter(name = "database description", example = "Sample")
    private String description;

    @Parameter(name = "database publisher", example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Parameter(name = "database year", example = "2022")
    private Short publicationYear;

    @Parameter(name = "database license")
    private LicenseDto license;

    @Parameter(name = "database language", example = "EN")
    private LanguageTypeDto language;

    @JsonProperty("contact_person")
    @Parameter(name = "database contact person")
    private String contactPerson;

}
