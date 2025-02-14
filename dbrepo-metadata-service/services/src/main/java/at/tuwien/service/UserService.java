package at.tuwien.service;

import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.UserExistsException;
import at.tuwien.exception.UserNotFoundException;

import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Finds all users in the metadata database.
     *
     * @return The list of users.
     */
    List<User> findAll();

    /**
     * Finds a user by username in the metadata database.
     *
     * @param username The username.
     * @return The user, if successfully.
     * @throws UserNotFoundException The user with this username was not found in the metadata database.
     */
    User findByUsername(String username) throws UserNotFoundException;

    List<User> findAllInternalUsers();

    /**
     * Finds a specific user in the metadata database by given id.
     *
     * @param id The user id.
     * @return The user, if successful.
     * @throws UserNotFoundException The user was not found.
     */
    User findById(UUID id) throws UserNotFoundException;

    /**
     * Creates a user in the metadata database managed by Keycloak in the given realm.
     *
     * @param data The user data.
     * @return The user, if successful.
     */
    User create(CreateUserDto data);

    /**
     * Updates the user information for a user with given id in the metadata database.
     *
     * @param user The user.
     * @param data The user information.
     * @return The user if successful. False otherwise.
     */
    User modify(User user, UserUpdateDto data) throws UserNotFoundException, AuthServiceException;

    /**
     * Updates the user password for a user with given id in the metadata database.
     *
     * @param user The user.
     * @param data The new password.
     */
    void updatePassword(User user, UserPasswordDto data);

    /**
     * Validates if a user with the given username already exists in the metadata database.
     *
     * @param username The username.
     * @throws UserExistsException The user with this username already exists.
     */
    void validateUsernameNotExists(String username) throws UserExistsException;

    String getMariaDbPassword(String password);
}
