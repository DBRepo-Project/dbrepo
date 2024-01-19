package at.tuwien.service;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.exception.*;

import java.util.UUID;

public interface AuthenticationService {

    /**
     * Create a user at the Authentication Service with given credentials.
     *
     * @param data The credentials.
     * @throws AccessDeniedException           The admin token could not be obtained.
     * @throws KeycloakRemoteException         The Authentication Service was not able to respond within the 3s timeout.
     * @throws UserAlreadyExistsException      The user already exists at the Authentication Service.
     * @throws UserEmailAlreadyExistsException The user email already exists in the metadata database.
     */
    void create(SignupRequestDto data) throws KeycloakRemoteException, AccessDeniedException,
            UserEmailAlreadyExistsException, UserAlreadyExistsException;

    /**
     * Deletes a user at the Authentication Service with given user id.
     *
     * @param userId The user id.
     * @throws KeycloakRemoteException The Authentication Service was not able to respond within the 3s timeout.
     * @throws AccessDeniedException   The admin token could not be obtained.
     * @throws UserNotFoundException   The user was not found at the Authentication Service.
     */
    void delete(UUID userId) throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException;

    /**
     * Finds a user with given username.
     *
     * @param username The username.
     * @return The user, if successful.
     * @throws UserNotFoundException   The user was not found at the Authentication Service.
     * @throws KeycloakRemoteException The Authentication Service was not able to respond within the 3s timeout.
     * @throws AccessDeniedException   The admin token could not be obtained.
     */
    UserDto findByUsername(String username) throws UserNotFoundException, KeycloakRemoteException,
            AccessDeniedException;

    /**
     * Updates the password of a user with given id.
     *
     * @param id   The user id.
     * @param data The new password.
     * @throws KeycloakRemoteException The Authentication Service was not able to respond within the 3s timeout.
     * @throws AccessDeniedException   The admin token could not be obtained.
     */
    void updatePassword(UUID id, UserPasswordDto data) throws KeycloakRemoteException, AccessDeniedException;
}
