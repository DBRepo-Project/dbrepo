package at.tuwien.api.user.external;

import at.tuwien.api.user.external.affiliation.ExternalAffiliationDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ExternalMetadataDto {

    @Schema(example = "Josiah")
    @JsonProperty("given_names")
    private String givenNames;

    @Schema(example = "Carberry")
    @JsonProperty("family_name")
    private String familyName;

    private ExternalAffiliationDto[] affiliations;

    private ExternalResultType type;

}
