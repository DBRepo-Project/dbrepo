package at.tuwien.service.impl;

import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.CredentialsInvalidException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.service.AuthenticationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final KeycloakGateway keycloakGateway;

    @Autowired
    public AuthenticationServiceImpl(KeycloakGateway keycloakGateway) {
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    public void delete(User user) throws AuthServiceException, UserNotFoundException {
        keycloakGateway.deleteUser(user.getKeycloakId());
    }

    @Override
    public void updatePassword(User user, UserPasswordDto data) throws UserNotFoundException {
        keycloakGateway.updateUserCredentials(user.getKeycloakId(), data);
    }

}
