package at.tuwien.api.orcid.activities;

import at.tuwien.api.orcid.activities.employments.OrcidEmploymentsDto;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OrcidActivitiesSummaryDto {

    private String path;

    private OrcidEmploymentsDto employments;

}
