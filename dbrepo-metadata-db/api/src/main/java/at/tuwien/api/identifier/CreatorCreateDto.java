package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
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
