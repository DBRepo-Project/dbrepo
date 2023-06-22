package at.tuwien.api.orcid.affiliation;

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
public class EmploymentSummaryDto {

    @JsonProperty("employment-summary.department-name")
    private String departmentName;

    @JsonProperty("employment-summary.role-title")
    private String roleTitle;

    @JsonProperty("employment-summary.organization.name")
    private String organizationName;

    @JsonProperty("employment-summary.display-index")
    private Integer displayIndex;

}
