package at.tuwien.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccountNotSetupException;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.CredentialsInvalidException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import org.springframework.security.authentication.BadCredentialsException;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws BadCredentialsException;

}
