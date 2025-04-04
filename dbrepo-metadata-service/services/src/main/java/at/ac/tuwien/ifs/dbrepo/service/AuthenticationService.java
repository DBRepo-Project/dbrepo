package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;

public interface AuthenticationService {

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param user The user.
     * @throws AuthServiceException   The auth service responded with unexpected behavior.
     * @throws UserNotFoundException  The user was not found after creation in the auth database.
     */
    void delete(User user) throws AuthServiceException, UserNotFoundException;

}
