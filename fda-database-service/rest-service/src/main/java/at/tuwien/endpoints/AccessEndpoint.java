package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.service.AccessService;
import at.tuwien.service.ContainerService;
import at.tuwien.service.DatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.security.Principal;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/container/{id}/database/{databaseId}/access")
public class AccessEndpoint extends AbstractEndpoint {

    private final AccessService accessService;

    @Autowired
    public AccessEndpoint(DatabaseService databaseService, ContainerService containerService,
                          AccessService accessService) {
        super(databaseService, containerService);
        this.accessService = accessService;
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Give access to some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> giveAccess(@NotBlank @PathVariable("id") Long containerId,
                                        @NotBlank @PathVariable("databaseId") Long databaseId,
                                        @Valid @RequestBody DatabaseGiveAccessDto accessDto,
                                        @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint give access to database, containerId={}, databaseId={}, accessDto={}, principal={}",
                containerId, databaseId, accessDto, principal);
        if (!hasDatabasePermission(containerId, databaseId, "GIVE_ACCESS", principal)) {
            log.error("Missing give access permission");
            throw new NotAllowedException("Missing give access permission");
        }
        if (accessService.hasAccess(databaseId, accessDto.getUsername())) {
            log.error("Failed to give access to user with username {}, already has access", accessDto.getUsername());
            throw new NotAllowedException("Failed to give access to user");
        }
        accessService.giveAccess(containerId, databaseId, accessDto);
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{username}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Modify access to some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> revokeAccess(@NotBlank @PathVariable("id") Long containerId,
                                          @NotBlank @PathVariable("databaseId") Long databaseId,
                                          @NotBlank @PathVariable("username") String username,
                                          @Valid @RequestBody DatabaseModifyAccessDto accessDto,
                                          @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint modify access to database, containerId={}, databaseId={}, username={}, accessDto={} principal={}",
                containerId, databaseId, username, accessDto, principal);
        if (!hasDatabasePermission(containerId, databaseId, "MODIFY_ACCESS", principal)) {
            log.error("Missing modify access permission");
            throw new NotAllowedException("Missing modify access permission");
        }
        if (!accessService.hasAccess(databaseId, username)) {
            log.error("Failed to modify access to user with username {}, does not have access", username);
            throw new NotAllowedException("Failed to modify access to user");
        }
        accessService.modifyAccess(containerId, databaseId, username, accessDto);
        return ResponseEntity.accepted()
                .build();
    }

    @DeleteMapping("/{username}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Revoke access to some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> revokeAccess(@NotBlank @PathVariable("id") Long containerId,
                                          @NotBlank @PathVariable("databaseId") Long databaseId,
                                          @NotBlank @PathVariable("username") String username,
                                          @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint revoke access to database, containerId={}, databaseId={}, username={}, principal={}",
                containerId, databaseId, username, principal);
        if (!hasDatabasePermission(containerId, databaseId, "REVOKE_ACCESS", principal)) {
            log.error("Missing revoke access permission");
            throw new NotAllowedException("Missing revoke access permission");
        }
        if (!accessService.hasAccess(databaseId, principal.getName())) {
            log.error("Failed to revoke access to user with username {}, does not have access", principal.getName());
            throw new NotAllowedException("Failed to revoke access to user");
        }
        accessService.revokeAccess(containerId, databaseId, username);
        return ResponseEntity.accepted()
                .build();
    }

}
