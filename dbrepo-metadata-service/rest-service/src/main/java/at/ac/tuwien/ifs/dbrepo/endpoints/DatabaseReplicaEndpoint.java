package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.AddReplicaDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseReplicaService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/database/{databaseId}/replicas")
@RequiredArgsConstructor
public class DatabaseReplicaEndpoint {
    private final DatabaseService databases;
    private final DatabaseReplicaService replicas;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('system', 'create-database')")
    public ResponseEntity<Map<String, String>> add(@PathVariable("databaseId") UUID databaseId,
                                                  @Valid @RequestBody AddReplicaDto request, Principal principal)
            throws DatabaseNotFoundException {
        final var database = databases.findById(databaseId);
        if (!AuthUtil.isSystem(principal) && !database.getOwnedBy().equals(AuthUtil.getUsername(principal))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the database owner or administrator may add replicas");
        }
        replicas.request(database, request.replicaUrl());
        return ResponseEntity.accepted().body(Map.of("status", "queued"));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('system')")
    public ResponseEntity<Void> register(@PathVariable("databaseId") UUID databaseId, @Valid @RequestBody AddReplicaDto request,
            @RequestParam(name = "preparedTableId", required = false, defaultValue = "") Set<UUID> preparedTableIds)
            throws UserNotFoundException, NotAllowedException {
        replicas.register(databaseId, request.replicaUrl(), preparedTableIds);
        return ResponseEntity.noContent().build();
    }
}
