package at.tuwien.api.orcid.activities.employments.affiliation;

import at.tuwien.api.orcid.activities.employments.affiliation.group.OrcidEmploymentSummaryDto;
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
public class OrcidAffiliationGroupDto {

    private OrcidEmploymentSummaryDto[] summaries;

}
