package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleNotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/replicate")
@RequiredArgsConstructor
@Tag(name = "Replicate", description = "Replication endpoints")
public class ReplicateEndpoint {

    @PostMapping("/insert")
    @Operation(summary = "Replicate insert", description = "Replicates an insert operation")
    public ResponseEntity<Map<String, Object>> replicateInsert(@RequestBody TupleNotificationDto insertTupleDto) {
        System.out.println("=== REPLICATE INSERT ===");

        System.out.println("========================");
        
        Map<String, Object> response = Map.of(
            "status", "success",
            "message", "Insert replicated successfully",
            "tupleInformation", insertTupleDto.getTupleData()
        );
        
        return ResponseEntity.ok(response);
    }
} 