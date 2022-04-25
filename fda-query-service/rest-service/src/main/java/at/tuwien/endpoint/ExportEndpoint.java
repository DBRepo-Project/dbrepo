package at.tuwien.endpoint;

import at.tuwien.exception.*;
import at.tuwien.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
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
    @Operation(summary = "Export table")
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long id,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      @NotNull @PathVariable("tableId") Long tableId,
                                                      @RequestParam(required = false) Instant timestamp)
            throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            FileStorageException {
        final HttpHeaders headers = new HttpHeaders();
        final InputStreamResource resource = queryService.findAll(id, databaseId, tableId, timestamp);
        headers.add("Content-Disposition", "attachment; filename=\"export.csv\"");
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }


}
