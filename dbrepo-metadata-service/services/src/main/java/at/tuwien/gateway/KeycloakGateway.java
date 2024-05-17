package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.exception.*;

import java.util.UUID;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws ServiceConnectionException,
            CredentialsInvalidException, AccountNotSetupException;

    TokenDto refreshUserToken(String refreshToken) throws ServiceConnectionException,
            CredentialsInvalidException;

    /**
     * Creates a user at the Authentication Service with given credentials.
     *
     * @param data The user credentials.
     * @throws UserExistsException      The user already exists at the Authentication Service.
     * @throws EmailExistsException The user email already exists in the metadata database.
     */
    void createUser(UserCreateDto data) throws ServiceException, ServiceConnectionException, EmailExistsException, UserExistsException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param id The user id.
     */
    void deleteUser(UUID id) throws ServiceException, ServiceConnectionException, UserNotFoundException;

    /**
     * Update the credentials for a given user.
     *
     * @param id       The user id.
     * @param password The user credential.
     */
    void updateUserCredentials(UUID id, UserPasswordDto password) throws ServiceException, ServiceConnectionException;

    /**
     * Finds a user in the metadata database by given username.
     *
     * @param username The user username.
     * @return The updated user.
     */
    UserDto findByUsername(String username) throws ServiceException, ServiceConnectionException, UserNotFoundException;

    UserDto findById(UUID id) throws ServiceException, ServiceConnectionException,
            UserNotFoundException;
}
