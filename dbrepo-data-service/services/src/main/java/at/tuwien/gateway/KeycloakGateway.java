package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.exception.AccountNotSetupException;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.CredentialsInvalidException;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws AuthServiceConnectionException,
            CredentialsInvalidException, AccountNotSetupException;

}
