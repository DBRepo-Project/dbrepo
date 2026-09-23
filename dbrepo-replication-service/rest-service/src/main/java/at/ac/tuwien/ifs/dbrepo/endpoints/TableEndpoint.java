package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableDeleteNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/replication/table")
@RequiredArgsConstructor
@Tag(name = "Table", description = "Table replication endpoints")
public class TableEndpoint {

    private final ReplicationService replicationService;

    @PostMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_table")
    @Operation(summary = "Replicate table",
            description = "Replicates a table creation notification.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> replicateTable(@Valid @RequestBody TableNotificationDto request) {
        final int replicas = replicationService.replicateTable(request);
        return ResponseEntity.ok(Map.of("status", "accepted", "replicas", replicas));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_table_delete")
    @Operation(summary = "Replicate table deletion",
            description = "Replicates a table deletion notification.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> deleteTable(@Valid @RequestBody TableDeleteNotificationDto request) {
        final int replicas = replicationService.replicateTableDelete(request);
        return ResponseEntity.ok(Map.of("status", "accepted", "replicas", replicas));
    }
}
