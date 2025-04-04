package at.ac.tuwien.ifs.dbrepo.core.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
public class UserForgotDto {

    @Schema(example = "jcarberry")
    private String username;

    @Email
    @Schema(example = "jcarberry@brown.edu")
    private String email;

}
