package at.tuwien.service;

import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import java.util.UUID;

public interface AuthenticationService {

    /**
     * Create a user at the Authentication Service with given credentials.
     *
     * @param data The credentials.
     * @throws UserExistsException        The user already exists at the auth database.
     * @throws ServiceException           The auth service responded with unexpected behavior.
     * @throws ServiceConnectionException The connection with the auth service could not be established.
     * @throws EmailExistsException       The user email already exists in the metadata database.
     */
    void create(SignupRequestDto data) throws UserExistsException, ServiceException, ServiceConnectionException,
            EmailExistsException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param user The user.
     * @throws ServiceException           The auth service responded with unexpected behavior.
     * @throws ServiceConnectionException The connection with the auth service could not be established.
     * @throws UserNotFoundException      The user was not found after creation in the auth database.
     */
    void delete(User user) throws ServiceException, ServiceConnectionException, UserNotFoundException;

    /**
     * Finds a user with given username.
     *
     * @param username The username.
     * @return The user, if successful.
     * @throws ServiceException           The auth service responded with unexpected behavior.
     * @throws ServiceConnectionException The connection with the auth service could not be established.
     * @throws UserNotFoundException      The user was not found in the auth database.
     */
    UserDto findByUsername(String username) throws ServiceException, ServiceConnectionException, UserNotFoundException;

    UserDto findById(UUID id) throws ServiceException, ServiceConnectionException, UserNotFoundException;

    TokenDto obtainToken(LoginRequestDto data) throws ServiceConnectionException, CredentialsInvalidException, AccountNotSetupException;

    TokenDto refreshToken(String refreshToken) throws ServiceConnectionException, CredentialsInvalidException;

    /**
     * Updates the password of a user with given id.
     *
     * @param user The user.
     * @param data The new password.
     * @throws ServiceException           The auth service responded with unexpected behavior.
     * @throws ServiceConnectionException The connection with the auth service could not be established.
     */
    void updatePassword(User user, UserPasswordDto data) throws ServiceException, ServiceConnectionException;
}
