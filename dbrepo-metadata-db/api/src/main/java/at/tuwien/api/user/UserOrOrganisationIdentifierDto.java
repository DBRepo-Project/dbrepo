package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserOrOrganisationIdentifierDto {

    @Schema(example = "https://orcid.org/0000-0002-1825-0097")
    private String url;

}
