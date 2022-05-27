package at.tuwien.api.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

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
    @Parameter(name = "database publicity")
    private Boolean isPublic;

    @Parameter(name = "database subjects")
    private List<DatabaseSubjectDto> subject;

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
    private LicenseIdentifierTypeDto license;

    @Parameter(name = "database language")
    private LanguageTypeDto language;

    @JsonProperty("contact_person")
    @Parameter(name = "database contact person")
    private String contactPerson;

}
