package at.ac.tuwien.ifs.dbrepo.core.api.ldap;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserDto {

    @NotNull
    private UUID id;

    @NotNull
    private String username;

    @NotNull
    private String email;

}
