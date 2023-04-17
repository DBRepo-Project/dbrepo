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
     * Finds all users
     *
     * @return The list of users.
     */
    List<User> findAll();

    User create(SignupRequestDto data, Realm realm, Role role) throws RemoteUnavailableException, UserNotFoundException,
            UserAlreadyExistsException;

    User modify(UUID id, UserUpdateDto data, Principal principal) throws UserNotFoundException, ForeignUserException, UserAttributeNotFoundException;

    User updatePassword(UUID id, UserPasswordDto data, Principal principal) throws UserNotFoundException,
            ForeignUserException;

    User toggleTheme(UUID id, UserThemeSetDto data, Principal principal) throws UserNotFoundException, ForeignUserException, UserAttributeNotFoundException;


    User find(UUID id) throws UserNotFoundException;
}
