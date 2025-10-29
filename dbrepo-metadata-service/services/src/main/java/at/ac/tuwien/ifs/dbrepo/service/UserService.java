package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;

import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Finds all users in the auth service.
     *
     * @return The list of users.
     */
    List<UserDto> findAll();

    /**
     * Finds a user by username in the auth service.
     *
     * @param username The username.
     * @return The user, if successfully.
     * @throws UserNotFoundException The user with this username was not found in the auth service.
     * @throws NotAllowedException   The user with this username is an internal user and not allowed to be found.
     */
    UserDto findByUsername(String username) throws UserNotFoundException, NotAllowedException;

    /**
     * Finds a specific user in the auth service by given id.
     *
     * @param id The user id.
     * @return The user, if successful.
     * @throws UserNotFoundException The user was not found.
     * @throws NotAllowedException   The user with this username is an internal user and not allowed to be found.
     */
    UserDto findById(UUID id) throws UserNotFoundException, NotAllowedException;

    /**
     * Updates the user information for a user with given id in the auth service.
     *
     * @param user The user.
     * @param data The user information.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException The user was not found.
     * @throws AuthServiceException  The auth service responded with an unexpected error code.
     */
    UserDto modify(UserDto user, UserUpdateDto data) throws UserNotFoundException, AuthServiceException;
}
