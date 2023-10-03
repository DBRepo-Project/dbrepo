package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

public interface UserService {

    /**
     * Finds a user by username.
     *
     * @param username The username.
     * @return The user, if successfully.
     * @throws UserNotFoundException The user with this username was not found in the metadata database.
     */
    User findByUsername(String username) throws UserNotFoundException;

}
