package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.*;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public QueryEndpoint(QueryService queryService, StoreService storeService, DatabaseService databaseService,
                         IdentifierService identifierService) {
        super(databaseService, identifierService);
        this.queryService = queryService;
        this.storeService = storeService;
    }

    @PutMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Execute query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> execute(@NotNull @PathVariable("id") Long containerId,
                                                  @NotNull @PathVariable("databaseId") Long databaseId,
                                                  @NotNull @Valid @RequestBody ExecuteStatementDto data,
                                                  @RequestParam(value = "page", required = false) Long page,
                                                  @RequestParam(value = "size", required = false) Long size,
                                                  @NotNull Principal principal,
                                                  @RequestParam(required = false) SortType sortDirection,
                                                  @RequestParam(required = false) String sortColumn)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException, QueryMalformedException,
            ContainerNotFoundException, ColumnParseException, UserNotFoundException, TableMalformedException,
            NotAllowedException, DatabaseConnectionException, SortException, PaginationException {
        /* check */
        if (!hasDatabasePermission(containerId, databaseId, "QUERY_EXECUTE", principal)) {
            log.error("Missing execute query permission");
            throw new NotAllowedException("Missing execute query permission");
        }
        if (data.getStatement() == null || data.getStatement().isBlank()) {
            log.error("Query is empty");
            throw new QueryMalformedException("Query is empty");
        }
        validateDataParams(page, size, sortDirection, sortColumn);
        /* execute */
        final QueryResultDto result = queryService.execute(containerId, databaseId, data, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/data")
    @Transactional(readOnly = true)
    @Operation(summary = "Re-execute some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> reExecute(@NotNull @PathVariable("id") Long containerId,
                                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                                    @NotNull @PathVariable("queryId") Long queryId,
                                                    Principal principal,
                                                    @RequestParam(value = "page", required = false) Long page,
                                                    @RequestParam(value = "size", required = false) Long size,
                                                    @RequestParam(required = false) SortType sortDirection,
                                                    @RequestParam(required = false) String sortColumn)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, TableMalformedException, ColumnParseException, NotAllowedException,
            DatabaseConnectionException, SortException, PaginationException {
        /* check */
        if (!hasQueryPermission(containerId, databaseId, queryId, "QUERY_RE_EXECUTE", principal)) {
            log.error("Missing re-execute query permission");
            throw new NotAllowedException("Missing re-execute query permission");
        }
        validateDataParams(page, size, sortDirection, sortColumn);
        /* execute */
        final Query query = storeService.findOne(containerId, databaseId, queryId);
        final QueryResultDto result = queryService.reExecute(containerId, databaseId, query, page, size,
                sortDirection, sortColumn);
        result.setId(queryId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/export")
    @Transactional(readOnly = true)
    @Operation(summary = "Exports some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> export(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("queryId") Long queryId,
                                    @RequestParam(value = "download", required = false) String download,
                                    Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, TableMalformedException, FileStorageException, NotAllowedException,
            QueryMalformedException, DatabaseConnectionException {
        if (!hasQueryPermission(containerId, databaseId, queryId, "QUERY_EXPORT", principal)) {
            log.error("Missing export query permission");
            throw new NotAllowedException("Missing export query permission");
        }
        storeService.findOne(containerId, databaseId, queryId);
        if (download != null) {
            final ExportResource resource = queryService.findOne(containerId, databaseId, queryId, true);
            final HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
            log.info("Exported data for container with id {} and database id {} and query id {} as stream",
                    containerId, databaseId, queryId);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource.getResource());
        }
        final ExportResource resource = queryService.findOne(containerId, databaseId, queryId, false);
        log.info("Exported data for container with id {} and database id {} and query id {} as at path {}",
                containerId, databaseId, queryId, "/tmp" + resource.getFilename());
        return ResponseEntity.ok()
                .body(ExportDto.builder()
                        .location(resource.getFilename())
                        .build());
    }

}
