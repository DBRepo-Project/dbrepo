package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceMariaDbImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/replication/replicate")
@RequiredArgsConstructor
@Tag(name = "Replicate", description = "Replication endpoints")
public class ReplicateEndpoint {

    private final DatabaseServiceMariaDbImpl databaseService;

    @PostMapping("/insert")
    @Operation(summary = "Replicate insert", description = "Replicates an insert operation")
    public ResponseEntity<Map<String, Object>> replicateInsert(@RequestBody TupleNotificationDto insertTupleDto) {
        Map<String, Object> response = Map.of(
            "status", "success",
            "message", "Insert replicated successfully",
            "tupleInformation", insertTupleDto.getTupleData()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/database")
    @Operation(summary = "Receive database replication", description = "Receives database replication notification from other instances")
    public ResponseEntity<Map<String, Object>> receiveDatabaseReplication(@RequestBody DatabaseNotificationDto databaseNotificationDto) {
        // Call the service to create the database locally
        Map<String, Object> response = databaseService.insertReplicatedDatabase(databaseNotificationDto);
        
        System.out.println("=== Database Replication Response ===");
        response.forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
        System.out.println("==================================");
        
        return ResponseEntity.ok(response);
    }
} 