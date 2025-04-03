package at.ac.tuwien.ifs.dbrepo.core.api.orcid;

import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.OrcidActivitiesSummaryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.OrcidPersonDto;
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
public class OrcidDto {

    private String path;

    private OrcidPersonDto person;

    @JsonProperty("activities-summary")
    private OrcidActivitiesSummaryDto activitiesSummary;

}
