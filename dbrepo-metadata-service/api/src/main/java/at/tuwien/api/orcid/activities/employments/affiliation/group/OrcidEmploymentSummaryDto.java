package at.tuwien.api.orcid.activities.employments.affiliation.group;

import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.OrcidSummaryDto;
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
public class OrcidEmploymentSummaryDto {

    @JsonProperty("employment-summary")
    private OrcidSummaryDto employmentSummary;

}
