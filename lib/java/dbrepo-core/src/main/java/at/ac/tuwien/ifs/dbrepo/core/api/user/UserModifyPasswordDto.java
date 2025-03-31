package at.ac.tuwien.ifs.dbrepo.core.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class UserModifyPasswordDto {

    @NotNull
    @Schema(example = "jcarberry")
    private String username;

    @NotNull
    private String password;

}
