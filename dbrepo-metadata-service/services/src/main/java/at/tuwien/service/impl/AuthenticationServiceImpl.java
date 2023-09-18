package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Log4j2
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserMapper userMapper;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public AuthenticationServiceImpl(UserMapper userMapper, KeycloakGateway keycloakGateway) {
        this.userMapper = userMapper;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    public void create(SignupRequestDto data) throws KeycloakRemoteException, AccessDeniedException,
            UserEmailAlreadyExistsException, UserAlreadyExistsException {
        keycloakGateway.createUser(userMapper.signupRequestDtoToUserCreateDto(data));
    }

    @Override
    public void delete(UUID userId) throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException {
        keycloakGateway.deleteUser(userId);
    }

    @Override
    public UserDto findByUsername(String username) throws UserNotFoundException, KeycloakRemoteException,
            AccessDeniedException {
        return keycloakGateway.findByUsername(username);
    }

    @Override
    public void updatePassword(UUID id, UserPasswordDto data) throws KeycloakRemoteException, AccessDeniedException {
        keycloakGateway.updateUserCredentials(id, data);
    }

}
