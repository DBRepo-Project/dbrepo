package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

import java.util.Map;

@RestController
@RequestMapping("api/replication/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "Replication monitoring endpoints")
public class MonitoringEndpoint {

    private final MonitoringService monitoringService;

    @GetMapping("/{databaseId}/status")
    @Operation(summary = "Replication status", description = "Performs a status check for a database: loads the database and its tables from metadata service and returns counts")
    public ResponseEntity<ReplicationMonitoringDatabaseDto> status(@PathVariable UUID databaseId) {
        try {
            final ReplicationMonitoringDatabaseDto result = monitoringService.status(databaseId);
            return ResponseEntity.ok(result);
        } catch (DatabaseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (RemoteUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (MetadataServiceException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


