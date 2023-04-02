package at.tuwien.gateway.impl;

import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.auth.TokenDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.gateway.AuthenticationServiceGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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
public class AuthenticationServiceGatewayImpl implements AuthenticationServiceGateway {

    private final RestTemplate restTemplate;
    private final GatewayConfig gatewayConfig;

    @Autowired
    public AuthenticationServiceGatewayImpl(@Qualifier("keycloakRestTemplate") RestTemplate restTemplate,
                                            GatewayConfig gatewayConfig) {
        this.restTemplate = restTemplate;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public TokenDto getToken() throws RemoteUnavailableException {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_FORM_URLENCODED.toString());
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", gatewayConfig.getKeycloakUsername());
        payload.add("password", gatewayConfig.getKeycloakPassword());
        payload.add("grant_type", "password");
        payload.add("client_id", "admin-cli");
        final String url = "/realms/master/protocol/openid-connect/token";
        log.debug("call authentication service {}", url);
        final ResponseEntity<TokenDto> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain admin token: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to obtain admin token", e);
        }
        if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            log.error("Failed to obtain admin token: credentials are invalid");
            throw new RemoteUnavailableException("Failed to obtain admin token: credentials are invalid");
        }
        return response.getBody();
    }

    @Override
    public void createUser(String token, CreateUserDto data) throws RemoteUnavailableException {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON.toString());
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("Authorization", "Bearer: " + token);
        final ResponseEntity<Void> response;
        try {
            response = restTemplate.exchange("/admin/realms/dbrepo/users", HttpMethod.POST, new HttpEntity<>(data, headers), Void.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to create user: {}", e.getMessage());
            throw new RemoteUnavailableException("Failed to create user", e);
        }
        if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            log.error("Failed to create user: credentials are invalid");
            throw new RemoteUnavailableException("Failed to create user: credentials are invalid");
        }
    }

}
