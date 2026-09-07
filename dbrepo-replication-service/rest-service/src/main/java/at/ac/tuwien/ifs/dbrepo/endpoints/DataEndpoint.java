package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.service.DataSynchronisationResult;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

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

    @PostMapping("/synchronise/database/{databaseId}/table/{tableId}")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_data_synchronise")
    @Operation(summary = "Synchronise replicated table data",
            description = "Synchronises existing table tuples to configured replica sites.",
            security = {@SecurityRequirement(name = "basicAuth")},
            hidden = true)
    public ResponseEntity<Map<String, Object>> synchroniseData(@PathVariable("databaseId") UUID databaseId,
                                                               @PathVariable("tableId") UUID tableId,
                                                               @RequestParam(defaultValue = "100") int pageSize) {
        if (pageSize <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be positive");
        }
        final DataSynchronisationResult result = replicationService.synchroniseData(databaseId, tableId, pageSize);
        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "pages", result.pages(),
                "tuples", result.tuples(),
                "replicaWrites", result.replicaWrites()));
    }
}
