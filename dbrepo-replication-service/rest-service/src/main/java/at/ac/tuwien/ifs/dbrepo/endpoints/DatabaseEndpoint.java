package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/database")
@RequiredArgsConstructor
@Tag(name = "Database", description = "Database replication endpoints")
public class DatabaseEndpoint {

    private final DatabaseService databaseService;

    @PostMapping
    @Operation(summary = "Replicate database", description = "Replicates a database creation notification")
    public ResponseEntity<Map<String, Object>> replicateDatabase(@RequestBody DatabaseNotificationDto databaseNotificationDto) {
        System.out.println("=== REPLICATE DATABASE ===");
        
        // Call the service to handle the replication
        databaseService.handleDatabaseReplication(databaseNotificationDto);
        
        System.out.println("========================");
        
        Map<String, Object> response = Map.of(
            "status", "success",
            "message", "Database replication notification received successfully",
            "databaseInformation", databaseNotificationDto.getCreateDatabaseDto()
        );
        
        return ResponseEntity.ok(response);
    }
} 