package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceMariaDbImpl;
import at.ac.tuwien.ifs.dbrepo.service.impl.TableServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/replication/replicate")
@RequiredArgsConstructor
@Tag(name = "Replicate", description = "Replication endpoints")
public class ReplicateEndpoint {

    private final DatabaseServiceMariaDbImpl databaseService;
    private final TableService tableService;
    private final ReplicationService replicationService;

    @Value("${BASE_URL:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${server.host:localhost}")
    private String serverHost;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${HOSTNAME:}")
    private String podHostname;

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

    @PostMapping("/update-replication-database-ids")
    @Operation(summary = "Update replication database IDs", description = "Receives map of replica URLs to database IDs")
    public ResponseEntity<Map<String, Object>> updateReplicationDatabaseIds(@RequestBody Map<String, String> replicaUrlToDatabaseIdMap) {
        System.out.println("=== Received Replication Database IDs Update ===");
        replicaUrlToDatabaseIdMap.forEach((url, id) -> {
            System.out.println("URL: " + url + " -> Database ID: " + id);
        });
        System.out.println("=============================================");
        
        try {

            String localDatabaseId = null;

            System.out.println("BASE URL:");
            System.out.println(baseUrl);

            
            // Find the local database ID by looking for the base URL in the map
            for (Map.Entry<String, String> entry : replicaUrlToDatabaseIdMap.entrySet()) {
                if (entry.getKey().contains(baseUrl)) {
                    localDatabaseId = entry.getValue();
                    break;
                }
            }
            
            if (localDatabaseId != null) {
                System.out.println("Local Database ID: " + localDatabaseId);
                
                // For each other URL in the map, call updateReplicationUrl
                for (Map.Entry<String, String> entry : replicaUrlToDatabaseIdMap.entrySet()) {
                    String replicaUrl = entry.getKey();
                    String remoteDatabaseId = entry.getValue();
                    
                    // Skip if this is the local URL
                    if (!entry.getKey().contains(baseUrl)) {
                        System.out.println("Updating replica URL: " + replicaUrl + " with remote ID: " + remoteDatabaseId);
                        // Call updateReplicationUrlWithRemoteId for each replica
                        replicationService.updateReplicationUrlWithRemoteId(UUID.fromString(localDatabaseId), replicaUrl, UUID.fromString(remoteDatabaseId));
                    }
                }
            } else {
                System.out.println("Could not find local database ID in the map");
            }
            
        } catch (Exception e) {
            System.out.println("Error processing replication database IDs: " + e.getMessage());
        }
        
        Map<String, Object> response = Map.of(
            "status", "success",
            "message", "Received " + replicaUrlToDatabaseIdMap.size() + " replication database IDs",
            "receivedCount", replicaUrlToDatabaseIdMap.size()
        );
        
        return ResponseEntity.ok(response);
    }

    

    //TODO: check if necessary
    @PostMapping("/table")
    @Operation(summary = "Receive table", description = "Receives table replication notification from other instances")
    public ResponseEntity<Map<String, Object>> receiveTableReplication(@RequestParam UUID databaseId, @RequestBody TableNotificationDto tableNotificationDto) {
        // Call the service to create the database locally
        Map<String, Object> response = tableService.insertReplicatedTable(databaseId, tableNotificationDto);

        System.out.println("=== Table Response ===");
        response.forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
        System.out.println("==================================");

        return ResponseEntity.ok(response);
    }

} 