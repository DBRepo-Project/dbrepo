package at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LdCreatorDto {

    @NotNull
    private String name;

    @NotNull
    @JsonProperty("@type")
    private String type;

    private String sameAs;

    private String givenName;

    private String familyName;

}
