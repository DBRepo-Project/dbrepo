package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.elasticsearch.client.ml.dataframe.QueryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
    public ExportEndpoint(QueryService queryService, DatabaseService databaseService, AccessService accessService,
                          IdentifierService identifierService, TableService tableService, QueryConfig queryConfig) {
        super(tableService, accessService, databaseService, identifierService, queryConfig);
        this.queryService = queryService;
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
            FileStorageException, NotAllowedException, QueryMalformedException, UserNotFoundException {
        log.debug("endpoint export table, id={}, databaseId={}, tableId={}, timestamp={}, principal={}", containerId, databaseId,
                tableId, timestamp, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_EXPORT", principal)) {
            log.error("Missing data export permission");
            throw new NotAllowedException("Missing data export permission");
        }
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = queryService.findAll(containerId, databaseId, tableId, timestamp, principal);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        log.trace("export table resulted in resource {}", resource);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }


}
