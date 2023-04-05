package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.entities.auth.Realm;
import at.tuwien.entities.user.User;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.UserAlreadyExistsException;
import at.tuwien.exception.UserNotFoundException;

import java.util.List;

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

    User create(SignupRequestDto data, Realm realm) throws RemoteUnavailableException, UserNotFoundException, UserAlreadyExistsException;

    User find(String id) throws UserNotFoundException;
}
