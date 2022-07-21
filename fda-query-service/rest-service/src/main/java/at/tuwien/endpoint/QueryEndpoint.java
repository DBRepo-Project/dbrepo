package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.api.database.query.*;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;

@Log4j2
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/query")
public class QueryEndpoint extends AbstractEndpoint {

    private final QueryService queryService;
    private final StoreService storeService;

    @Autowired
    public QueryEndpoint(TableService tableService, QueryService queryService, StoreService storeService,
                         DatabaseService databaseService,
                         IdentifierService identifierService) {
        super(tableService, databaseService, identifierService);
        this.queryService = queryService;
        this.storeService = storeService;
    }

    @PutMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Execute query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> execute(@NotNull @PathVariable("id") Long id,
                                                  @NotNull @PathVariable("databaseId") Long databaseId,
                                                  @NotNull @Valid @RequestBody ExecuteStatementDto data,
                                                  @RequestParam(value = "page", required = false) Long page,
                                                  @RequestParam(value = "size", required = false) Long size,
                                                  @NotNull Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException, QueryMalformedException,
            ContainerNotFoundException, ColumnParseException, UserNotFoundException, TableMalformedException,
            NotAllowedException {
        if (!hasQueryPermission(databaseId, "QUERY_EXECUTE", principal)) {
            log.error("Missing execute query permission");
            throw new NotAllowedException("Missing execute query permission");
        }
        /* validation */
        if (data.getStatement() == null || data.getStatement().isBlank()) {
            log.error("Query is empty");
            throw new QueryMalformedException("Query is empty");
        }
        log.trace("data for execution: {}", data);
        final QueryResultDto result = queryService.execute(id, databaseId, data, principal, page, size);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @PutMapping("/{queryId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Re-execute some query")
    public ResponseEntity<QueryResultDto> reExecute(@NotNull @PathVariable("id") Long id,
                                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                                    @NotNull @PathVariable("queryId") Long queryId,
                                                    @RequestParam(value = "page", required = false) Long page,
                                                    @RequestParam(value = "size", required = false) Long size,
                                                    @NotNull Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            TableNotFoundException, QueryMalformedException, ContainerNotFoundException, TableMalformedException,
            ColumnParseException, NotAllowedException {
        if (!hasQueryPermission(databaseId, queryId, "QUERY_RE_EXECUTE", principal)) {
            log.error("Missing re-execute query permission");
            throw new NotAllowedException("Missing re-execute query permission");
        }
        final Query query = storeService.findOne(id, databaseId, queryId);
        final QueryResultDto result = queryService.reExecute(id, databaseId, query, page, size);
        result.setId(queryId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/export")
    @Transactional(readOnly = true)
    @Operation(summary = "Exports some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<InputStreamResource> export(@NotNull @PathVariable("id") Long id,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      @NotNull @PathVariable("queryId") Long queryId,
                                                      @NotNull Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, TableMalformedException, FileStorageException, NotAllowedException, QueryMalformedException {
        if (!hasQueryPermission(databaseId, queryId, "QUERY_EXPORT", principal)) {
            log.error("Missing export query permission");
            throw new NotAllowedException("Missing export query permission");
        }
        storeService.findOne(id, databaseId, queryId);
        final HttpHeaders headers = new HttpHeaders();
        final ExportResource resource = queryService.findOne(id, databaseId, queryId);
        headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource.getResource());
    }

}
