package at.tuwien.auth;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.tuwien.service.CredentialService;
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

    private final CredentialService credentialService;
    private final AuthTokenFilter authTokenFilter;

    @Autowired
    public BasicAuthenticationProvider(CredentialService credentialService, AuthTokenFilter authTokenFilter) {
        this.credentialService = credentialService;
        this.authTokenFilter = authTokenFilter;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        final TokenDto tokenDto = credentialService.getAccessToken(auth.getName(), auth.getCredentials().toString());
        final UserDetails userDetails = authTokenFilter.verifyJwt(tokenDto.getAccessToken());
        log.debug("set basic auth principal username: {}", userDetails.getUsername());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
