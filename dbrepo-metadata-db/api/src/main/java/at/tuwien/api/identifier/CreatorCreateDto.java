package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "Josiah")
    private String firstname;

    @NotBlank
    @Schema(example = "Carberry")
    private String lastname;

    @Schema(example = "Wesleyan University")
    private String affiliation;

    @Schema(example = "0000-0002-1825-0097")
    private String orcid;

}
