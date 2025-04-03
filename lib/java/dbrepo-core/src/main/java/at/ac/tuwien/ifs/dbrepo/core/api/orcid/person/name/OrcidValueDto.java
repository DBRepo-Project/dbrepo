package at.ac.tuwien.ifs.dbrepo.core.api.orcid.person.name;

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
public class OrcidValueDto {

    private String value;

}
