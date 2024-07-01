package at.tuwien.api.ldap;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDto {

    @NotNull
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull
    private String username;

    @NotNull
    private String email;

}
