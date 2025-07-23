package at.ac.tuwien.ifs.dbrepo.endpoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class CustomHealthEndpoint {

    @GetMapping
    @Operation(summary = "Health check", description = "Returns the health status of the replication service")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = Map.of(
            "status", "UP",
            "service", "replication-service",
            "version", "1.0.0"
        );
        return ResponseEntity.ok(response);
    }
} 