package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import org.springframework.security.authentication.BadCredentialsException;

public interface KeycloakGateway {

    /**
     * Obtains a user token from the Auth Service for a user with given username and password existing in the given
     * realm for a client with given client id and client secret.
     *
     * @param username     The username.
     * @param password     The password.
     * @param realm        The realm.
     * @param clientId     The client id.
     * @param clientSecret The client secret.
     * @return The user token.
     * @throws BadCredentialsException The credentials given for username and password are invalid.
     */
    TokenDto getUserToken(String username, String password, String realm, String clientId, String clientSecret)
            throws BadCredentialsException;


    /**
     * Obtains a user token from the Auth Service for a user with given username and password.
     *
     * @param username The username.
     * @param password The password.
     * @return The user token.
     * @throws BadCredentialsException The credentials given for username and password are invalid.
     */
    TokenDto getUserToken(String username, String password) throws BadCredentialsException;

}
