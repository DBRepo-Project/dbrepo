package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;

import java.util.UUID;

public interface AuthenticationService {

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param id The user id.
     * @throws AuthServiceException  The auth service responded with unexpected behavior.
     * @throws UserNotFoundException The user was not found after creation in the auth database.
     */
    void delete(UUID id) throws AuthServiceException, UserNotFoundException;

}
