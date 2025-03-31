package at.ac.tuwien.ifs.dbrepo.core.api.orcid.person;

import at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.name.OrcidNameDto;
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
public class OrcidPersonDto {

    private OrcidNameDto name;

}
