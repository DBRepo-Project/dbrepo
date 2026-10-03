package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxStatus;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/replication/outbox")
@RequiredArgsConstructor
@Tag(name = "Replication Outbox", description = "Durable replication retry endpoints")
public class OutboxEndpoint {

    private final ReplicationService replicationService;
    private final ReplicationOutboxService outboxService;

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
        if (outboxService.findById(id).map(entry -> entry.getStatus() == ReplicationOutboxStatus.CANCELLED)
                .orElse(false)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled entries cannot be retried");
        }
        return ResponseEntity.ok(Map.of("retried", replicationService.retryOutboxEntry(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_replication_outbox_cancel")
    @Operation(summary = "Cancel an obsolete replication outbox entry",
            description = "Stops future retries and retains the payload, errors and operator audit. "
                    + "Does not undo a request already sent to the peer.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<ReplicationOutboxEntry> cancel(@PathVariable("id") UUID id,
                                                        @Valid @RequestBody CancellationRequest request,
                                                        Principal principal) {
        try {
            return ResponseEntity.ok(outboxService.cancel(id, request.reason(), principal.getName()));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    public record CancellationRequest(@NotBlank @Size(max = 2000) String reason) {
    }
}
