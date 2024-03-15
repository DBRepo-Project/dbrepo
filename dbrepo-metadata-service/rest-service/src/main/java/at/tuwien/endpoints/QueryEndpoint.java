package at.tuwien.endpoints;

import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.querystore.Query;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import at.tuwien.utils.PrincipalUtil;
import at.tuwien.utils.UserUtil;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Log4j2
@RestController
@RequestMapping(path = "/api/database/{databaseId}/query",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class QueryEndpoint {

    private final QueryService queryService;
    private final StoreService storeService;
    private final DatabaseService databaseService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public QueryEndpoint(QueryService queryService, StoreService storeService, DatabaseService databaseService,
                         EndpointValidator endpointValidator) {
        this.queryService = queryService;
        this.storeService = storeService;
        this.databaseService = databaseService;
        this.endpointValidator = endpointValidator;
    }

    @PostMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbr_query_execute")
    @PreAuthorize("hasAuthority('execute-query')")
    @Operation(summary = "Execute query", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Executed query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Execute query not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, query or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Could not store query in query store",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Could not parse columns",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<QueryResultDto> execute(@NotNull @PathVariable("databaseId") Long databaseId,
                                                  @NotNull @Valid @RequestBody ExecuteStatementDto data,
                                                  @RequestParam(value = "page", required = false) Long page,
                                                  @RequestParam(value = "size", required = false) Long size,
                                                  @NotNull Principal principal,
                                                  @RequestParam(required = false) SortType sortDirection,
                                                  @RequestParam(required = false) String sortColumn)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException, QueryMalformedException,
            ColumnParseException, UserNotFoundException, TableMalformedException, SortException, PaginationException,
            NotAllowedException, AccessDeniedException, QueryNotFoundException {
        log.debug("endpoint execute query, databaseId={}, data={}, page={}, size={}, sortDirection={}, sortColumn={}, {}",
                databaseId, data, page, size, sortDirection, sortColumn, PrincipalUtil.formatForDebug(principal));
        /* check */
        if (data.getStatement() == null || data.getStatement().isBlank()) {
            log.error("Failed to execute empty query");
            throw new QueryMalformedException("Failed to execute empty query");
        }
        endpointValidator.validateForbiddenStatements(data);
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        endpointValidator.validateDataParams(page, size, sortDirection, sortColumn);
        /* execute */
        final QueryResultDto result = queryService.execute(databaseId, data, principal, page, size,
                sortDirection, sortColumn);
        log.trace("execute query resulted in result {}", result);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/data")
    @Transactional(readOnly = true)
    @Observed(name = "dbr_query_reexecute")
    @Operation(summary = "Re-execute some query", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Executed query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Execute query not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or query could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Could not store query in query store",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Could not parse columns",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<QueryResultDto> reExecute(@NotNull @PathVariable("databaseId") Long databaseId,
                                                    @NotNull @PathVariable("queryId") Long queryId,
                                                    Principal principal,
                                                    @RequestParam(value = "page", required = false) Long page,
                                                    @RequestParam(value = "size", required = false) Long size,
                                                    @RequestParam(required = false) SortType sortDirection,
                                                    @RequestParam(required = false) String sortColumn)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException, QueryMalformedException,
            ColumnParseException, TableMalformedException, SortException, PaginationException, NotAllowedException,
            AccessDeniedException, QueryNotFoundException {
        log.debug("endpoint re-execute query, databaseId={}, queryId={}, page={}, size={}, sortDirection={}, sortColumn={}, {}",
                databaseId, queryId, page, size, sortDirection, sortColumn, PrincipalUtil.formatForDebug(principal));
        endpointValidator.validateDataParams(page, size, sortDirection, sortColumn);
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        /* execute */
        final Query query = storeService.findOne(databaseId, queryId, principal);
        final QueryResultDto result = queryService.reExecute(databaseId, query, page, size, sortDirection, sortColumn,
                principal);
        result.setId(queryId);
        log.trace("re-execute query resulted in result {}", result);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/{queryId}/data/count")
    @Transactional(readOnly = true)
    @Observed(name = "dbr_query_reexecute_count")
    @Operation(summary = "Re-execute some query", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Executed query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Execute query not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or query could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Could not parse columns",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<Long> reExecuteCount(@NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("queryId") Long queryId,
                                               Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            QueryMalformedException, TableMalformedException, ColumnParseException, NotAllowedException,
            AccessDeniedException {
        log.debug("endpoint re-execute query count, databaseId={}, queryId={}, {}", databaseId, queryId, PrincipalUtil.formatForDebug(principal));
        endpointValidator.validateOnlyAccessOrPublic(databaseId, principal);
        /* execute */
        final Query query = storeService.findOne(databaseId, queryId, principal);
        final Long result = queryService.reExecuteCount(databaseId, query, principal);
        log.trace("re-execute query count resulted in result {}", result);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping(value = "/{queryId}/export", produces = MediaType.ALL_VALUE)
    @Transactional(readOnly = true)
    @Observed(name = "dbr_query_export")
    @Operation(summary = "Exports some query", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Executed query"),
            @ApiResponse(responseCode = "400",
                    description = "Image is not supported",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Execute query not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or query could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "Export of query failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "410",
                    description = "Could not find in S3 storage",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Sidecar failed to export",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))})
    })
    public ResponseEntity<?> export(@NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("queryId") Long queryId,
                                    @RequestHeader(HttpHeaders.ACCEPT) String accept,
                                    Principal principal)
            throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException,
            FileStorageException, QueryMalformedException, NotAllowedException, DataDbSidecarException, DataProcessingException {
        log.debug("endpoint export query, databaseId={}, queryId={}, accept={}, {}", databaseId, queryId, accept, PrincipalUtil.formatForDebug(principal));
        final Database database = databaseService.find(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to export private query: principal is null");
                throw new NotAllowedException("Failed to export private query: principal is null");
            }
            if (!UserUtil.hasRole(principal, "export-query-data")) {
                log.error("Failed to export private query: role missing");
                throw new NotAllowedException("Failed to export private query: role missing");
            }
        }
        final Query query = storeService.findOne(databaseId, queryId, principal);
        log.trace("query store returned query {}", query);
        final ExportResource resource = queryService.findOne(databaseId, queryId, principal);
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
