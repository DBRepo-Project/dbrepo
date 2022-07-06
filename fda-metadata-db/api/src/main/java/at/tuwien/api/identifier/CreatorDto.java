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
    @Parameter(name = "name", example = "Mustermann, Maximilian")
    private String name;

    @Parameter(name = "affiliation", example = "TU Wien")
    private String affiliation;

    @Parameter(name = "orcid", example = "ORCID")
    private String orcid;

    @NotNull
    private Instant created;

    @JsonProperty("last_modified")
    private Instant lastModified;

}
