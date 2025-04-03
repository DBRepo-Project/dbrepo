package at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments;

import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.affiliation.OrcidAffiliationGroupDto;
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
public class OrcidEmploymentsDto {

    @JsonProperty("affiliation-group")
    private OrcidAffiliationGroupDto[] affiliationGroup;

}
