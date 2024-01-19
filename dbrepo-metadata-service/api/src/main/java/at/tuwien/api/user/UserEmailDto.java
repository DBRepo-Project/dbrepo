package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserEmailDto {

    @NotNull
    @Email
    @Schema(example = "jcarberry@brown.edu")
    private String email;

}
