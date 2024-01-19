package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

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

    /**
     * Finds a specific user in the metadata database by given id.
     *
     * @param id The user id.
     * @return The user, if successful.
     * @throws UserNotFoundException The user was not found.
     */
    User find(UUID id) throws UserNotFoundException;

    /**
     * Creates a user in the metadata database managed by Keycloak in the given realm.
     *
     * @param data The user data.
     * @param id   The user id.
     * @return The user, if successful.
     */
    User create(SignupRequestDto data, UUID id);

    /**
     * Updates the user information for a user with given id in the metadata database.
     *
     * @param id   The user id.
     * @param data The user information.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException The user was not found.
     */
    User modify(UUID id, UserUpdateDto data) throws UserNotFoundException;

    /**
     * Updates the user password for a user with given id in the metadata database.
     *
     * @param id   The user id.
     * @param data The new password.
     */
    void updatePassword(UUID id, UserPasswordDto data) throws UserNotFoundException;

    /**
     * Updates the user theme for a user with given id in the metadata database.
     *
     * @param id   The user id.
     * @param data The user theme.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException The user was not found.
     */
    User toggleTheme(UUID id, UserThemeSetDto data) throws UserNotFoundException;

    /**
     * Validates if a user with the given username already exists in the metadata database.
     *
     * @param username The username.
     * @throws UserAlreadyExistsException The user with this username already exists.
     */
    void validateUsernameNotExists(String username) throws UserAlreadyExistsException;

    /**
     * Validates if a user with the given email already exists in the metadata database.
     *
     * @param email The email.
     * @throws UserEmailAlreadyExistsException The user with this email already exists.
     */
    void validateEmailNotExists(String email) throws UserEmailAlreadyExistsException;
}
