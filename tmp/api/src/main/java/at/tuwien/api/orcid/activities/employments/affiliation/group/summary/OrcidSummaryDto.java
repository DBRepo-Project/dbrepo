package at.tuwien.api.orcid.activities.employments.affiliation.group.summary;

import at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.OrcidOrganizationDto;
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
public class OrcidSummaryDto {

    @JsonProperty("department-name")
    private String departmentName;

    @JsonProperty("role-title")
    private String roleTitle;

    private OrcidOrganizationDto organization;

    @JsonProperty("display-index")
    private Integer displayIndex;

}
