package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;

public interface UserService {

    /**
     * Finds a user with given username.
     *
     * @param username The username.
     * @return The user.
     * @throws UserNotFoundException The user does not exist.
     */
    User findByUsername(String username) throws UserNotFoundException;
}
