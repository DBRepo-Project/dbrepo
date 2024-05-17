package at.tuwien.api.orcid;

import at.tuwien.api.orcid.activities.OrcidActivitiesSummaryDto;
import at.tuwien.api.orcid.person.OrcidPersonDto;
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

    private String path;

    private OrcidPersonDto person;

    @JsonProperty("activities-summary")
    private OrcidActivitiesSummaryDto activitiesSummary;

}
