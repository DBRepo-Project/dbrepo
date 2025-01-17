package at.tuwien.api.user.internal;

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
public class UpdateUserPasswordDto {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

}
