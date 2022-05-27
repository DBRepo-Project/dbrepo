package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseModifyDto {

    @NotNull
    @JsonProperty("is_public")
    @Parameter(name = "database publicity")
    private Boolean isPublic;

    @Parameter(name = "database subject")
    private String subject;

    @Parameter(name = "database description")
    private String description;

    @NotNull
    @Parameter(name = "database publisher")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Parameter(name = "database year")
    private Short publicationYear;

    @Parameter(name = "database license")
    private LicenseDto license;

    @Parameter(name = "database language")
    private LanguageDto language;

    @JsonProperty("contact_person")
    @Parameter(name = "database contact person")
    private String contactPerson;

}
