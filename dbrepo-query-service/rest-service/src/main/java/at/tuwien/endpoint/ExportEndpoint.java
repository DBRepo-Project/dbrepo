package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.time.Instant;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/export")
public class ExportEndpoint {

    private final QueryService queryService;
    private final DatabaseService databaseService;

    @Autowired
    public ExportEndpoint(QueryService queryService, DatabaseService databaseService) {
        this.queryService = queryService;
        this.databaseService = databaseService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "table.export", description = "Time needed to export table data")
    @Operation(summary = "Export table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long containerId,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      @NotNull @PathVariable("tableId") Long tableId,
                                                      @RequestParam(required = false) Instant timestamp,
                                                      Principal principal)
            throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            FileStorageException, QueryMalformedException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint export table, id={}, databaseId={}, tableId={}, timestamp={}, principal={}", containerId, databaseId,
                tableId, timestamp, principal);
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to export private table: principal is null");
                throw new NotAllowedException("Failed to export private table: principal is null");
            }
            if (!User.hasRole(principal, "export-table-data")) {
                log.error("Failed to export private table: role missing");
                throw new NotAllowedException("Failed to export private table: role missing");
            }
        }
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = queryService.tableFindAll(containerId, databaseId, tableId, timestamp, principal);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        log.trace("export table resulted in resource {}", resource);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }


}
