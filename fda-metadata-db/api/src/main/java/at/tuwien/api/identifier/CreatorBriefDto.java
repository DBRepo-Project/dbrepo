
package at.tuwien.api.identifier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
@Data
@Getter
@Setter
@Builder
public class CreatorBriefDto {

    @NotBlank
    @Schema(example = "Josiah")
    private String firstname;

    @NotBlank
    @Schema(example = "Carberry")
    private String lastname;

    @Schema(example = "Wesleyan University")
    private String affiliation;

}
