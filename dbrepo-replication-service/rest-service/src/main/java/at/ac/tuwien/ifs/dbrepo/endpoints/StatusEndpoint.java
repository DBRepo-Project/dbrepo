package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.service.ReplicationMonitoringService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationStatusDto;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replication/status")
@RequiredArgsConstructor
@Tag(name = "Replication Status", description = "Replication health and backlog endpoints")
public class StatusEndpoint {

    private final ReplicationMonitoringService monitoringService;

    @GetMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_status")
    @Operation(summary = "Get replication status",
            description = "Returns replication-service dependency health and local outbox backlog.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<ReplicationStatusDto> getStatus() {
        return ResponseEntity.ok(monitoringService.getStatus());
    }
}
