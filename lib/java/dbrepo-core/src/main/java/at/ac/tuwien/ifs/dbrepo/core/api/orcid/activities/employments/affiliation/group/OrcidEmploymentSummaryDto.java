package at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group;

import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.group.summary.OrcidSummaryDto;
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
public class OrcidEmploymentSummaryDto {

    @JsonProperty("employment-summary")
    private OrcidSummaryDto employmentSummary;

}
