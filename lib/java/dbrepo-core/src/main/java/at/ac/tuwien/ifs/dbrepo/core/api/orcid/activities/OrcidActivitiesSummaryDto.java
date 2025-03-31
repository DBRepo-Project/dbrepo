package at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities;

import at.ac.tuwien.ifs.dbrepo.core.api.orcid.activities.employments.OrcidEmploymentsDto;
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
public class OrcidActivitiesSummaryDto {

    private String path;

    private OrcidEmploymentsDto employments;

}
