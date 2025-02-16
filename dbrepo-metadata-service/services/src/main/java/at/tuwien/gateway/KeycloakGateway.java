package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.UserNotFoundException;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.UUID;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password);

    UserRepresentation findByUsername(String username) throws UserNotFoundException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param id The user id.
     */
    void deleteUser(UUID id) throws UserNotFoundException;

    void updateUser(UUID id, UserUpdateDto data) throws AuthServiceException, UserNotFoundException;
}
