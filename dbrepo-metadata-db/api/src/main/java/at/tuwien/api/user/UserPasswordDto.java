package at.tuwien.api.user;

import lombok.*;

import javax.validation.constraints.NotNull;

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
