package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.Email;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserForgotDto {

    @Schema(example = "jcarberry")
    private String username;

    @Email
    @Schema(example = "jcarberry@brown.edu")
    private String email;

}
