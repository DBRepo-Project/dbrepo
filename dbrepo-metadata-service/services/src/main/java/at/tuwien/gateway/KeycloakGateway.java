package at.tuwien.gateway;

import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.KeycloakRemoteException;
import at.tuwien.exception.UserNotFoundException;

import java.util.UUID;

public interface KeycloakGateway {

    void createUser(UserCreateDto data) throws AccessDeniedException, KeycloakRemoteException;

    void updateUserCredentials(UUID id, UserPasswordDto password) throws AccessDeniedException,
            KeycloakRemoteException;

    UserDto findByUsername(String username) throws AccessDeniedException, UserNotFoundException,
            KeycloakRemoteException;
}
