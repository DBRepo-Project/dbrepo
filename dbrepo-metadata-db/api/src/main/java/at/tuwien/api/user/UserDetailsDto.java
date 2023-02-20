package at.tuwien.api.user;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsDto implements UserDetails {

    private Long id;

    private List<? extends GrantedAuthority> authorities;

    @NotNull
    private String username;

    @NotNull
    @ToString.Exclude
    private String password;

    @NotNull
    @Email
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
