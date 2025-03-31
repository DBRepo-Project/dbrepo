package at.ac.tuwien.ifs.dbrepo.auth;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.service.CredentialService;
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

    private final CredentialService credentialService;
    private final GatewayConfig gatewayConfig;

    public InternalRequestInterceptor(CredentialService credentialService, GatewayConfig gatewayConfig) {
        this.credentialService = credentialService;
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        final HttpHeaders headers = request.getHeaders();
        if (headers.get("Accept") == null) {
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        }
        final TokenDto token = credentialService.getAccessToken(gatewayConfig.getSystemUsername(),
                gatewayConfig.getSystemPassword());
        headers.setBearerAuth(token.getAccessToken());
        log.trace("set bearer token for internal user: {}", gatewayConfig.getSystemUsername());
        return execution.execute(request, body);
    }
}
