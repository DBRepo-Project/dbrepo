package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.config.QueryConfig;
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
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    public ExportEndpoint(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('export-table')")
    @Timed(value = "table.export", description = "Time needed to export table data")
    @Operation(summary = "Export table", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long containerId,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      @NotNull @PathVariable("tableId") Long tableId,
                                                      @RequestParam(required = false) Instant timestamp,
                                                      Principal principal)
            throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            FileStorageException, QueryMalformedException, UserNotFoundException {
        log.debug("endpoint export table, id={}, databaseId={}, tableId={}, timestamp={}, principal={}", containerId, databaseId,
                tableId, timestamp, principal);
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = queryService.tableFindAll(containerId, databaseId, tableId, timestamp, principal);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        log.trace("export table resulted in resource {}", resource);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }


}
