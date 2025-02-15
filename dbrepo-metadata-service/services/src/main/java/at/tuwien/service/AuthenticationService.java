package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.CredentialsInvalidException;
import at.tuwien.exception.UserNotFoundException;

public interface AuthenticationService {

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param user The user.
     * @throws AuthServiceException           The auth service responded with unexpected behavior.
     * @throws AuthServiceConnectionException The connection with the auth service could not be established.
     * @throws UserNotFoundException      The user was not found after creation in the auth database.
     */
    void delete(User user) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException,
            CredentialsInvalidException;

}
