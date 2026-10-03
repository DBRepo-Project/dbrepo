package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationInboxService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class HistorySnapshotReconciliationEndpoint {
    private final MetadataService metadata;
    private final HistorySnapshotService snapshots;
    private final ReplicationInboxService inbox;

    @PostMapping("/api/v1/database/{databaseId}/replication/snapshots/{snapshotId}/reconcile")
    @PreAuthorize("hasAnyAuthority('system', 'replication')")
    public HistorySnapshotDto.Receipt reconcile(@PathVariable UUID databaseId, @PathVariable UUID snapshotId,
                                                 @Valid @RequestBody Request request)
            throws DatabaseNotFoundException, TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, SQLException, IOException {
        return inbox.reconcile(metadata.getDatabase(databaseId), metadata.getTable(databaseId, request.tableId()), snapshotId, snapshots);
    }

    public record Request(@NotNull UUID tableId) { }

    @GetMapping("/api/v1/database/{databaseId}/replication/snapshots/table/{tableId}/checkpoint-with-inbox")
    @PreAuthorize("hasAnyAuthority('system', 'replication')")
    public HistorySnapshotDto.Checkpoint checkpoint(@PathVariable UUID databaseId, @PathVariable UUID tableId)
            throws DatabaseNotFoundException, TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, SQLException {
        return inbox.checkpoint(metadata.getDatabase(databaseId), metadata.getTable(databaseId, tableId), snapshots);
    }
}
