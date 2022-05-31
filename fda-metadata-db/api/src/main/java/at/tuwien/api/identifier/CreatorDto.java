package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Getter
@Setter
@Builder
public class CreatorDto {

    @NotNull
    private Long id;

    @NotBlank
    @Parameter(name = "firstname", example = "Maximilian")
    private String firstname;

    @NotBlank
    @Parameter(name = "lastname", example = "Mustermann")
    private String lastname;

    @Parameter(name = "affiliation", example = "TU Wien")
    private String affiliation;

    @Parameter(name = "orcid", example = "ORCID")
    private String orcid;

    @NotNull
    private Instant created;

    @NotNull
    @JsonProperty("last_modified")
    private Instant lastModified;

}
