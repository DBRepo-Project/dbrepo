package at.tuwien.gateway.impl;

import at.tuwien.api.keycloak.*;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.KeycloakRemoteException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.UserMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final KeycloakConfig keycloakConfig;

    public KeycloakGatewayImpl(UserMapper userMapper, KeycloakConfig keycloakConfig) {
        this.userMapper = userMapper;
        this.restTemplate = new RestTemplate();
        this.keycloakConfig = keycloakConfig;
    }

    public TokenDto obtainToken() throws AccessDeniedException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", keycloakConfig.getUsername());
        payload.add("password", keycloakConfig.getPassword());
        payload.add("grant_type", "password");
        payload.add("client_id", "admin-cli");
        final ResponseEntity<TokenDto> response;
        try {
            response = restTemplate.exchange("/api/auth/realms/master/protocol/openid-connect/token",
                    HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain admin token: {}", e.getMessage());
            throw new AccessDeniedException("Failed to obtain admin token: " + e.getMessage());
        }
        return response.getBody();
    }

    @Override
    public void createUser(UserCreateDto data) throws AccessDeniedException, KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange("/api/auth/admin/realms/dbrepo/users", HttpMethod.POST,
                    new HttpEntity<>(data, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to create user: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to create user: " + e.getMessage());
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user: status {} was not expected", response.getStatusCode().value());
            throw new KeycloakRemoteException("Failed to create user: status " + response.getStatusCode().value() + "was not expected");
        }
    }

    @Override
    public void updateUserAttributes(UUID id, UserAttributesDto data) throws AccessDeniedException,
            KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final UpdateAttributesDto payload = userMapper.userAttributesDtoToUpdateAttributesDto(data);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange("/api/auth/admin/realms/dbrepo/users/" + id, HttpMethod.PUT,
                    new HttpEntity<>(payload, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to update user attributes: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to update user attributes: " + e.getMessage());
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update user attributes: status {} was not expected", response.getStatusCode().value());
            throw new KeycloakRemoteException("Failed to update user attributes: status " + response.getStatusCode().value() + "was not expected");
        }
    }

    @Override
    public void updateUserCredentials(UUID id, UserPasswordDto data) throws AccessDeniedException,
            KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final UpdateCredentialsDto payload = userMapper.passwordToUpdateCredentialsDto(data.getPassword());
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange("/api/auth/admin/realms/dbrepo/users/" + id, HttpMethod.PUT,
                    new HttpEntity<>(payload, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to update user credentials: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to update user credentials: " + e.getMessage());
        }
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update user credentials: status {} was not expected", response.getStatusCode().value());
            throw new KeycloakRemoteException("Failed to update user credentials: status " + response.getStatusCode().value() + "was not expected");
        }
    }

    @Override
    public UserDto findByUsername(String username) throws AccessDeniedException, UserNotFoundException,
            KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final ResponseEntity<UserDto[]> response;
        try {
            response = restTemplate.exchange("/api/auth/admin/realms/dbrepo/users/?username=" + username,
                    HttpMethod.GET, new HttpEntity<>(null, headers), UserDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to find user: " + e.getMessage());
        }
        final UserDto[] body = response.getBody();
        if (body == null || body.length != 1) {
            log.error("Failed to find user with username {}: response is not exactly 1 but is {}", username, body.length);
            throw new UserNotFoundException("Failed to find user with username " + username);
        }
        return body[0];
    }

    @Override
    public UserDto findByEmail(String email) throws AccessDeniedException, UserNotFoundException,
            KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final ResponseEntity<UserDto[]> response;
        try {
            response = restTemplate.exchange("/api/auth/admin/realms/dbrepo/users/?email=" + email,
                    HttpMethod.GET, new HttpEntity<>(null, headers), UserDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to find user: " + e.getMessage());
        }
        final UserDto[] body = response.getBody();
        if (body == null || body.length != 1) {
            log.error("Failed to find user with email {}: response is not exactly 1 but is {}", email, body.length);
            throw new UserNotFoundException("Failed to find user with email " + email);
        }
        return body[0];
    }

    @Override
    public UserDto findById(UUID id) throws AccessDeniedException, UserNotFoundException, KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final ResponseEntity<UserDto> response;
        try {
            response = restTemplate.exchange("/api/auth/admin/realms/dbrepo/users/" + id, HttpMethod.GET,
                    new HttpEntity<>(null, headers), UserDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to find user: " + e.getMessage());
        }
        return response.getBody();
    }

    @Override
    public List<UserDto> findAllUsers() throws AccessDeniedException, KeycloakRemoteException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final ResponseEntity<UserDto[]> response;
        try {
            response = restTemplate.exchange("/api/auth/admin/realms/dbrepo/users/", HttpMethod.GET,
                    new HttpEntity<>(null, headers), UserDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find users: {}", e.getMessage());
            throw new KeycloakRemoteException("Failed to find users: " + e.getMessage());
        }
        final UserDto[] body = response.getBody();
        if (body == null) {
            log.error("Failed to find users: body is empty");
            throw new KeycloakRemoteException("Failed to find users: body is empty");
        }
        return Arrays.asList(body);
    }

}
