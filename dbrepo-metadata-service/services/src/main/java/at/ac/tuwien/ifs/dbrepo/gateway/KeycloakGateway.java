package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.UUID;

public interface KeycloakGateway {

    List<UserRepresentation> findAll();

    TokenDto obtainUserToken(String username, String password);

    UserRepresentation findByUsername(String username) throws UserNotFoundException;

    UserRepresentation findById(UUID id) throws UserNotFoundException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param id The user id.
     */
    void deleteUser(UUID id) throws UserNotFoundException;

    void updateUser(UUID id, UserUpdateDto data) throws AuthServiceException, UserNotFoundException;
}
