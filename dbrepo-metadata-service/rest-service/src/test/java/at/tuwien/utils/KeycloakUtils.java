package at.tuwien.utils;

import at.tuwien.api.keycloak.RoleRepresentationDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.gateway.impl.KeycloakGatewayImpl;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Log4j2
@Component
public class KeycloakUtils {

    final static UUID realmId = UUID.fromString("82c39861-d877-4667-a0f3-4daa2ee230e0");

    private final KeycloakGateway keycloakGateway;

    @Autowired
    public KeycloakUtils(KeycloakGateway keycloakGateway) {
        this.keycloakGateway = keycloakGateway;
    }

    public void deleteUser(String username) throws AuthServiceException, AuthServiceConnectionException,
            CredentialsInvalidException {
        try {
            final UUID userId = keycloakGateway.findByUsername(username).getId();
            keycloakGateway.deleteUser(userId);
        } catch (UserNotFoundException e) {
            /* ignore */
        }
    }
}
