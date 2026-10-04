package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/database/{databaseId}/subset")
public class SubsetReplicationBackfillEndpoint {
    private final MetadataService metadata;
    private final SubsetReplicationService replication;
    private final SubsetService subsets;

    @PostMapping("/replication-backfill")
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_subset_replication_backfill")
    @Operation(summary = "Queue existing canonical subsets for a configured replication target")
    public ResponseEntity<Map<String, String>> backfill(@PathVariable("databaseId") UUID databaseId,
                                                       @RequestParam("targetSite") String targetSite)
            throws DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException, SQLException {
        final var database = metadata.refreshDatabase(databaseId);
        replication.requireBackfillTarget(database, targetSite);
        subsets.upgradeQueryStore(database);
        replication.backfill(database, targetSite);
        return ResponseEntity.accepted().body(Map.of("status", "queued"));
    }
}
