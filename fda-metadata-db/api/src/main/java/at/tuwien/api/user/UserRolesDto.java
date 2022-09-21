package at.tuwien.api.user;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRolesDto {

    @NotNull
    private List<String> roles;

}
