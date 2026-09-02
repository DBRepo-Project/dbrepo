package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/replication/data")
@RequiredArgsConstructor
@Tag(name = "Data", description = "Data replication endpoints")
public class DataEndpoint {

    private final ReplicationService replicationService;

    @PostMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_data_create")
    @Operation(summary = "Replicate tuple create",
            description = "Replicates a tuple create notification.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> replicateCreate(@Valid @RequestBody DataReplicationDto request) {
        final int replicas = replicationService.replicateData(request, HttpMethod.POST);
        return ResponseEntity.ok(Map.of("status", "accepted", "replicas", replicas));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_data_update")
    @Operation(summary = "Replicate tuple update",
            description = "Replicates a tuple update notification.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> replicateUpdate(@Valid @RequestBody DataReplicationDto request) {
        final int replicas = replicationService.replicateData(request, HttpMethod.PUT);
        return ResponseEntity.ok(Map.of("status", "accepted", "replicas", replicas));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_data_delete")
    @Operation(summary = "Replicate tuple delete",
            description = "Replicates a tuple delete notification.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> replicateDelete(@Valid @RequestBody DataReplicationDto request) {
        final int replicas = replicationService.replicateData(request, HttpMethod.DELETE);
        return ResponseEntity.ok(Map.of("status", "accepted", "replicas", replicas));
    }
}
