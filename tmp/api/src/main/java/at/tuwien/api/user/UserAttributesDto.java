package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserAttributesDto {

    @NotNull
    @Schema(example = "light")
    private String theme;

    @Schema(example = "https://orcid.org/0000-0002-1825-0097")
    private String orcid;

    @Schema(example = "Brown University")
    private String affiliation;

    @NotNull
    @Schema(example = "en")
    private String language;

    @JsonIgnore
    @ToString.Exclude
    @Schema(example = "*CC67043C7BCFF5EEA5566BD9B1F3C74FD9A5CF5D")
    private String mariadbPassword;

}
