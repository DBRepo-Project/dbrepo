package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.service.ReplicationActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/database/{databaseId}/replication")
public class ReplicationActivationEndpoint {
    private final ReplicationActivationService activation;

    @PostMapping("/activate")
    @PreAuthorize("hasAuthority('system')")
    public ResponseEntity<Void> activate(@PathVariable("databaseId") UUID databaseId,
                                          @RequestParam("targetSite") String targetSite) throws Exception {
        activation.activate(databaseId, targetSite);
        return ResponseEntity.noContent().build();
    }
}
