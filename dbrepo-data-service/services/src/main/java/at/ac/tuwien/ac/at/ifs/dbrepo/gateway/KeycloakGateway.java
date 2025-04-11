package at.ac.tuwien.ac.at.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import org.springframework.security.authentication.BadCredentialsException;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws BadCredentialsException;

}
