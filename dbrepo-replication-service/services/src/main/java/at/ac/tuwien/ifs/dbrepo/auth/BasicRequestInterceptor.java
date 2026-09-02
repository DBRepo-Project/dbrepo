package at.ac.tuwien.ifs.dbrepo.auth;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.List;

@Slf4j
public class BasicRequestInterceptor implements ClientHttpRequestInterceptor {

    private final GatewayConfig gatewayConfig;

    public BasicRequestInterceptor(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        final HttpHeaders headers = request.getHeaders();
        if (headers.getAccept().isEmpty()) {
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(gatewayConfig.getSystemUsername(), gatewayConfig.getSystemPassword());
        log.trace("set basic auth for internal user: {}", gatewayConfig.getSystemUsername());
        return execution.execute(request, body);
    }
}
