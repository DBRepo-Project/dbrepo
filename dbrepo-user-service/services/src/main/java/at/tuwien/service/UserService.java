package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserThemeSetDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.user.Realm;
import at.tuwien.entities.user.Role;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Finds all users in the metadata database
     *
     * @return The list of users.
     */
    List<User> findAll();

    /**
     * Creates a user in the metadata database managed by Keycloak in the given realm and with given role.
     *
     * @param data  The user data.
     * @param realm The realm this user should be created.
     * @param role  The role.
     * @return The user, if successful. False otherwise.
     * @throws UserAlreadyExistsException The user already exists in the metadata database.
     */
    User create(SignupRequestDto data, Realm realm, Role role) throws UserAlreadyExistsException;

    /**
     * Updates the user information for a user with given id in the metadata database.
     *
     * @param id   The user id.
     * @param data The user information.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException          The user was not found.
     * @throws UserAttributeNotFoundException One or more user attributes for the user information were not found.
     */
    User modify(UUID id, UserUpdateDto data) throws UserNotFoundException, UserAttributeNotFoundException;

    /**
     * Updates the user password for a user with given id.
     *
     * @param id   The user id.
     * @param data The new password.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException The user was not found.
     */
    User updatePassword(UUID id, UserPasswordDto data) throws UserNotFoundException;

    /**
     * Updates the user theme for a user with given id.
     *
     * @param id   The user id.
     * @param data The user theme.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException          The user was not found.
     * @throws UserAttributeNotFoundException One or more user attributes for the user information were not found.
     */
    User toggleTheme(UUID id, UserThemeSetDto data) throws UserNotFoundException, UserAttributeNotFoundException;

    /**
     * Finds a specific user in the metadata database by given id.
     *
     * @param id The user id.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException The user was not found.
     */
    User find(UUID id) throws UserNotFoundException;

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
