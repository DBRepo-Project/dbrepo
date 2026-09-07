package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;
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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/database/{databaseId}/replication/outbox")
@RequiredArgsConstructor
@Tag(name = "Data Replication Outbox", description = "Tuple replication notification retry endpoints")
public class ReplicationOutboxEndpoint {

    private final MetadataService metadataService;
    private final ReplicationService replicationService;

    @GetMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_data_replication_outbox_list")
    @Operation(summary = "List tuple replication outbox entries",
            description = "Lists persisted tuple replication notifications and their retry state.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<List<TupleReplicationOutboxEntry>> list(@PathVariable("databaseId") UUID databaseId)
            throws RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, SQLException {
        final Database database = metadataService.getDatabase(databaseId);
        return ResponseEntity.ok(replicationService.findOutboxEntries(database));
    }

    @PostMapping("/retry")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_data_replication_outbox_retry_due")
    @Operation(summary = "Retry due tuple replication outbox entries",
            description = "Retries all pending tuple replication notifications that are due for one database.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> retryDue(@PathVariable("databaseId") UUID databaseId)
            throws RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException {
        final Database database = metadataService.getDatabase(databaseId);
        return ResponseEntity.ok(Map.of("retried", replicationService.retryDueOutboxEntries(database)));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_data_replication_outbox_retry_one")
    @Operation(summary = "Retry a tuple replication outbox entry",
            description = "Retries one persisted tuple replication notification by id.",
            security = {@SecurityRequirement(name = "basicAuth")})
    public ResponseEntity<Map<String, Object>> retry(@PathVariable("databaseId") UUID databaseId,
                                                     @PathVariable("id") UUID id)
            throws RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException {
        final Database database = metadataService.getDatabase(databaseId);
        return ResponseEntity.ok(Map.of("retried", replicationService.retryOutboxEntry(database, id)));
    }
}
