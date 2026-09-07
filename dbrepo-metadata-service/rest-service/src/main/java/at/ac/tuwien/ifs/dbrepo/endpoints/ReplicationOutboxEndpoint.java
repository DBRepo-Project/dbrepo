package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/metadata/replication/outbox")
@RequiredArgsConstructor
@Tag(name = "Metadata Replication Outbox", description = "Metadata replication notification retry endpoints")
public class ReplicationOutboxEndpoint {

    private final ReplicationService replicationService;

    @GetMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_metadata_replication_outbox_list")
    @Operation(summary = "List metadata replication outbox entries",
            description = "Lists persisted metadata replication notifications and their retry state.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<List<ReplicationNotificationOutbox>> list() {
        return ResponseEntity.ok(replicationService.findOutboxEntries());
    }

    @PostMapping("/retry")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_metadata_replication_outbox_retry_due")
    @Operation(summary = "Retry due metadata replication outbox entries",
            description = "Retries all pending metadata replication notifications that are due.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> retryDue() {
        return ResponseEntity.ok(Map.of("retried", replicationService.retryDueOutboxEntries()));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_metadata_replication_outbox_retry_one")
    @Operation(summary = "Retry a metadata replication outbox entry",
            description = "Retries one persisted metadata replication notification by id.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> retry(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(Map.of("retried", replicationService.retryOutboxEntry(id)));
    }
}
