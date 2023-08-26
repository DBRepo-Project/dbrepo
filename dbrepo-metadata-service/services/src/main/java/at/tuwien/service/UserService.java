package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.exception.*;

import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Finds all users in the metadata database
     *
     * @return The list of users.
     */
    List<UserBriefDto> findAll() throws KeycloakRemoteException, AccessDeniedException;

    /**
     * Finds a user by username.
     *
     * @param username The username.
     * @return The user, if successfully.
     * @throws UserNotFoundException The user with this username was not found in the metadata database.
     */
    UserDto findByUsername(String username) throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException;

    /**
     * Finds a specific user in the metadata database by given id.
     *
     * @param id The user id.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException The user was not found.
     */
    UserDto find(UUID id) throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException;

    /**
     * Creates a user in the metadata database managed by Keycloak in the given realm.
     *
     * @param data  The user data.
     * @return The user, if successful. False otherwise.
     * @throws UserAlreadyExistsException The user already exists in the metadata database.
     */
    UserDto create(SignupRequestDto data) throws UserAlreadyExistsException, AccessDeniedException,
            KeycloakRemoteException, UserNotFoundException;

    /**
     * Updates the user information for a user with given id in the metadata database.
     *
     * @param id   The user id.
     * @param data The user information.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException          The user was not found.
     * @throws UserAttributeNotFoundException One or more user attributes for the user information were not found.
     */
    UserDto modify(UUID id, UserUpdateDto data) throws UserNotFoundException, UserAttributeNotFoundException, KeycloakRemoteException, AccessDeniedException;

    /**
     * Updates the user password for a user with given id.
     *
     * @param id   The user id.
     * @param data The new password.
     * @throws UserNotFoundException The user was not found.
     */
    void updatePassword(UUID id, UserPasswordDto data) throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException;

    /**
     * Updates the user theme for a user with given id.
     *
     * @param id   The user id.
     * @param data The user theme.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException          The user was not found.
     * @throws UserAttributeNotFoundException One or more user attributes for the user information were not found.
     */
    UserDto toggleTheme(UUID id, UserThemeSetDto data) throws UserNotFoundException, UserAttributeNotFoundException, KeycloakRemoteException, AccessDeniedException;

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
