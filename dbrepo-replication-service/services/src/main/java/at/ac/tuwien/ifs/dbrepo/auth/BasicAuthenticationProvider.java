package at.ac.tuwien.ifs.dbrepo.auth;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

@Slf4j
@Component
public class BasicAuthenticationProvider implements AuthenticationProvider {

    private final GatewayConfig gatewayConfig;

    public BasicAuthenticationProvider(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!authentication.getName().equals(gatewayConfig.getSystemUsername())
                || !authentication.getCredentials().toString().equals(gatewayConfig.getSystemPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
        final Collection<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("system"));
        final UserDetails userDetails = User.builder()
                .username(authentication.getName())
                .password(authentication.getCredentials().toString())
                .authorities(authorities)
                .build();
        log.info("authenticated internal user {} with authorities: {}", userDetails.getUsername(), authorities);
        return UsernamePasswordAuthenticationToken.authenticated(userDetails, "not_a_secret", authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.equals(authentication);
    }
}
