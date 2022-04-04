package at.tuwien.api.user;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsDto implements UserDetails {

    @Parameter(name = "id")
    private Long id;

    @Parameter(name = "user authorities")
    private List<? extends GrantedAuthority> authorities;

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "password hash")
    private String password;

    @NotNull
    @Parameter(name = "mail address")
    private String email;

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
