package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationServiceHealthDto;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@RestController
public class BrokerHealthEndpoint {

    private final RestTemplate brokerRestTemplate;

    public BrokerHealthEndpoint(@Qualifier("brokerRestTemplate") RestTemplate brokerRestTemplate) {
        this.brokerRestTemplate = brokerRestTemplate;
    }

    @GetMapping("/api/metadata/broker/health")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Inspect broker resource alarms",
            description = "Returns the probe result, not an AMQP delivery guarantee. HTTP 200 means the probe completed; "
                    + "the body distinguishes UP, DOWN (alarms), UNAVAILABLE and UNKNOWN.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ReplicationServiceHealthDto health() {
        final long started = System.nanoTime();
        final ReplicationServiceHealthDto result = ReplicationServiceHealthDto.builder()
                .name("broker")
                .status("UNKNOWN")
                .build();
        try {
            final ResponseEntity<JsonNode> response = brokerRestTemplate.exchange("/api/health/checks/alarms",
                    HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class);
            classify(result, response.getStatusCode().value(), response.getBody());
        } catch (HttpStatusCodeException e) {
            JsonNode body = null;
            try {
                body = e.getResponseBodyAs(JsonNode.class);
            } catch (RestClientException | IllegalStateException ignored) {
                // A proxy error page is not evidence of a RabbitMQ alarm.
            }
            classify(result, e.getStatusCode().value(), body);
        } catch (ResourceAccessException e) {
            result.setStatus("UNAVAILABLE");
            result.setError("Broker management endpoint is unreachable or timed out");
        } catch (RestClientException e) {
            result.setError("Broker management endpoint returned an unreadable health response");
        }
        result.setDurationMs(Duration.ofNanos(System.nanoTime() - started).toMillis());
        return result;
    }

    private void classify(ReplicationServiceHealthDto result, int httpStatus, JsonNode body) {
        result.setHttpStatus(httpStatus);
        final String status = body == null ? "" : body.path("status").asText("");
        if (httpStatus == 200 && "ok".equals(status)) {
            result.setStatus("UP");
        } else if (httpStatus == 503 && "failed".equals(status)) {
            result.setStatus("DOWN");
            result.setError("Broker reports resource alarms");
        } else if (httpStatus == 401 || httpStatus == 403 || httpStatus >= 500) {
            result.setStatus("UNAVAILABLE");
            result.setError("Broker management health check unavailable (HTTP " + httpStatus + ")");
        } else {
            result.setError("Broker management endpoint returned an unrecognized health response");
        }
    }
}
