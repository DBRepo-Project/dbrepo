package at.tuwien.auth;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.ServiceConnectionException;
import at.tuwien.exception.ServiceException;
import at.tuwien.gateway.KeycloakGateway;
import jakarta.servlet.ServletException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
public class BasicAuthenticationProvider implements AuthenticationManager {

    private final GatewayConfig gatewayConfig;
    private final AuthTokenFilter authTokenFilter;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public BasicAuthenticationProvider(GatewayConfig gatewayConfig, AuthTokenFilter authTokenFilter,
                                       KeycloakGateway keycloakGateway) {
        this.gatewayConfig = gatewayConfig;
        this.authTokenFilter = authTokenFilter;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        if (auth.getName().equals(gatewayConfig.getAdminUsername())
                && auth.getCredentials().toString().equals(gatewayConfig.getAdminPassword())) {
            log.trace("current user is {}: skip authentication", gatewayConfig.getAdminUsername());
            final UserDetails userDetails = UserDetailsDto.builder()
                    .username(auth.getName())
                    .authorities(List.of(new SimpleGrantedAuthority("admin")))
                    .build();
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        }
        log.trace("current user is {}: begin authentication", auth.getName());
        try {
            final TokenDto tokenDto = keycloakGateway.obtainUserToken(auth.getName(), auth.getCredentials().toString());
            final UserDetails userDetails = authTokenFilter.verifyJwt(tokenDto.getAccessToken());
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (ServletException | ServiceConnectionException | ServiceException e) {
            throw new BadCredentialsException("Failed to authenticate with authentication service", e);
        }
    }
}
