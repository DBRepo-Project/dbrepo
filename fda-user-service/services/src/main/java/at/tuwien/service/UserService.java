package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserThemeSetDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.auth.Realm;
import at.tuwien.entities.user.Role;
import at.tuwien.entities.user.User;
import at.tuwien.exception.ForeignUserException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.UserAlreadyExistsException;
import at.tuwien.exception.UserNotFoundException;

import java.security.Principal;
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

    User findById(String id) throws UserNotFoundException;

    User create(SignupRequestDto data, Realm realm, Role role) throws RemoteUnavailableException, UserNotFoundException,
            UserAlreadyExistsException;

    User modify(String id, UserUpdateDto data, Principal principal) throws UserNotFoundException, ForeignUserException;

    User updatePassword(String id, UserPasswordDto data, Principal principal) throws UserNotFoundException,
            ForeignUserException;

    User toggleTheme(String id, UserThemeSetDto data, Principal principal) throws UserNotFoundException, ForeignUserException;

    User find(String id) throws UserNotFoundException;
}
