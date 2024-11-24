package at.tuwien.config;

import at.tuwien.api.auth.KeycloakErrorDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.EmailExistsException;
import at.tuwien.exception.UserExistsException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Getter
@Configuration
public class KeycloakConfig {

    @Value("${dbrepo.endpoints.keycloak}")
    private String keycloakEndpoint;

    @Autowired
    @Qualifier("keycloakRestTemplate")
    private RestTemplate keycloakRestTemplate;

    public Boolean existsByUsername(String username) throws AuthServiceException, AuthServiceConnectionException {
        final String path = "/admin/realms/dbrepo/users/?username=" + username;
        final ResponseEntity<UserDto[]> response;
        try {
            response = keycloakRestTemplate.exchange(path, HttpMethod.GET, HttpEntity.EMPTY, UserDto[].class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (Exception e) {
            log.error("Failed to find user: unexpected response: {}", e.getMessage());
            throw new AuthServiceException("Unexpected result", e);
        }
        final UserDto[] body = response.getBody();
        if (body == null || body.length != 1) {
            log.error("Failed to find user with username {}", username);
            return false;
        }
        return true;
    }

    public void createUser(UserCreateDto data) throws UserExistsException, EmailExistsException,
            AuthServiceConnectionException, AuthServiceException {
        final String path = "/admin/realms/dbrepo/users";
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

}
