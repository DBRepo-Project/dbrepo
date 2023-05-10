package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserModifyPasswordDto {

    @NotNull
    @Schema(example = "jcarberry")
    private String username;

    @NotNull
    private String password;

}
