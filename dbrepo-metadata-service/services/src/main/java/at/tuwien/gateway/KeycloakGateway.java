package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.exception.*;

import java.util.UUID;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws AuthServiceConnectionException,
            CredentialsInvalidException, AccountNotSetupException;

    TokenDto refreshUserToken(String refreshToken) throws AuthServiceConnectionException,
            CredentialsInvalidException;

    /**
     * Creates a user at the Authentication Service with given credentials.
     *
     * @param data The user credentials.
     * @throws UserExistsException      The user already exists at the Authentication Service.
     * @throws EmailExistsException The user email already exists in the metadata database.
     */
    void createUser(UserCreateDto data) throws AuthServiceException, AuthServiceConnectionException,
            EmailExistsException, UserExistsException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param id The user id.
     */
    void deleteUser(UUID id) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException;

    /**
     * Update the credentials for a given user.
     *
     * @param id       The user id.
     * @param password The user credential.
     */
    void updateUserCredentials(UUID id, UserPasswordDto password) throws AuthServiceException,
            AuthServiceConnectionException, UserNotFoundException;

    /**
     * Finds a user in the metadata database by given username.
     *
     * @param username The user username.
     * @return The updated user.
     */
    UserDto findByUsername(String username) throws AuthServiceException, AuthServiceConnectionException,
            UserNotFoundException;

    UserDto findById(UUID id) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException;
}
