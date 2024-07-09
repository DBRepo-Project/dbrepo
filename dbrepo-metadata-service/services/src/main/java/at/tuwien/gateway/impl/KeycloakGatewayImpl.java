package at.tuwien.gateway.impl;

import at.tuwien.api.auth.KeycloakErrorDto;
import at.tuwien.api.keycloak.*;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.MetadataMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Log4j2
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final RestTemplate restTemplate;
    private final RestTemplate keycloakRestTemplate;
    private final KeycloakConfig keycloakConfig;
    private final MetadataMapper metadataMapper;

    public KeycloakGatewayImpl(@Qualifier("restTemplate") RestTemplate restTemplate,
                               @Qualifier("keycloakRestTemplate") RestTemplate keycloakRestTemplate,
                               KeycloakConfig keycloakConfig, MetadataMapper metadataMapper) {
        this.restTemplate = restTemplate;
        this.keycloakRestTemplate = keycloakRestTemplate;
        this.keycloakConfig = keycloakConfig;
        this.metadataMapper = metadataMapper;
    }

    public TokenDto obtainToken() throws AuthServiceConnectionException, AuthServiceException,
            CredentialsInvalidException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", keycloakConfig.getKeycloakUsername());
        payload.add("password", keycloakConfig.getKeycloakPassword());
        payload.add("grant_type", "password");
        payload.add("client_id", "admin-cli");
        final String url = keycloakConfig.getKeycloakEndpoint() + "/realms/master/protocol/openid-connect/token";
        log.trace("request admin token from url: {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = keycloakRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to obtain admin token: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to obtain admin token: invalid credentials: {}", e.getMessage(), e);
            throw new CredentialsInvalidException("Invalid credentials: " + e.getMessage(), e);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("Failed to obtain admin token: unexpected response: {}", e.getMessage(), e);
            throw new AuthServiceException("Unexpected response: " + e.getMessage(), e);
        }
        return response.getBody();
    }

    @Override
    public TokenDto obtainUserToken(String username, String password) throws AuthServiceConnectionException,
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
        log.trace("request admin token from url: {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.BadRequest e) {
            final KeycloakErrorDto error = e.getResponseBodyAs(KeycloakErrorDto.class);
            if (error != null && error.getError().equals("invalid_grant")) {
                log.error("Failed to obtain user token: {}", error.getErrorDescription());
                throw new AccountNotSetupException(error.getErrorDescription());
            }
            log.error("Failed to obtain user token: bad request");
            throw new CredentialsInvalidException("Bad request", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to obtain user token: invalid credentials");
            throw new CredentialsInvalidException("Invalid credentials", e);
        }
        return response.getBody();
    }

    @Override
    public TokenDto refreshUserToken(String refreshToken) throws AuthServiceConnectionException,
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
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to refresh user token: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to refresh user token: invalid credentials");
            throw new CredentialsInvalidException("Invalid credentials", e);
        } catch (HttpClientErrorException.BadRequest e) {
            if (e.getMessage() != null && e.getMessage().contains("Session not active")) {
                log.error("Failed to refresh user token: inactive session", e);
                throw new CredentialsInvalidException("Inactive session", e);
            }
            log.error("Failed to refresh user token: unexpected response: {}", e.getMessage(), e);
            throw new CredentialsInvalidException("Unexpected response: " + e.getMessage(), e);
        }
        return response.getBody();
    }

    @Override
    public void createUser(UserCreateDto data) throws AuthServiceException, AuthServiceConnectionException,
            EmailExistsException, UserExistsException, CredentialsInvalidException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users";
        log.debug("create user at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = keycloakRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(data, headers), Void.class);
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

    @Override
    public void deleteUser(UUID id) throws AuthServiceException, AuthServiceConnectionException, UserNotFoundException,
            CredentialsInvalidException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("delete user at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = keycloakRestTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(null, headers), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to delete user: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to delete user: user not found: {}", e.getMessage());
            throw new UserNotFoundException("User not found", e);
        } catch (Exception e) {
            log.error("Failed to delete user: unexpected response: {}", e.getMessage());
            throw new AuthServiceException("Unexpected result", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to delete user: unexpected response");
            throw new AuthServiceException("Unexpected result");
        }
        log.info("Deleted user {} at auth service", id);
    }

    @Override
    public void updateUserCredentials(UUID id, UserPasswordDto data) throws AuthServiceException,
            AuthServiceConnectionException, CredentialsInvalidException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final UpdateCredentialsDto payload = metadataMapper.passwordToUpdateCredentialsDto(data.getPassword());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("update user credentials at url {}", url);
        final ResponseEntity<Void> response;
        try {
            response = keycloakRestTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), Void.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to update user credentials: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (Exception e) {
            log.error("Failed to update user: unexpected response: {}", e.getMessage());
            throw new AuthServiceException("Unexpected result", e);
        }
        if (!response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            log.error("Failed to update user: unexpected status: {}", response.getStatusCode().value());
            throw new AuthServiceException("Unexpected status: " + response.getStatusCode().value());
        }
        log.info("Updated user {} password at auth service", id);
    }

    @Override
    public UserDto findByUsername(String username) throws AuthServiceException, AuthServiceConnectionException,
            UserNotFoundException, CredentialsInvalidException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/?username=" + username;
        log.debug("find user from url {}", url);
        final ResponseEntity<UserDto[]> response;
        try {
            response = keycloakRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDto[].class);
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
            throw new UserNotFoundException("Failed to find user with username " + username);
        }
        return body[0];
    }

    @Override
    public UserDto findById(UUID id) throws AuthServiceException, AuthServiceConnectionException,
            UserNotFoundException, CredentialsInvalidException {
        /* obtain admin token */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainToken().getAccessToken());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/admin/realms/dbrepo/users/" + id;
        log.debug("find user from url {}", url);
        final ResponseEntity<UserDto> response;
        try {
            response = keycloakRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), UserDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to find user: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Failed to find user: not found: {}", e.getMessage());
            throw new UserNotFoundException("User not found");
        } catch (Exception e) {
            log.error("Failed to find user: unexpected response: {}", e.getMessage());
            throw new AuthServiceException("Unexpected result", e);
        }
        return response.getBody();
    }

}
