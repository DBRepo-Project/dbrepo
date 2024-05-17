package at.tuwien.gateway.impl;

import at.tuwien.api.auth.KeycloakErrorDto;
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

    public KeycloakGatewayImpl(UserMapper userMapper, @Qualifier("keycloakRestTemplate") RestTemplate restTemplate,
                               KeycloakConfig keycloakConfig) {
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
        this.keycloakConfig = keycloakConfig;
    }

    public TokenDto obtainToken() throws ServiceConnectionException, ServiceException {
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
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.BadGateway e) {
            log.error("Failed to obtain admin token: {}", e.getMessage());
            throw new ServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to obtain admin token: remote host answered unexpected: {}", e.getMessage(), e);
            throw new ServiceException("Authentication service answered unexpected: " + e.getMessage(), e);
        }
        return response.getBody();
    }

    @Override
    public TokenDto obtainUserToken(String username, String password) throws ServiceConnectionException,
            CredentialsInvalidException, AccountNotSetupException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", username);
        payload.add("password", password);
        payload.add("grant_type", "password");
        payload.add("scope", "openid roles");
        payload.add("client_id", keycloakConfig.getKeycloakClient());
        payload.add("client_secret", keycloakConfig.getKeycloakClientSecret());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/realms/dbrepo/protocol/openid-connect/token";
        log.trace("request user token from url: {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = new RestTemplate()
                    .exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new ServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.BadRequest e) {
            final KeycloakErrorDto error = e.getResponseBodyAs(KeycloakErrorDto.class);
            if (error != null && error.getError().equals("invalid_grant")) {
                log.error("Failed to obtain user token: {}", error.getErrorDescription());
                throw new AccountNotSetupException(error.getErrorDescription());
            }
            log.error("Failed to obtain user token: bad request");
            throw new CredentialsInvalidException("Failed to obtain user token: bad request");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to obtain user token: invalid credentials");
            throw new CredentialsInvalidException("Invalid credentials", e);
        }
        return response.getBody();
    }

    @Override
    public TokenDto refreshUserToken(String refreshToken) throws ServiceConnectionException,
            CredentialsInvalidException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("refresh_token", refreshToken);
        payload.add("grant_type", "refresh_token");
        payload.add("client_id", keycloakConfig.getKeycloakClient());
        payload.add("client_secret", keycloakConfig.getKeycloakClientSecret());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/realms/dbrepo/protocol/openid-connect/token";
        log.trace("request user token from url: {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = new RestTemplate()
                    .exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to refresh user token: {}", e.getMessage());
            throw new ServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to refresh user token: invalid credentials");
            throw new CredentialsInvalidException("Invalid credentials", e);
        } catch (HttpClientErrorException.BadRequest e) {
            if (e.getMessage().contains("Session not active")) {
                log.error("Failed to refresh user token: inactive session", e);
                throw new CredentialsInvalidException("Failed to refresh user token: inactive session", e);
            }
            log.error("Failed to refresh user token: remote host answered unexpected: {}", e.getMessage(), e);
            throw new CredentialsInvalidException("Authentication service answered unexpected: " + e.getMessage(), e);
        }
        return response.getBody();
    }

    @Override
    public void createUser(UserCreateDto data) throws ServiceException, ServiceConnectionException,
            EmailExistsException, UserExistsException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users";
        log.debug("create user at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable |
                 HttpServerErrorException.BadGateway e) {
            log.error("Failed to create user: {}", e.getMessage());
            throw new ServiceConnectionException("Service unavailable");
        } catch (HttpClientErrorException.Conflict e) {
            if (e.getMessage().contains("same email")) {
                log.error("Failed to create user: email exists: {}", e.getMessage());
                throw new EmailExistsException("E-Mail exists");
            } else {
                log.error("Failed to create user: user exists: {}", e.getMessage());
                throw new UserExistsException("User exists");
            }
        }
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user: unexpected status: {}", response.getStatusCode().value());
            throw new ServiceException("Failed to create user: unexpected status: " + response.getStatusCode().value());
        }
        log.debug("Created user {} at auth service", data.getUsername());
    }

    @Override
    public void deleteUser(UUID id) throws ServiceException, ServiceConnectionException, UserNotFoundException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("delete user at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to delete user: {}", e.getMessage());
            throw new ServiceConnectionException("Service unavailable");
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete user: user not found: {}", e.getMessage());
            throw new UserNotFoundException("User not found");
        } catch (Exception e) {
            log.error("Failed to delete user: unexpected response: {}", e.getMessage());
            throw new ServiceException("Unexpected result", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to delete user: unexpected response");
            throw new ServiceException("Unexpected result");
        }
        log.info("Deleted user {} at auth service", id);
    }

    @Override
    public void updateUserCredentials(UUID id, UserPasswordDto data) throws ServiceException,
            ServiceConnectionException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final UpdateCredentialsDto payload = userMapper.passwordToUpdateCredentialsDto(data.getPassword());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("update user credentials at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to update user credentials: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to update user credentials: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update user: unexpected response: {}", e.getMessage());
            throw new ServiceException("Unexpected result", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to update user: unexpected status: {}", response.getStatusCode().value());
            throw new ServiceException("Failed to update user: unexpected status: " + response.getStatusCode().value());
        }
        log.info("Updated user {} password at auth service", id);
    }

    @Override
    public UserDto findByUsername(String username) throws ServiceException, ServiceConnectionException,
            UserNotFoundException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/?username=" + username;
        log.debug("find user from url {}", url);
        final ResponseEntity<UserDto[]> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDto[].class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to find user: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to find user: unexpected response: {}", e.getMessage());
            throw new ServiceException("Unexpected result", e);
        }
        final UserDto[] body = response.getBody();
        if (body == null || body.length != 1) {
            log.error("Failed to find user with username {}", username);
            throw new UserNotFoundException("Failed to find user with username " + username);
        }
        return body[0];
    }

    @Override
    public UserDto findById(UUID id) throws ServiceException, ServiceConnectionException,
            UserNotFoundException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("find user from url {}", url);
        final ResponseEntity<UserDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new ServiceConnectionException("Service unavailable");
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find user: not found: {}", e.getMessage());
            throw new UserNotFoundException("User not found");
        } catch (Exception e) {
            log.error("Failed to find user: unexpected response: {}", e.getMessage());
            throw new ServiceException("Unexpected result", e);
        }
        return response.getBody();
    }

}
