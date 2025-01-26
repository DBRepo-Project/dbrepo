package at.tuwien.utils;

import at.tuwien.api.auth.KeycloakErrorDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Log4j2
@Component
public class KeycloakUtils {

    private final RestTemplate keycloakRestTemplate;
    private final KeycloakConfig keycloakConfig;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public KeycloakUtils(@Qualifier("keycloakRestTemplate") RestTemplate keycloakRestTemplate, KeycloakConfig keycloakConfig,
                         KeycloakGateway keycloakGateway) {
        this.keycloakRestTemplate = keycloakRestTemplate;
        this.keycloakConfig = keycloakConfig;
        this.keycloakGateway = keycloakGateway;
    }

    public void createUser(UserCreateDto data) throws AuthServiceException, AuthServiceConnectionException,
            EmailExistsException, UserExistsException {
        final String path = "/admin/realms/dbrepo/users";
        log.trace("create user at endpoint {} with path {}", keycloakConfig.getKeycloakEndpoint(), path);
        final ResponseEntity<Void> response;
        try {
            response = keycloakRestTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(data), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to create user: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.Conflict e) {
            if (e.getResponseBodyAsByteArray() != null && e.getResponseBodyAsByteArray().length > 0) {
                final KeycloakErrorDto error = e.getResponseBodyAs(KeycloakErrorDto.class);
                if (error != null && error.getErrorMessage().contains("same email")) {
                    log.error("Failed to create user: email exists: {}", e.getMessage());
                    throw new EmailExistsException("E-Mail exists", e);
                }
            }
            log.error("Failed to create user: user exists: {}", e.getMessage());
            throw new UserExistsException("User exists", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user: unexpected status: {}", response.getStatusCode().value());
            throw new AuthServiceException("Unexpected status: " + response.getStatusCode().value());
        }
        log.debug("Created user {} at auth service", data.getUsername());
    }

    public void deleteUser(String username) throws AuthServiceException, AuthServiceConnectionException {
        try {
            final UUID userId = keycloakGateway.findByUsername(username).getId();
            keycloakGateway.deleteUser(userId);
        } catch (UserNotFoundException e) {
            /* ignore */
        }
    }
}
