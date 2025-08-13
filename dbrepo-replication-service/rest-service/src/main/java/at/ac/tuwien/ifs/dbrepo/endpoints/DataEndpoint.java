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

    @PostMapping
    @Operation(summary = "Receive tuple with timestamps", description = "Receives a tuple including versioning timestamps along with database and table context for replication")
    public ResponseEntity<Map<String, Object>> receiveTupleWithTimestamps(@RequestBody DataReplicationDto request) {
        log.info("Received tuple for replication: database={}, table={}",
                request.getDatabase() != null ? request.getDatabase().getInternalName() : null,
                request.getTable() != null ? request.getTable().getInternalName() : null);
        log.debug("Tuple payload: {}", request.getTuple());

        // TODO: call service to fan-out to other replicas using request.getDatabase() / request.getTable()

        final Map<String, Object> response = Map.of(
                "status", "accepted",
                "database", request.getDatabase() != null ? request.getDatabase().getId() : null,
                "table", request.getTable() != null ? request.getTable().getId() : null,
                "receivedTupleKeys", request.getTuple() != null ? request.getTuple().keySet() : null
        );
        return ResponseEntity.ok(response);
    }
}


