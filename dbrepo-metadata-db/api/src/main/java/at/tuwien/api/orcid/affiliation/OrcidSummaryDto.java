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
public class OrcidSummaryDto {

    @JsonProperty("summaries")
    private EmploymentSummaryDto[] summaries;

}
