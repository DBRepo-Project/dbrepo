package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDto {

    @JsonProperty("titles_before")
    @Schema(example = "Prof.")
    private String titlesBefore;

    @JsonProperty("titles_after")
    private String titlesAfter;

    @Schema(example = "Josiah")
    private String firstname;

    @Schema(example = "Carberry")
    private String lastname;

    @Schema(example = "Brown University")
    private String affiliation;

    @Schema(example = "0000-0002-1825-0097")
    private String orcid;

}
