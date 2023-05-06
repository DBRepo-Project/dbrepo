package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.*;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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
public class QueryEndpoint {

    private final QueryService queryService;
    private final StoreService storeService;
    private final AccessService accessService;
    private final DatabaseService databaseService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public QueryEndpoint(QueryService queryService, StoreService storeService, AccessService accessService,
                         DatabaseService databaseService, EndpointValidator endpointValidator) {
        this.accessService = accessService;
        this.databaseService = databaseService;
        this.endpointValidator = endpointValidator;
        this.queryService = queryService;
        this.storeService = storeService;
    }

    @PostMapping
    @Transactional(readOnly = true)
    @Timed(value = "query.execute", description = "Time needed to execute a query")
    @PreAuthorize("isAuthenticated()")
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
            DatabaseConnectionException, SortException, PaginationException, NotAllowedException {
        log.debug("endpoint execute query, containerId={}, databaseId={}, data={}, page={}, size={}, principal={}, sortDirection={}, sortColumn={}",
                containerId, databaseId, data, page, size, principal, sortDirection, sortColumn);
        /* check */
        if (data.getStatement() == null || data.getStatement().isBlank()) {
            log.error("Failed to execute empty query");
            throw new QueryMalformedException("Failed to execute empty query");
        }
        endpointValidator.validateForbiddenStatements(data);
        endpointValidator.validateDataParams(page, size, sortDirection, sortColumn);
        /* has access */
        accessService.find(databaseId, principal.getName());
        /* execute */
        final QueryResultDto result = queryService.execute(containerId, databaseId, data, principal, page, size,
                sortDirection, sortColumn);
        log.trace("execute query resulted in result {}", result);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/data")
    @Transactional(readOnly = true)
    @Timed(value = "query.reexecute", description = "Time needed to re-execute a query")
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
            QueryMalformedException, TableMalformedException, ColumnParseException,
            DatabaseConnectionException, SortException, PaginationException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint re-execute query, containerId={}, databaseId={}, queryId={}, principal={}, page={}, size={}, sortDirection={}, sortColumn={}",
                containerId, databaseId, queryId, principal, page, size, sortDirection, sortColumn);
        endpointValidator.validateDataParams(page, size, sortDirection, sortColumn);
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to re-execute private query: principal is null");
                throw new NotAllowedException("Failed to re-execute private query: principal is null");
            }
            if (!User.hasRole(principal, "re-execute-query")) {
                log.error("Failed to re-execute private query: role missing");
                throw new NotAllowedException("Failed to re-execute private query: role missing");
            }
        }
        /* execute */
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        final QueryResultDto result = queryService.reExecute(containerId, databaseId, query, page, size,
                sortDirection, sortColumn, principal);
        result.setId(queryId);
        log.trace("re-execute query resulted in result {}", result);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/data/count")
    @Transactional(readOnly = true)
    @Timed(value = "query.reexecute.count", description = "Time needed to re-execute a query")
    @Operation(summary = "Re-execute some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Long> reExecuteCount(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("queryId") Long queryId,
                                               Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, TableMalformedException, ColumnParseException, NotAllowedException,
            DatabaseConnectionException, UserNotFoundException {
        log.debug("endpoint re-execute query count, containerId={}, databaseId={}, queryId={}, principal={}",
                containerId, databaseId, queryId, principal);
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to re-execute private query: principal is null");
                throw new NotAllowedException("Failed to re-execute private query: principal is null");
            }
            if (!User.hasRole(principal, "re-execute-query")) {
                log.error("Failed to re-execute private query: role missing");
                throw new NotAllowedException("Failed to re-execute private query: role missing");
            }
        }
        /* execute */
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        final Long result = queryService.reExecuteCount(containerId, databaseId, query, principal);
        log.trace("re-execute query count resulted in result {}", result);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/export")
    @Transactional(readOnly = true)
    @Timed(value = "query.export", description = "Time needed to export query data")
    @Operation(summary = "Exports some query", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> export(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("queryId") Long queryId,
                                    @RequestHeader(HttpHeaders.ACCEPT) String accept,
                                    Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, TableMalformedException, FileStorageException, QueryMalformedException,
            DatabaseConnectionException, UserNotFoundException, NotAllowedException {
        log.debug("endpoint export query, containerId={}, databaseId={}, queryId={}, accept={}, principal={}",
                containerId, databaseId, queryId, accept, principal);
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to export private query: principal is null");
                throw new NotAllowedException("Failed to export private query: principal is null");
            }
            if (!User.hasRole(principal, "export-query-data")) {
                log.error("Failed to export private query: role missing");
                throw new NotAllowedException("Failed to export private query: role missing");
            }
        }
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        log.trace("querystore returned query {}", query);
        final ExportResource resource = queryService.findOne(containerId, databaseId, queryId, principal);
        if (accept == null || accept.equals("text/csv")) {
            final HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
            log.trace("export query resulted in resource {}", resource);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource.getResource());
        }
        log.error("Failed to export, non-csv exports are not supported");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .build();
    }

}
