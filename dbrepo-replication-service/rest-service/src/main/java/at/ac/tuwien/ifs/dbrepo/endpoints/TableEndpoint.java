package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/replication/table")
@RequiredArgsConstructor
@Tag(name = "Table", description = "Table replication endpoints")
public class TableEndpoint {

    private final TableService tableService;

    @PostMapping
    @Operation(summary = "Replicate table", description = "Replicates a table creation notification")
    public ResponseEntity<Map<String, Object>> replicateTable(@RequestBody TableNotificationDto tableNotificationDto) {
        System.out.println("=== REPLICATE TABLE ===");
        System.out.println("Database ID: " + tableNotificationDto.getDatabaseId());
        System.out.println("Table DTO: " + tableNotificationDto.getCreateTableDto());
        System.out.println("Replicas: " + tableNotificationDto.getReplicas());
        
        // Call the service to handle the replication
        tableService.handleTableReplication(tableNotificationDto.getReplicas(), tableNotificationDto.getCreateTableDto());
        
        System.out.println("========================");
        
        Map<String, Object> response = Map.of(
            "status", "success",
            "message", "Table replication notification received successfully",
            "tableInformation", tableNotificationDto.getCreateTableDto()
        );
        
        return ResponseEntity.ok(response);
    }
}
