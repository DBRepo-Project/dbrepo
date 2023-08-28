package at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization;

import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedDto;
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
public class OrcidOrganizationDto {

    private String name;

    @JsonProperty("disambiguated-organization")
    private OrcidDisambiguatedDto disambiguatedOrganization;

}
