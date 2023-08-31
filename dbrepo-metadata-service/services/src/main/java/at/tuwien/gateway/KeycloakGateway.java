package at.tuwien.gateway;

import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.exception.*;

import java.util.UUID;

public interface KeycloakGateway {

    void createUser(UserCreateDto data) throws AccessDeniedException, KeycloakRemoteException, UserAlreadyExistsException, UserEmailAlreadyExistsException;

    void updateUserCredentials(UUID id, UserPasswordDto password) throws AccessDeniedException,
            KeycloakRemoteException;

    UserDto findByUsername(String username) throws AccessDeniedException, UserNotFoundException,
            KeycloakRemoteException;
}
