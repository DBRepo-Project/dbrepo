package at.tuwien.auth;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.gateway.KeycloakGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class BasicAuthenticationProvider implements AuthenticationManager {

    private final AuthTokenFilter authTokenFilter;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public BasicAuthenticationProvider(AuthTokenFilter authTokenFilter, KeycloakGateway keycloakGateway) {
        this.authTokenFilter = authTokenFilter;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        final TokenDto tokenDto = keycloakGateway.obtainUserToken(auth.getName(), auth.getCredentials().toString());
        final UserDetails userDetails = authTokenFilter.verifyJwt(tokenDto.getAccessToken());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
