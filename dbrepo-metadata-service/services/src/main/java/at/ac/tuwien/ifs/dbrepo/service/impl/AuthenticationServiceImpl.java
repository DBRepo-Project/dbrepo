package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
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

}
