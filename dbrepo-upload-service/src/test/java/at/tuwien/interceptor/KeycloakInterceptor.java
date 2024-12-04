package at.tuwien.interceptor;

import at.tuwien.api.keycloak.TokenDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;

@Log4j2
public class KeycloakInterceptor implements ClientHttpRequestInterceptor {

    private final String adminUsername;
    private final String adminPassword;
    private final String keycloakEndpoint;
    private final RestTemplate restTemplate;

    public KeycloakInterceptor(RestTemplate restTemplate, String adminUsername, String adminPassword,
                               String keycloakEndpoint) {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.keycloakEndpoint = keycloakEndpoint;
        this.restTemplate = restTemplate;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(keycloakEndpoint));
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        final MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("username", adminUsername);
        payload.add("password", adminPassword);
        payload.add("grant_type", "password");
        payload.add("client_id", "admin-cli");
        final String path = "/realms/master/protocol/openid-connect/token";
        log.trace("obtain admin token at endpoint {} with path {}", keycloakEndpoint, path);
        final ResponseEntity<TokenDto> response;
        try {
            response = restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(payload, headers), TokenDto.class);
        } catch (ResourceAccessException | HttpServerErrorException.ServiceUnavailable e) {
            log.error("Failed to obtain admin token: {}", e.getMessage());
            return execution.execute(request, body);
        }
        if (response.getBody() == null) {
            return execution.execute(request, body);
        }
        request.getHeaders().set("Authorization", "Bearer " + response.getBody().getAccessToken());
        log.trace("set header: Authorization {} (shortened)", response.getBody().getAccessToken().substring(0, 5));
        return execution.execute(request, body);
    }
}
