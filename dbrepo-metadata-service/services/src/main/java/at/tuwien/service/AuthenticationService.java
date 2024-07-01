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
     * @return The user, if successful.
     * @throws UserExistsException        The user already exists at the auth database.
     * @throws AuthServiceException           The auth service responded with unexpected behavior.
     * @throws AuthServiceConnectionException The connection with the auth service could not be established.
     * @throws EmailExistsException       The user email already exists in the metadata database.
     */
    UserDto create(SignupRequestDto data) throws UserExistsException, AuthServiceException, AuthServiceConnectionException,
            EmailExistsException, CredentialsInvalidException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param user The user.
     * @throws AuthServiceException           The auth service responded with unexpected behavior.
     * @throws AuthServiceConnectionException The connection with the auth service could not be established.
     * @throws UserNotFoundException      The user was not found after creation in the auth database.
     */
    void delete(User user) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException, CredentialsInvalidException;

    /**
     * Finds a user with given username.
     *
     * @param username The username.
     * @return The user, if successful.
     * @throws AuthServiceException           The auth service responded with unexpected behavior.
     * @throws AuthServiceConnectionException The connection with the auth service could not be established.
     * @throws UserNotFoundException      The user was not found in the auth database.
     */
    UserDto findByUsername(String username) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException, CredentialsInvalidException;

    UserDto findById(UUID id) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException, CredentialsInvalidException;

    TokenDto obtainToken(LoginRequestDto data) throws AuthServiceConnectionException, CredentialsInvalidException, AccountNotSetupException;

    TokenDto refreshToken(String refreshToken) throws AuthServiceConnectionException, CredentialsInvalidException;

    /**
     * Updates the password of a user with given id.
     *
     * @param user The user.
     * @param data The new password.
     * @throws AuthServiceException           The auth service responded with unexpected behavior.
     * @throws AuthServiceConnectionException The connection with the auth service could not be established.
     */
    void updatePassword(User user, UserPasswordDto data) throws AuthServiceException, AuthServiceConnectionException, CredentialsInvalidException;
}
