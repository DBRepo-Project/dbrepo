package at.tuwien.endpoint;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.*;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
                         IdentifierService identifierService, TableService tableService, AccessService accessService,
                         QueryConfig queryConfig) {
        super(tableService, accessService, databaseService, identifierService, queryConfig);
        this.queryService = queryService;
        this.storeService = storeService;
    }

    @PostMapping
    @Transactional(readOnly = true)
    @Timed(value = "query.execute", description = "Time needed to execute a query")
    @Operation(summary = "Execute query", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Executed query successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Query is malformed or sorting is not valid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container, database or user could not be found or pagination is not within valid range",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Execution of query is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Parsing of resulting columns failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Execution of time-versioned query resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Query store failed to store query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
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
        log.debug("endpoint execute query, containerId={}, databaseId={}, data={}, page={}, size={}, principal={}, sortDirection={}, sortColumn={}",
                containerId, databaseId, data, page, size, principal, sortDirection, sortColumn);
        /* check */
        if (!hasDatabasePermission(containerId, databaseId, "QUERY_EXECUTE", principal)) {
            log.error("Missing execute query permission");
            throw new NotAllowedException("Missing execute query permission");
        }
        if (data.getStatement() == null || data.getStatement().isBlank()) {
            log.error("Failed to execute empty query");
            throw new QueryMalformedException("Failed to execute empty query");
        }
        validateForbiddenStatements(data);
        validateDataParams(page, size, sortDirection, sortColumn);
        /* execute */
        final QueryResultDto result = queryService.execute(containerId, databaseId, data, principal, page, size,
                sortDirection, sortColumn);
        log.trace("execute query resulted in result {}", result);
        return ResponseEntity.accepted()
                .body(result);
    }

    @GetMapping("/{queryId}/data")
    @Transactional(readOnly = true)
    @Timed(value = "query.reexecute", description = "Time needed to re-execute a query")
    @Operation(summary = "Re-execute some query", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Re-executed query successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Query is malformed or sorting is not valid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container, database or user could not be found or pagination is not within valid range",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Re-execution of query is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Parsing of resulting columns failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Re-execution of time-versioned query resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Query store failed to store query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
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
            DatabaseConnectionException, SortException, PaginationException, UserNotFoundException {
        log.debug("endpoint re-execute query, containerId={}, databaseId={}, queryId={}, principal={}, page={}, size={}, sortDirection={}, sortColumn={}",
                containerId, databaseId, queryId, principal, page, size, sortDirection, sortColumn);
        /* check */
        if (!hasQueryPermission(containerId, databaseId, queryId, "QUERY_RE_EXECUTE", principal)) {
            log.error("Missing re-execute query permission");
            throw new NotAllowedException("Missing re-execute query permission");
        }
        validateDataParams(page, size, sortDirection, sortColumn);
        /* execute */
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        final QueryResultDto result = queryService.reExecute(containerId, databaseId, query, page, size,
                sortDirection, sortColumn, principal);
        result.setId(queryId);
        log.trace("re-execute query resulted in result {}", result);
        return ResponseEntity.accepted()
                .body(result);
    }

    @GetMapping("/{queryId}/export")
    @Transactional(readOnly = true)
    @Timed(value = "query.export", description = "Time needed to export query data")
    @Operation(summary = "Exports some query", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Export query successfully",
                    content = {@Content(mediaType = "text/csv")}),
            @ApiResponse(responseCode = "400",
                    description = "Query is malformed or sorting is not valid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Container, database or user could not be found or pagination is not within valid range",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Export of query is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Parsing of resulting columns failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Export of time-versioned query resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Query store failed to store query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> export(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("queryId") Long queryId,
                                    @RequestHeader(HttpHeaders.ACCEPT) String accept,
                                    Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, TableMalformedException, FileStorageException, NotAllowedException,
            QueryMalformedException, DatabaseConnectionException, UserNotFoundException {
        log.debug("endpoint export query, containerId={}, databaseId={}, queryId={}, accept={}, principal={}",
                containerId, databaseId, queryId, accept, principal);
        if (!hasQueryPermission(containerId, databaseId, queryId, "QUERY_EXPORT", principal)) {
            log.error("Missing export query permission");
            throw new NotAllowedException("Missing export query permission");
        }
        log.trace("checking if query exists in the query store");
        final Query query = storeService.findOne(containerId, databaseId, queryId, principal);
        log.trace("query store returned query {}", query);
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
