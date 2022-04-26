package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

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
     * @return The specific user.
     * @throws UserNotFoundException The user was not found.
     */
    User find(Long id) throws UserNotFoundException;

    /**
     * Creates a new user with information.
     *
     * @param user The information.
     * @return The created user.
     * @throws UserEmailExistsException The email in the information exists already.
     * @throws UserNameExistsException  The user name extists already.
     * @throws RoleNotFoundException    The role specified was not found.
     */
    User create(SignupRequestDto user) throws UserEmailExistsException, UserNameExistsException, RoleNotFoundException;

    /**
     * Updates a user with given id and updated information.
     *
     * @param id   The id.
     * @param data The updated information.
     * @return The updated user.
     * @throws UserNotFoundException The user was not found.
     */
    User update(Long id, UserUpdateDto data) throws UserNotFoundException;

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
     * Updates a user with the given id and updated password.
     *
     * @param id   The id.
     * @param data The updated roles.
     * @return The updated user.
     * @throws UserNotFoundException The user was not found.
     */
    User updatePassword(Long id, UserPasswordDto data) throws UserNotFoundException;

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
