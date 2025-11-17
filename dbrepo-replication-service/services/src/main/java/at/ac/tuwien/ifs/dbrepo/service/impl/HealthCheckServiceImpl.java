package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationHealthDto;
import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationServiceHealthDto;
import at.ac.tuwien.ifs.dbrepo.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private final GatewayConfig gatewayConfig;
    private final RestTemplate internalRestTemplate;          // points to metadata-service
    private final RestTemplate dataRestTemplate;              // points to data-service
    private final RestTemplate replicationRestTemplate;       // points to replication-service (self via gateway)

    @Override
    public ReplicationHealthDto checkHealth() {

        final ReplicationServiceHealthDto metadataHealth = checkEndpoint(
                internalRestTemplate,
                "/actuator/health",
                "metadata-service"
        );
        final ReplicationServiceHealthDto dataHealth = checkEndpoint(
                dataRestTemplate,
                "/actuator/health",
                "data-service"
        );
        final ReplicationServiceHealthDto replicationHealth = checkEndpoint(
                replicationRestTemplate,
                "/actuator/health",
                "replication-service"
        );

        // Broker: for now, we rely on metadata-service to be reachable;
        // in your deployment, this can be replaced with a direct RabbitMQ management API call.
        final ReplicationServiceHealthDto brokerHealth = ReplicationServiceHealthDto.builder()
                .name("broker")
                .status(metadataHealth.getStatus() != null ? metadataHealth.getStatus() : "unknown")
                .httpStatus(metadataHealth.getHttpStatus())
                .durationMs(metadataHealth.getDurationMs())
                .error("broker health piggybacks on metadata-service reachability for now")
                .build();

        // overall status: degraded if any mandatory service is not UP
        final boolean degraded =
                !"UP".equalsIgnoreCase(String.valueOf(metadataHealth.getStatus())) ||
                !"UP".equalsIgnoreCase(String.valueOf(dataHealth.getStatus())) ||
                !"UP".equalsIgnoreCase(String.valueOf(replicationHealth.getStatus()));

        return ReplicationHealthDto.builder()
                .status(degraded ? "DEGRADED" : "UP")
                .metadataService(metadataHealth)
                .dataService(dataHealth)
                .replicationService(replicationHealth)
                .broker(brokerHealth)
                .build();
    }

    private ReplicationServiceHealthDto checkEndpoint(RestTemplate template,
                                                      String path,
                                                      String logicalName) {
        long started = System.currentTimeMillis();
        try {
            ResponseEntity<Map> response = template.getForEntity(path, Map.class);
            long duration = System.currentTimeMillis() - started;
            HttpStatus status = response.getStatusCode();

            // Try to read Spring Boot actuator's \"status\" field if present
            Object bodyStatus = response.getBody() != null ? response.getBody().get("status") : null;
            String effectiveStatus;
            if (bodyStatus != null) {
                effectiveStatus = String.valueOf(bodyStatus);
            } else if (status.is2xxSuccessful()) {
                effectiveStatus = "UP";
            } else {
                effectiveStatus = "DOWN";
            }
            log.info(\"Health check for {} returned HTTP {} in {} ms (status={})\",
                    logicalName, status.value(), duration, effectiveStatus);
            return ReplicationServiceHealthDto.builder()
                    .name(logicalName)
                    .status(effectiveStatus)
                    .httpStatus(status.value())
                    .durationMs(duration)
                    .build();
        } catch (RestClientException ex) {
            long duration = System.currentTimeMillis() - started;
            log.warn(\"Health check for {} failed after {} ms: {}\", logicalName, duration, ex.getMessage());
            return ReplicationServiceHealthDto.builder()
                    .name(logicalName)
                    .status(\"DOWN\")
                    .httpStatus(503)
                    .durationMs(duration)
                    .error(ex.getMessage())
                    .build();
        }
    }
}


