package at.tuwien.api.orcid;

import at.tuwien.api.orcid.affiliation.OrcidSummaryDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OrcidDto {

    @JsonProperty("person.name.given-names.value")
    private String givenNames;

    @JsonProperty("person.name.family-name.value")
    private String familyName;

    @JsonProperty("activities-summary.employments.affiliation-group")
    private OrcidSummaryDto[] affiliationGroup;

}
