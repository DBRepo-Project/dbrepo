package at.tuwien.api.user;

import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordDto {

    @NotNull
    private String password;

}
