package at.tuwien.gateway.impl;

import at.tuwien.api.auth.KeycloakErrorDto;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final KeycloakConfig keycloakConfig;

    @Autowired
    public KeycloakGatewayImpl(KeycloakConfig keycloakConfig) {
        this.keycloakConfig = keycloakConfig;
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
        log.trace("request user token from url: {}", url);
        log.trace("request username: {}", username);
        log.trace("request password: {}", password != null ? "(set)" : "(not set)");
        log.trace("request client_id: {}", keycloakConfig.getKeycloakClient());
        log.trace("request client_secret: {}", keycloakConfig.getKeycloakClientSecret());
        final ResponseEntity<TokenDto> response;
        try {
            response = new RestTemplate()
                    .exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (HttpServerErrorException e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new AuthServiceConnectionException("Service unavailable", e);
        } catch (HttpClientErrorException.BadRequest e) {
            if (e.getResponseBodyAsByteArray() != null && e.getResponseBodyAsByteArray().length > 0) {
                final KeycloakErrorDto error = e.getResponseBodyAs(KeycloakErrorDto.class);
                if (error != null && error.getError().equals("invalid_grant")) {
                    log.error("Failed to obtain user token: {}", error.getErrorDescription());
                    throw new AccountNotSetupException(error.getErrorDescription());
                }
            }
            log.error("Failed to obtain user token: bad request");
            throw new CredentialsInvalidException("Bad request", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Failed to obtain user token: invalid credentials");
            throw new CredentialsInvalidException("Invalid credentials", e);
        }
        return response.getBody();
    }

}
