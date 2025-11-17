package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationHealthDto;
import at.ac.tuwien.ifs.dbrepo.service.HealthCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/replication/health")
@RequiredArgsConstructor
@Tag(name = "Replication Health", description = "Local health checks for replication-service dependencies")
public class ReplicationHealthEndpoint {

    private final HealthCheckService healthCheckService;

    @GetMapping
    @Operation(summary = "Replication-service dependency health",
            description = "Checks health of metadata-service, data-service, replication-service and broker")
    public ResponseEntity<ReplicationHealthDto> health() {
        ReplicationHealthDto body = healthCheckService.checkHealth();

        String status = body.getStatus() != null ? body.getStatus() : "UP";
        HttpStatus httpStatus = "DEGRADED".equalsIgnoreCase(status) ? HttpStatus.MULTI_STATUS : HttpStatus.OK;

        return ResponseEntity.status(httpStatus)
                .header("X-Replication-Health-Status", status)
                .body(body);
    }
}


