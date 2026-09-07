package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
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
@RequestMapping("/api/replication/outbox")
@RequiredArgsConstructor
@Tag(name = "Replication Outbox", description = "Durable replication retry endpoints")
public class OutboxEndpoint {

    private final ReplicationService replicationService;

    @GetMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_outbox_list")
    @Operation(summary = "List replication outbox entries",
            description = "Lists persisted replication operations and their retry state.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<List<ReplicationOutboxEntry>> list() {
        return ResponseEntity.ok(replicationService.findOutboxEntries());
    }

    @PostMapping("/retry")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_outbox_retry_due")
    @Operation(summary = "Retry due replication outbox entries",
            description = "Retries all pending outbox entries that are due.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> retryDue() {
        return ResponseEntity.ok(Map.of("retried", replicationService.retryDueOutboxEntries()));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_outbox_retry_one")
    @Operation(summary = "Retry a replication outbox entry",
            description = "Retries one persisted replication operation by id.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> retry(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(Map.of("retried", replicationService.retryOutboxEntry(id)));
    }
}
