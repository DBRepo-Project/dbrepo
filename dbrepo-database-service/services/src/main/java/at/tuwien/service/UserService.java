package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;

public interface UserService {

    /**
     * Finds a user by given username in the metadata database.
     *
     * @param username The username.
     * @return The user, if successful.
     * @throws UserNotFoundException The user with this username could not be found in the metadata database.
     */
    User findByUsername(String username) throws UserNotFoundException;

    /**
     * Finds a user by given id in the metadata database.
     *
     * @param userId The user id.
     * @return The user, if successful.
     * @throws UserNotFoundException The user with this id could not be found in the metadata database.
     */
    User find(Long userId) throws UserNotFoundException;
}
