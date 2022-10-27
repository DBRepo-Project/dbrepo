package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
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
    private final DatabaseMapper databaseMapper;

    @Autowired
    public AccessEndpoint(DatabaseService databaseService, ContainerService containerService,
                          AccessService accessService, DatabaseMapper databaseMapper) {
        super(databaseService, containerService);
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Give access to some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> giveAccess(@NotBlank @PathVariable("id") Long containerId,
                                        @NotBlank @PathVariable("databaseId") Long databaseId,
                                        @Valid @RequestBody DatabaseGiveAccessDto accessDto,
                                        @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseMalformedException {
        log.debug("endpoint give access to database, containerId={}, databaseId={}, accessDto={}, principal={}",
                containerId, databaseId, accessDto, principal);
        if (!hasDatabasePermission(containerId, databaseId, "GIVE_ACCESS", principal)) {
            log.error("Missing give access permission");
            throw new NotAllowedException("Missing give access permission");
        }
        try {
            accessService.hasAccess(databaseId, accessDto.getUsername());
            log.error("Failed to give access to user with username {}, already has access", accessDto.getUsername());
            throw new NotAllowedException("Failed to give access to user");
        } catch (AccessDeniedException e) {
            /* ignore */
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
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, AccessDeniedException,
            QueryMalformedException, DatabaseMalformedException {
        log.debug("endpoint modify access to database, containerId={}, databaseId={}, username={}, accessDto={}, principal={}",
                containerId, databaseId, username, accessDto, principal);
        if (!hasDatabasePermission(containerId, databaseId, "MODIFY_ACCESS", principal)) {
            log.error("Missing modify access permission");
            throw new NotAllowedException("Missing modify access permission");
        }
        accessService.hasAccess(databaseId, username);
        accessService.modifyAccess(containerId, databaseId, username, accessDto);
        return ResponseEntity.accepted()
                .build();
    }

    @GetMapping("/{username}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Check access to some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseAccessDto> checkAccess(@NotBlank @PathVariable("id") Long containerId,
                                                         @NotBlank @PathVariable("databaseId") Long databaseId,
                                                         @NotBlank @PathVariable("username") String username,
                                                         @NotNull Principal principal) throws NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint check access to database, containerId={}, databaseId={}, username={}, principal={}",
                containerId, databaseId, username, principal);
        if (!hasDatabasePermission(containerId, databaseId, "CHECK_ACCESS", principal)) {
            log.error("Missing modify access permission");
            throw new NotAllowedException("Missing modify access permission");
        }
        final DatabaseAccess access = accessService.hasAccess(databaseId, username);
        final DatabaseAccessDto dto = databaseMapper.databaseAccessToDatabaseAccessDto(access);
        log.trace("check access resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{username}")
    @Transactional
    @PreAuthorize("hasRole('ROLE_RESEARCHER')")
    @Operation(summary = "Revoke access to some database", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> revokeAccess(@NotBlank @PathVariable("id") Long containerId,
                                          @NotBlank @PathVariable("databaseId") Long databaseId,
                                          @NotBlank @PathVariable("username") String username,
                                          @NotNull Principal principal)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, AccessDeniedException,
            QueryMalformedException, DatabaseMalformedException {
        log.debug("endpoint revoke access to database, containerId={}, databaseId={}, username={}, principal={}",
                containerId, databaseId, username, principal);
        if (!hasDatabasePermission(containerId, databaseId, "REVOKE_ACCESS", principal)) {
            log.error("Missing revoke access permission");
            throw new NotAllowedException("Missing revoke access permission");
        }
        accessService.hasAccess(databaseId, principal.getName());
        accessService.revokeAccess(containerId, databaseId, username);
        return ResponseEntity.accepted()
                .build();
    }

}
