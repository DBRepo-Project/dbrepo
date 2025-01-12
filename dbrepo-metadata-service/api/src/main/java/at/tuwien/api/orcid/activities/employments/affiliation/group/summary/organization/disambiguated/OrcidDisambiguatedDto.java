package at.tuwien.api.orcid.activities.employments.affiliation.group.summary.organization.disambiguated;

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
public class OrcidDisambiguatedDto {

    @JsonProperty("disambiguated-organization-identifier")
    private String identifier;

    @JsonProperty("disambiguation-source")
    private OrcidDisambiguatedSourceTypeDto source;

}
