package at.ac.tuwien.ifs.dbrepo.auth;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.service.CredentialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class BasicRequestInterceptor implements ClientHttpRequestInterceptor {

    private final GatewayConfig gatewayConfig;

    @Autowired
    public BasicRequestInterceptor(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        final HttpHeaders headers = request.getHeaders();
        if (headers.get("Accept") == null) {
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        }
        headers.setBasicAuth(gatewayConfig.getSystemUsername(), gatewayConfig.getSystemPassword());
        log.trace("set bearer token for internal user: {}", gatewayConfig.getSystemUsername());
        return execution.execute(request, body);
    }
}
