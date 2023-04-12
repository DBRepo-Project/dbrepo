package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.security.Principal;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/access")
public class AccessEndpoint {

    private final AccessService accessService;
    private final DatabaseMapper databaseMapper;

    @Autowired
    public AccessEndpoint(DatabaseService databaseService, AccessService accessService, DatabaseMapper databaseMapper) {
        this.accessService = accessService;
        this.databaseMapper = databaseMapper;
    }

    @GetMapping
    @Transactional
    @Timed(value = "access.check", description = "Time needed to check access to a table")
    @PreAuthorize("hasAuthority('check-access')")
    @Operation(summary = "Check access to some table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<DatabaseAccessDto> checkAccess(@NotBlank @PathVariable("id") Long containerId,
                                                         @NotBlank @PathVariable("databaseId") Long databaseId,
                                                         @NotBlank @PathVariable("tableId") Long tableId,
                                                         @NotNull Principal principal)
            throws AccessDeniedException {
        log.debug("endpoint check access to database, containerId={}, databaseId={}, principal={}",
                containerId, databaseId, principal);
        final DatabaseAccess access = accessService.hasAccess(databaseId, tableId, principal.getName());
        final DatabaseAccessDto dto = databaseMapper.databaseAccessToDatabaseAccessDto(access);
        log.trace("check access resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

}
