package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;

@Data
@Getter
@Setter
@Builder
@Jacksonized
public class CreatorCreateDto {

    @NotBlank
    @Parameter(name = "name", example = "Mustermann, Maximilian")
    private String name;

    @Parameter(name = "affiliation", example = "TU Wien")
    private String affiliation;

    @Parameter(name = "orcid", example = "ORCID")
    private String orcid;

}
