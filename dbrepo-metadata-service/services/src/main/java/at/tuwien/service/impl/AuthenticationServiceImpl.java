package at.tuwien.service.impl;

import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.AuthenticationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Log4j2
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final MetadataMapper metadataMapper;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public AuthenticationServiceImpl(MetadataMapper metadataMapper, KeycloakGateway keycloakGateway) {
        this.metadataMapper = metadataMapper;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    public UserDto create(SignupRequestDto data) throws UserExistsException, AuthServiceException,
            AuthServiceConnectionException, EmailExistsException, CredentialsInvalidException {
        keycloakGateway.createUser(metadataMapper.signupRequestDtoToUserCreateDto(data));
        try {
            return findByUsername(data.getUsername());
        } catch (UserNotFoundException e) {
            throw new AuthServiceException("Failed to find user in auth service", e);
        }
    }

    @Override
    public void delete(User user) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException,
            CredentialsInvalidException {
        final UserDto keycloakUser = findByUsername(user.getUsername());
        keycloakGateway.deleteUser(keycloakUser.getId());
    }

    @Override
    public UserDto findByUsername(String username) throws AuthServiceException, AuthServiceConnectionException,
            UserNotFoundException, CredentialsInvalidException {
        return keycloakGateway.findByUsername(username);
    }

    @Override
    public UserDto findById(UUID id) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException,
            CredentialsInvalidException {
        return keycloakGateway.findById(id);
    }

    @Override
    public TokenDto obtainToken(LoginRequestDto data) throws AuthServiceConnectionException,
            CredentialsInvalidException, AccountNotSetupException {
        return keycloakGateway.obtainUserToken(data.getUsername(), data.getPassword());
    }

    @Override
    public TokenDto refreshToken(String refreshToken) throws AuthServiceConnectionException,
            CredentialsInvalidException {
        return keycloakGateway.refreshUserToken(refreshToken);
    }

    @Override
    public void updatePassword(User user, UserPasswordDto data) throws AuthServiceException,
            AuthServiceConnectionException, CredentialsInvalidException, UserNotFoundException {
        final UserDto keycloakUser = findByUsername(user.getUsername());
        keycloakGateway.updateUserCredentials(keycloakUser.getId(), data);
    }

}
