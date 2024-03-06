package at.tuwien.gateway.impl;

import at.tuwien.api.keycloak.*;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.UserMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Log4j2
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final KeycloakConfig keycloakConfig;

    public KeycloakGatewayImpl(UserMapper userMapper, @Qualifier("keycloakRestTemplate") RestTemplate restTemplate, KeycloakConfig keycloakConfig) {
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
        this.keycloakConfig = keycloakConfig;
    }

    public TokenDto obtainToken() throws AccessDeniedException, KeycloakRemoteException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", keycloakConfig.getKeycloakUsername());
        payload.add("password", keycloakConfig.getKeycloakPassword());
        payload.add("grant_type", "password");
        payload.add("client_id", "admin-cli");
        final String url = keycloakConfig.getKeycloakEndpoint() + "/realms/master/protocol/openid-connect/token";
        log.debug("request admin token from url {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain admin token: {}", e.getMessage());
            throw new AccessDeniedException("Failed to obtain admin token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to obtain admin token: remote host answered unexpected: {}", e.getMessage(), e);
            throw new KeycloakRemoteException("Failed to obtain admin token: remote host answered unexpected: " + e.getMessage(), e);
        }
        return response.getBody();
    }

    @Override
    public TokenDto obtainUserToken(String username, String password) throws AccessDeniedException, KeycloakRemoteException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", username);
        payload.add("password", password);
        payload.add("grant_type", "password");
        payload.add("scope", "openid roles attributes");
        payload.add("client_id", "dbrepo-client");
        payload.add("client_secret", keycloakConfig.getKeycloakClientSecret());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/realms/dbrepo/protocol/openid-connect/token";
        log.debug("request user token from url {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = new RestTemplate()
                    .exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new AccessDeniedException("Failed to obtain user token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to obtain user token: remote host answered unexpected: {}", e.getMessage(), e);
            throw new KeycloakRemoteException("Failed to obtain user token: remote host answered unexpected: " + e.getMessage(), e);
        }
        return response.getBody();
    }

    @Override
    public void createUser(UserCreateDto data) throws AccessDeniedException, KeycloakRemoteException,
            UserAlreadyExistsException, UserEmailAlreadyExistsException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users";
        log.debug("create user at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to create user: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to create user: " + e.getMessage());
        } catch (HttpClientErrorException.Conflict e) {
            if (e.getMessage().contains("same email")) {
                log.error("Conflict when creating user: {}", e.getMessage());
                throw new UserEmailAlreadyExistsException("Conflict when creating user: " + e.getMessage());
            } else {
                log.error("Conflict when creating user: {}", e.getMessage());
                throw new UserAlreadyExistsException("Conflict when creating user: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to create user: remote host answered unexpected: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to create user: remote host answered unexpected: " + e.getMessage(), e);
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user: status {} was not expected", response.getStatusCode().value());
            throw new KeycloakRemoteException("Failed to create user: status " + response.getStatusCode().value() + "was not expected");
        }
        log.info("Created user {} at authentication service", data.getUsername());
    }

    @Override
    public void deleteUser(UUID id) throws KeycloakRemoteException, AccessDeniedException, UserNotFoundException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("delete user at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to delete user: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to delete user: " + e.getMessage());
        } catch (HttpClientErrorException.NotFound e) {
            log.error("User does not exist: {}", e.getMessage());
            throw new UserNotFoundException("User does not exist: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete user: remote host answered unexpected: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to delete user: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to delete user: status {} was not expected", response.getStatusCode().value());
            throw new KeycloakRemoteException("Failed to delete user: status " + response.getStatusCode().value() + "was not expected");
        }
        log.info("Deleted user {} at authentication service", id);
    }

    @Override
    public void updateUserCredentials(UUID id, UserPasswordDto data) throws AccessDeniedException,
            KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final UpdateCredentialsDto payload = userMapper.passwordToUpdateCredentialsDto(data.getPassword());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("update user credentials at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to update user credentials: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to update user credentials: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create user: remote host answered unexpected: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to create user: remote host answered unexpected", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to update user credentials: status {} was not expected", response.getStatusCode().value());
            throw new KeycloakRemoteException("Failed to update user credentials: status " + response.getStatusCode().value() + "was not expected");
        }
        log.info("Updated user {} password at authentication service", id);
    }

    @Override
    public UserDto findByUsername(String username) throws AccessDeniedException, UserNotFoundException,
            KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/?username=" + username;
        log.debug("find user from url {}", url);
        final ResponseEntity<UserDto[]> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to find user: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to create user: remote host answered unexpected: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to create user: remote host answered unexpected: " + e.getMessage(), e);
        }
        final UserDto[] body = response.getBody();
        if (body == null || body.length != 1) {
            log.error("Failed to find user with username {}: response is not exactly 1 but is {}", username, body.length);
            throw new UserNotFoundException("Failed to find user with username " + username);
        }
        return body[0];
    }

}
