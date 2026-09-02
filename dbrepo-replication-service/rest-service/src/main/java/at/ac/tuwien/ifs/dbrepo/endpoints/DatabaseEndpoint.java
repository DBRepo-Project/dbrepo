package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/replication/database")
@RequiredArgsConstructor
@Tag(name = "Database", description = "Database replication endpoints")
public class DatabaseEndpoint {

    private final ReplicationService replicationService;

    @PostMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_database")
    @Operation(summary = "Replicate database",
            description = "Replicates a database creation notification.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> replicateDatabase(@Valid @RequestBody DatabaseNotificationDto request) {
        final int replicas = replicationService.replicateDatabase(request);
        return ResponseEntity.ok(Map.of("status", "accepted", "replicas", replicas));
    }
}
