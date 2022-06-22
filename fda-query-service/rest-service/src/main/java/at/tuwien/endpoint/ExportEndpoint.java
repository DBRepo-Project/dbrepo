package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.exception.*;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.QueryService;
import at.tuwien.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
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
public class ExportEndpoint extends AbstractEndpoint {

    private final QueryService queryService;

    @Autowired
    public ExportEndpoint(TableService tableService, QueryService queryService, DatabaseService databaseService,
                          IdentifierService identifierService) {
        super(tableService, databaseService, identifierService);
        this.queryService = queryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#tableId, 'DATA_EXPORT')")
    @Operation(summary = "Export table")
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long id,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      @NotNull @PathVariable("tableId") Long tableId,
                                                      @RequestParam(required = false) Instant timestamp,
                                                      @NotNull Principal principal)
            throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            FileStorageException, NotAllowedException {
        if (!hasDatabasePermission(databaseId, tableId, "DATA_EXPORT", principal)) {
            throw new NotAllowedException("Data export not allowed");
        }
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = queryService.findAll(id, databaseId, tableId, timestamp);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }


}
