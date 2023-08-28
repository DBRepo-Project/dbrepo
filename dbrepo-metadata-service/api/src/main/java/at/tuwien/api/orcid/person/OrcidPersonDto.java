package at.tuwien.api.orcid.person;

import at.tuwien.api.orcid.person.name.OrcidNameDto;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OrcidPersonDto {

    private OrcidNameDto name;

}
