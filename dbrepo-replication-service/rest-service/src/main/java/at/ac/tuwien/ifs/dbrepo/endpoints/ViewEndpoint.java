package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/replication/view")
@RequiredArgsConstructor
@Tag(name = "View", description = "View replication endpoints")
public class ViewEndpoint {

    private final ViewService viewService;

    @PostMapping
    @Operation(summary = "Replicate view", description = "Replicates a view creation notification")
    public ResponseEntity<Map<String, Object>> replicateView(@RequestBody ViewNotificationDto viewNotificationDto) {
        // Delegate to service to forward to other instances
        viewService.handleViewReplication(viewNotificationDto);
        Map<String, Object> response = Map.of(
            "status", "success",
            "message", "View replication notification received successfully",
            "viewInformation", viewNotificationDto.getViewDto()
        );
        return ResponseEntity.ok(response);
    }
}


