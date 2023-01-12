package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import java.util.List;

public interface UserService {

    /**
     * Find all users.
     *
     * @return The list of users.
     */
    List<User> findAll();

    /**
     * Finds a specific user by given id.
     *
     * @param id The id.
     * @return The user.
     * @throws UserNotFoundException The user was not found in the metadata database.
     */
    User find(Long id) throws UserNotFoundException;

    /**
     * Finds a specific user by given username or email.
     *
     * @param username The username.
     * @param email    The email.
     * @return The user.
     * @throws UserNotFoundException The user was not found in the metadata database.
     */
    User findByUsernameOrEmail(String username, String email) throws UserNotFoundException;

    /**
     * Finds a specific user by given username.
     *
     * @param username The username.
     * @return The user.
     * @throws UserNotFoundException The user was not found in the metadata database.
     */
    User findByUsername(String username) throws UserNotFoundException;

    /**
     * Creates a new user with information.
     *
     * @param user The information.
     * @return The created user.
     * @throws UserEmailExistsException The email in the information exists already.
     * @throws UserNameExistsException  The username exists already.
     * @throws RoleNotFoundException    The role specified was not found.
     */
    User create(SignupRequestDto user) throws UserEmailExistsException, UserNameExistsException, RoleNotFoundException;

    /**
     * Resets the user information
     *
     * @param data The user username or email
     * @return The user.
     * @throws UserNotFoundException The user was not found.
     */
    User forgot(UserForgotDto data) throws UserNotFoundException;

    /**
     * Updates a user with given id and updated information.
     *
     * @param id   The id.
     * @param data The updated information.
     * @return The updated user.
     * @throws UserNotFoundException The user was not found.
     */
    User update(Long id, UserUpdateDto data) throws UserNotFoundException, OrcidMalformedException;

    /**
     * Updates a user with given id and updated roles.
     *
     * @param id   The id.
     * @param data The updated roles.
     * @return The updated user.
     * @throws UserNotFoundException The user was not found.
     * @throws RoleNotFoundException Some updated roles were not found.
     */
    User updateRoles(Long id, UserRolesDto data)
            throws UserNotFoundException, RoleNotFoundException, RoleUniqueException;

    /**
     * Sets the theme for the provided user.
     *
     * @param id   The user id.
     * @param data The theme.
     * @throws UserNotFoundException The user was not found.
     */
    void updateTheme(Long id, UserThemeSetDto data) throws UserNotFoundException;

    /**
     * Updates a user with the given id and updated password.
     *
     * @param id   The id.
     * @param data The updated roles.
     * @return The updated user.
     * @throws UserNotFoundException The user was not found.
     */
    User updatePassword(Long id, UserPasswordDto data) throws UserNotFoundException, BrokerUserCreationException;

    /**
     * Updates a user with the given id and updated email.
     *
     * @param id   The id.
     * @param data The updated email.
     * @return The updated user.
     * @throws UserNotFoundException The user was not found.
     */
    User updateEmail(Long id, UserEmailDto data) throws UserNotFoundException;
}
