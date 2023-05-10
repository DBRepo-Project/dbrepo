package at.tuwien.api.user;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRolesDto {

    @NotNull
    private List<RoleTypeDto> roles;

}
