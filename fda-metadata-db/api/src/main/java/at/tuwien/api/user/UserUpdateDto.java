package at.tuwien.api.user;

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
public class UserUpdateDto {

    @JsonProperty("titles_before")
    @Parameter(name = "titles before the first name")
    private String titlesBefore;

    @JsonProperty("titles_after")
    @Parameter(name = "titles after the last name")
    private String titlesAfter;

    @NotBlank
    @Parameter(name = "first name")
    private String firstname;

    @NotBlank
    @Parameter(name = "last name")
    private String lastname;

    @Parameter(name = "affiliation")
    private String affiliation;

    @Parameter(name = "orcid")
    private String orcid;

    @NotNull
    @JsonProperty("theme_dark")
    @Parameter(name = "theme dark")
    private Boolean themeDark;

}
