package at.tuwien.service;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Finds all users
     *
     * @return The list of users.
     */
    List<User> findAll();

    /**
     * Finds a user by username.
     *
     * @param username The username.
     * @return The user.
     * @throws UserNotFoundException The user was not found in the metadata database.
     */
    User findByUsername(String username) throws UserNotFoundException;
}
