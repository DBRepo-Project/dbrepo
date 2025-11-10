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

    TokenDto getUserToken(String username, String password, String realm, String clientId, String clientSecret)
            throws BadCredentialsException;

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
