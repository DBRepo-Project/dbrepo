package at.ac.tuwien.ifs.dbrepo.core.api.ror;

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
public class RorDto {

    private String id;

    private String name;

    private Integer established;
}
