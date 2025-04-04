package at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization;

import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated.OrcidDisambiguatedDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OrcidOrganizationDto {

    private String name;

    @JsonProperty("disambiguated-organization")
    private OrcidDisambiguatedDto disambiguatedOrganization;

}
