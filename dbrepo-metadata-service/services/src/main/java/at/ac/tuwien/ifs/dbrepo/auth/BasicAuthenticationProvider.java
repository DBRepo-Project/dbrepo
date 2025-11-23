package at.ac.tuwien.ifs.dbrepo.auth;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

@Log4j2
@Component
public class BasicAuthenticationProvider implements AuthenticationProvider {

    private final JwtDecoder jwtDecoder;
    private final GatewayConfig gatewayConfig;
    private final KeycloakGateway keycloakGateway;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    public BasicAuthenticationProvider(JwtDecoder jwtDecoder, GatewayConfig gatewayConfig,
                                       KeycloakGateway keycloakGateway,
                                       JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.gatewayConfig = gatewayConfig;
        this.keycloakGateway = keycloakGateway;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication.getName().equals(gatewayConfig.getSystemUsername()) &&
                authentication.getCredentials().toString().equals(gatewayConfig.getSystemPassword())) {
            /* internal user */
            final Collection<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("system"));
            final UserDetails userDetails = User.builder()
                    .username(authentication.getName())
                    .password(authentication.getCredentials().toString())
                    .roles("SYSTEM")
                    .authorities(authorities)
                    .build();
            log.info("authenticated internal user {} with authorities: {}", userDetails.getUsername(), authorities);
            return UsernamePasswordAuthenticationToken.authenticated(userDetails, "not_a_secret", authorities);
        }
        /* fallback */
        final Jwt jwt = jwtDecoder.decode(keycloakGateway.getUserToken(authentication.getName(),
                        authentication.getCredentials().toString())
                .getAccessToken());
        return new JwtAuthenticationToken(jwt, jwtAuthenticationConverter.convert(jwt).getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.equals(authentication);
    }
}
