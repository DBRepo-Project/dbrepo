package at.tuwien.api.orcid.person.name;

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
public class OrcidNameDto {

    private String path;

    @JsonProperty("given-names")
    private OrcidValueDto givenNames;

    @JsonProperty("family-name")
    private OrcidValueDto familyName;

}
