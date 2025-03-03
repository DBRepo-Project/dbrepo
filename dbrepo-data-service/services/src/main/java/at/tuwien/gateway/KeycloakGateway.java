package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.exception.AccountNotSetupException;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.CredentialsInvalidException;
import at.tuwien.exception.NotAllowedException;
import org.springframework.security.authentication.BadCredentialsException;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws BadCredentialsException;

}
