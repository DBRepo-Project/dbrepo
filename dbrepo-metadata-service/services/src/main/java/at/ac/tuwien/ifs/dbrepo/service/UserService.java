package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.auth.CreateUserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;

import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Finds all users in the metadata database.
     *
     * @return The list of users.
     */
    List<UserDto> findAll();

    /**
     * Finds a user by username in the metadata database.
     *
     * @param username The username.
     * @return The user, if successfully.
     * @throws UserNotFoundException The user with this username was not found in the metadata database.
     */
    UserDto findByUsername(String username) throws UserNotFoundException;

    /**
     * Finds a specific user in the metadata database by given id.
     *
     * @param id The user id.
     * @return The user, if successful.
     * @throws UserNotFoundException The user was not found.
     */
    UserDto findById(UUID id) throws UserNotFoundException;

    /**
     * Updates the user information for a user with given id in the metadata database.
     *
     * @param user The user.
     * @param data The user information.
     * @return The user if successful. False otherwise.
     * @throws UserNotFoundException The user was not found.
     * @throws AuthServiceException  The auth service responded with an unexpected error code.
     */
    UserDto modify(UserDto user, UserUpdateDto data) throws UserNotFoundException, AuthServiceException;
}
