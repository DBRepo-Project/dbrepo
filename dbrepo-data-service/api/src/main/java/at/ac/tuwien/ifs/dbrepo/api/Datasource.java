package at.ac.tuwien.ifs.dbrepo.api;

import jakarta.validation.constraints.NotBlank;
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
public class Datasource {

    @NotBlank
    private String name;

    private String alias;

}
