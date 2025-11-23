package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;
import java.util.UUID;

public interface KeycloakGateway {

    List<UserRepresentation> findAll();

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

    UserRepresentation findByUsername(String username) throws UserNotFoundException, NotAllowedException;

    UserRepresentation findById(UUID id) throws UserNotFoundException, NotAllowedException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param id The user id.
     */
    void deleteUser(UUID id) throws UserNotFoundException;

    void updateUser(UUID id, UserUpdateDto data) throws AuthServiceException, UserNotFoundException;
}
