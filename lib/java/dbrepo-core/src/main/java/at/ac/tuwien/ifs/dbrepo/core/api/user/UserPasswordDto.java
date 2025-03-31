package at.ac.tuwien.ifs.dbrepo.core.api.user;

import jakarta.validation.constraints.NotNull;
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
public class UserPasswordDto {

    @NotNull
    private String password;

}
