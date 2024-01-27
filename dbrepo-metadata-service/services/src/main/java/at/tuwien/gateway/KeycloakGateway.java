package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.exception.*;

import java.util.UUID;

public interface KeycloakGateway {

    TokenDto obtainUserToken(String username, String password) throws AccessDeniedException, KeycloakRemoteException;

    /**
     * Creates a user at the Authentication Service with given credentials.
     *
     * @param data The user credentials.
     * @throws AccessDeniedException           The admin token could not be obtained.
     * @throws KeycloakRemoteException         The Authentication Service was not able to respond within the 3s timeout.
     * @throws UserAlreadyExistsException      The user already exists at the Authentication Service.
     * @throws UserEmailAlreadyExistsException The user email already exists in the metadata database.
     */
    void createUser(UserCreateDto data) throws AccessDeniedException, KeycloakRemoteException, UserAlreadyExistsException, UserEmailAlreadyExistsException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param id The user id.
     * @throws KeycloakRemoteException The Authentication Service was not able to respond within the 3s timeout.
     * @throws AccessDeniedException   The admin token could not be obtained.
     * @throws UserNotFoundException   The user was not found at the Authentication Service.
     */
    void deleteUser(UUID id) throws KeycloakRemoteException, AccessDeniedException, UserNotFoundException;

    /**
     * Update the credentials for a given user.
     *
     * @param id       The user id.
     * @param password The user credential.
     * @throws AccessDeniedException   The admin token could not be obtained.
     * @throws KeycloakRemoteException The Authentication Service was not able to respond within the 3s timeout.
     */
    void updateUserCredentials(UUID id, UserPasswordDto password) throws AccessDeniedException,
            KeycloakRemoteException;

    /**
     * Finds a user in the metadata database by given username.
     *
     * @param username The user username.
     * @return The updated user.
     * @throws AccessDeniedException   The admin token could not be obtained.
     * @throws UserNotFoundException   The user was not found,
     * @throws KeycloakRemoteException The Authentication Service was not able to respond within the 3s timeout.
     */
    UserDto findByUsername(String username) throws AccessDeniedException, UserNotFoundException,
            KeycloakRemoteException;
}
