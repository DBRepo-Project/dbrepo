package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api/replication/data")
@Tag(name = "Data", description = "Data replication endpoints")
public class DataEndpoint {

    private final at.ac.tuwien.ifs.dbrepo.service.TableService tableService;

    public DataEndpoint(at.ac.tuwien.ifs.dbrepo.service.TableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping
    @Operation(summary = "Receive tuple with timestamps", description = "Receives a tuple including versioning timestamps along with database and table context for replication")
    public ResponseEntity<Map<String, Object>> receiveTupleWithTimestamps(@RequestBody DataReplicationDto request) {
        log.info("Received tuple for replication: database={}, table={}",
                request.getDatabase() != null ? request.getDatabase().getInternalName() : null,
                request.getTable() != null ? request.getTable().getInternalName() : null);
        log.debug("Tuple payload: {}", request.getTuple());

        // fan-out to other replicas
        tableService.handleDataReplication(request);

        final Map<String, Object> response = Map.of(
                "status", "accepted",
                "database", request.getDatabase() != null ? request.getDatabase().getId() : null,
                "table", request.getTable() != null ? request.getTable().getId() : null,
                "receivedTupleKeys", request.getTuple() != null ? request.getTuple().getData().keySet() : null
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Receive delete replication", description = "Receives a delete replication (tuple incl. timestamps) and fans out to replicas on the same base path")
    public ResponseEntity<Map<String, Object>> receiveDeleteReplication(@RequestBody DataReplicationDto request) {
        log.info("Received delete replication");

        tableService.handleDataDeleteReplication(request);

        final Map<String, Object> response = Map.of(
                "status", "accepted",
                "database", request.getDatabase() != null ? request.getDatabase().getId() : null,
                "table", request.getTable() != null ? request.getTable().getId() : null,
                "deletedAt", request.getTuple() != null ? request.getTuple().getDeletedAt() : null
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Receive update replication", description = "Receives an update replication (tuple incl. timestamps) and fans out to replicas on the same base path")
    public ResponseEntity<Map<String, Object>> receiveUpdateReplication(@RequestBody DataReplicationDto request) {
        log.info("Received update replication");

        tableService.handleDataUpdateReplication(request);

        final Map<String, Object> response = Map.of(
                "status", "accepted",
                "database", request.getDatabase() != null ? request.getDatabase().getId() : null,
                "table", request.getTable() != null ? request.getTable().getId() : null
        );
        return ResponseEntity.ok(response);
    }
}


