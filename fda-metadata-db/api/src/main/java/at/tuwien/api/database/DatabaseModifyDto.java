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

    @Parameter(name = "database subjects", example = "[\"test\"]")
    private List<String> subject;

    @Parameter(name = "database description", example = "Description")
    private String description;

    @NotNull
    @Parameter(name = "database publisher", example = "Publisher")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Parameter(name = "database year", example = "2022")
    private Short publicationYear;

    @Parameter(name = "database license", example = "MIT")
    private String license;

    @Parameter(name = "database language", example = "EN")
    private LanguageTypeDto language;

    @JsonProperty("contact_person")
    @Parameter(name = "database contact person")
    private String contactPerson;

}
