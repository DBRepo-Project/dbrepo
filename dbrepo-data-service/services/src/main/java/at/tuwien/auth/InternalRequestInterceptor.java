package at.tuwien.auth;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.gateway.KeycloakGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.List;

@Log4j2
public class InternalRequestInterceptor implements ClientHttpRequestInterceptor {

    private final GatewayConfig gatewayConfig;
    private final KeycloakGateway keycloakGateway;

    public InternalRequestInterceptor(GatewayConfig gatewayConfig, KeycloakGateway keycloakGateway) {
        this.gatewayConfig = gatewayConfig;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        final HttpHeaders headers = request.getHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        final TokenDto token = keycloakGateway.obtainUserToken(gatewayConfig.getSystemUsername(),
                gatewayConfig.getSystemPassword());
        headers.setBearerAuth(token.getAccessToken());
        log.trace("set bearer token for internal user: {}", gatewayConfig.getSystemUsername());
        return execution.execute(request, body);
    }
}
