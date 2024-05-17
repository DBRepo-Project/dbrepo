package at.tuwien.gateway.impl;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.ServiceConnectionException;
import at.tuwien.exception.ServiceException;
import at.tuwien.gateway.KeycloakGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final RestTemplate restTemplate;
    private final KeycloakConfig keycloakConfig;

    public KeycloakGatewayImpl(@Qualifier("keycloakRestTemplate") RestTemplate restTemplate,
                               KeycloakConfig keycloakConfig) {
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
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain admin token: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to obtain admin token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to obtain admin token: remote host answered unexpected: {}", e.getMessage(), e);
            throw new ServiceException("Failed to obtain admin token: remote host answered unexpected: " + e.getMessage(), e);
        }
        return response.getBody();
    }

    @Override
    public TokenDto obtainUserToken(String username, String password) throws ServiceConnectionException, ServiceException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", username);
        payload.add("password", password);
        payload.add("grant_type", "password");
        payload.add("scope", "openid roles attributes");
        payload.add("client_id", keycloakConfig.getKeycloakClient());
        payload.add("client_secret", keycloakConfig.getKeycloakClientSecret());
        final String url = keycloakConfig.getKeycloakEndpoint() + "/realms/dbrepo/protocol/openid-connect/token";
        log.debug("request user token from url {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = new RestTemplate()
                    .exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new ServiceConnectionException("Failed to obtain user token: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to obtain user token: unexpected response: {}", e.getMessage(), e);
            throw new ServiceException("Failed to obtain user token: unexpected response: " + e.getMessage(), e);
        }
        return response.getBody();
    }

}
