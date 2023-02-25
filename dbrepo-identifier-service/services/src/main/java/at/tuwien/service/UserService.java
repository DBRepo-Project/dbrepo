package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;

public interface UserService {

    /**
     * Finds a user with given username in the metadata database.
     *
     * @param username The username.
     * @return The user, if successful.
     * @throws UserNotFoundException The user could not be found in the metadata database.
     */
    User findByUsername(String username) throws UserNotFoundException;
}
