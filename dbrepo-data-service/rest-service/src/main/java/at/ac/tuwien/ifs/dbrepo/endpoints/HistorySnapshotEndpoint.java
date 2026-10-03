package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/database/{databaseId}/replication/snapshots")
@PreAuthorize("hasAnyAuthority('system', 'replication')")
@RequiredArgsConstructor
public class HistorySnapshotEndpoint {
    private final MetadataService metadataService;
    private final HistorySnapshotService snapshots;

    @PostMapping
    public Envelope create(@PathVariable UUID databaseId, @Valid @RequestBody Create request)
            throws DatabaseNotFoundException, TableNotFoundException, RemoteUnavailableException, MetadataServiceException,
            SQLException, IOException {
        return snapshots.create(metadataService.getDatabase(databaseId), metadataService.getTable(databaseId, request.tableId()), request);
    }

    @GetMapping("/{snapshotId}")
    public Envelope manifest(@PathVariable UUID databaseId, @PathVariable UUID snapshotId)
            throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException, SQLException, IOException {
        return snapshots.manifest(metadataService.getDatabase(databaseId), snapshotId);
    }

    @GetMapping("/{snapshotId}/chunks/{index}")
    public Chunk chunk(@PathVariable UUID databaseId, @PathVariable UUID snapshotId, @PathVariable long index)
            throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException, SQLException, IOException {
        return snapshots.readChunk(metadataService.getDatabase(databaseId), snapshotId, index);
    }

    @GetMapping("/{snapshotId}/status")
    public Receipt status(@PathVariable UUID databaseId, @PathVariable UUID snapshotId)
            throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException, SQLException, IOException {
        return snapshots.status(metadataService.getDatabase(databaseId), snapshotId);
    }

    @GetMapping("/table/{tableId}/checkpoint")
    public Checkpoint checkpoint(@PathVariable UUID databaseId, @PathVariable UUID tableId)
            throws DatabaseNotFoundException, TableNotFoundException, RemoteUnavailableException, MetadataServiceException, SQLException {
        return snapshots.targetCheckpoint(metadataService.getDatabase(databaseId), metadataService.getTable(databaseId, tableId));
    }

    @PostMapping("/imports")
    public Receipt beginImport(@PathVariable UUID databaseId, @Valid @RequestBody Import request)
            throws DatabaseNotFoundException, TableNotFoundException, RemoteUnavailableException, MetadataServiceException,
            SQLException, IOException {
        return snapshots.beginImport(metadataService.getDatabase(databaseId),
                metadataService.getTable(databaseId, request.targetTableId()), request);
    }

    @PutMapping("/{snapshotId}/chunks/{index}")
    public Receipt putChunk(@PathVariable UUID databaseId, @PathVariable UUID snapshotId, @PathVariable long index,
                            @RequestBody Chunk chunk)
            throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException, SQLException, IOException {
        if (index != chunk.index()) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Chunk path index differs from payload");
        return snapshots.putChunk(metadataService.getDatabase(databaseId), snapshotId, chunk);
    }

    @PostMapping("/{snapshotId}/verify")
    public Receipt verify(@PathVariable UUID databaseId, @PathVariable UUID snapshotId, @Valid @RequestBody Verify request)
            throws DatabaseNotFoundException, TableNotFoundException, RemoteUnavailableException, MetadataServiceException,
            SQLException, IOException {
        return snapshots.verifyImport(metadataService.getDatabase(databaseId),
                metadataService.getTable(databaseId, request.tableId()), snapshotId);
    }

    public record Verify(@NotNull UUID tableId) { }
}
