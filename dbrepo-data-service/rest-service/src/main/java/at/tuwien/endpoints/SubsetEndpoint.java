package at.tuwien.endpoints;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.SubsetService;
import at.tuwien.utils.UserUtil;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/subset")
public class SubsetEndpoint {

    private final SubsetService subsetService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public SubsetEndpoint(SubsetService queryService, EndpointValidator endpointValidator,
                          MetadataServiceGateway metadataServiceGateway) {
        this.subsetService = queryService;
        this.endpointValidator = endpointValidator;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @GetMapping
    @Observed(name = "dbrepo_subset_list")
    @Operation(summary = "Find subsets", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found subsets",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryDto[].class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to find subsets",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<QueryDto>> list(@NotNull @PathVariable("databaseId") Long databaseId,
                                               @RequestParam(name = "persisted", required = false) Boolean filterPersisted,
                                               Principal principal)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, NotAllowedException {
        log.debug("endpoint find subsets in database, databaseId={}, filterPersisted={}, principal.name={}", databaseId,
                filterPersisted, principal != null ? principal.getName() : null);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to find subsets in database: no access");
                throw new NotAllowedException("Failed to find subsets in database: no access");
            }
            metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        }
        final List<QueryDto> queries;
        try {
            queries = subsetService.findAll(database, filterPersisted);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        log.info("Found {} subsets in data database", queries.size());
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/{subsetId}")
    @Observed(name = "dbrepo_subset_find")
    @Operation(summary = "Find subset", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryDto.class)),
                            @Content(mediaType = "text/csv")}),
            @ApiResponse(responseCode = "400",
                    description = "Malformed select query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to find subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to find acceptable representation",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> findById(@NotNull @PathVariable("databaseId") Long databaseId,
                                      @NotNull @PathVariable("subsetId") Long subsetId,
                                      @NotNull HttpServletRequest httpServletRequest,
                                      @RequestParam(required = false) Instant timestamp,
                                      Principal principal)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, FormatNotAvailableException, StorageUnavailableException, QueryMalformedException,
            SidecarExportException, StorageNotFoundException, NotAllowedException, UserNotFoundException {
        String accept = httpServletRequest.getHeader("Accept");
        log.debug("endpoint find subset in database, databaseId={}, subsetId={}, accept={}, timestamp={}", databaseId,
                subsetId, accept, timestamp);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to find subsets in database: no access");
                throw new NotAllowedException("Failed to find subsets in database: no access");
            }
            metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        }
        final QueryDto query;
        try {
            query = subsetService.findById(database, subsetId);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        /* parameters */
        if (timestamp == null) {
            log.debug("timestamp not set: default to now");
            timestamp = Instant.now();
        }
        if (accept == null) {
            log.debug("accept header not set: default to application/json");
            accept = MediaType.APPLICATION_JSON_VALUE;
        }
        switch (accept) {
            case MediaType.APPLICATION_JSON_VALUE:
                log.trace("accept header matches json");
                return ResponseEntity.ok(query);
            case "text/csv":
                log.trace("accept header matches csv");
                final String filename = RandomStringUtils.randomAlphabetic(20).toLowerCase();
                try {
                    final ExportResourceDto resource = subsetService.export(database, query, timestamp, filename);
                    final HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
                    log.trace("export table resulted in resource {}", resource);
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(resource.getResource());

                } catch (SQLException e) {
                    log.error("Failed to establish connection to database: {}", e.getMessage());
                    throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
                }
        }
        throw new FormatNotAvailableException("Must provide either application/json or text/csv headers");
    }

    @PostMapping
    @Observed(name = "dbrepo_subset_create")
    @PreAuthorize("hasAuthority('execute-query')")
    @Operation(summary = "Create subset", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Malformed select query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to find subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to insert query into query store of data database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "501",
                    description = "Failed to execute query as it contains non-supported keywords",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryResultDto> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                                 @Valid @RequestBody ExecuteStatementDto data,
                                                 @NotNull Principal principal,
                                                 @RequestParam(required = false) Long page,
                                                 @RequestParam(required = false) Long size,
                                                 @RequestParam(required = false) Instant timestamp)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, StorageUnavailableException, QueryMalformedException, SidecarExportException,
            StorageNotFoundException, QueryStoreInsertException, TableMalformedException, PaginationException,
            QueryNotSupportedException, NotAllowedException, UserNotFoundException {
        log.debug("endpoint create subset in database, databaseId={}, data.statement={}, principal.name={}, page={}, size={}, timestamp={}",
                databaseId, data.getStatement(), principal.getName(), page, size, timestamp);
        /* check */
        endpointValidator.validateDataParams(page, size);
        endpointValidator.validateForbiddenStatements(data.getStatement());
        metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        /* parameters */
        if (page == null) {
            log.debug("page not set: default to 0");
            page = 0L;
        }
        if (size == null) {
            log.debug("size not set: default to 10");
            size = 10L;
        }
        if (timestamp == null) {
            log.debug("timestamp not set: default to now");
            timestamp = Instant.now();
        }
        /* create */
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        final QueryResultDto queryResult;
        try {
            queryResult = subsetService.execute(database, data.getStatement(), timestamp, UserUtil.getId(principal),
                    page, size, null, null);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        log.info("Created subset with id {} in data database", queryResult.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(queryResult);
    }

    @RequestMapping(value = "/{subsetId}/data", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Observed(name = "dbrepo_subset_data")
    @Operation(summary = "Retrieved subset data", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved subset data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Malformed select query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to retrieve subset data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryResultDto> getData(@NotNull @PathVariable("databaseId") Long databaseId,
                                                  @NotNull @PathVariable("subsetId") Long subsetId,
                                                  Principal principal,
                                                  @NotNull HttpServletRequest request,
                                                  @RequestParam(required = false) Long page,
                                                  @RequestParam(required = false) Long size) throws PaginationException,
            DatabaseNotFoundException, RemoteUnavailableException, NotAllowedException, QueryNotFoundException,
            DatabaseUnavailableException, TableMalformedException, QueryMalformedException, UserNotFoundException {
        log.debug("endpoint re-execute query, databaseId={}, subsetId={}, principal.name={} page={}, size={}",
                databaseId, subsetId, principal != null ? principal.getName() : null, page, size);
        endpointValidator.validateDataParams(page, size);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to re-execute query: no authentication found");
                throw new NotAllowedException("Failed to re-execute query: no authentication found");
            }
            metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        }
        /* parameters */
        if (page == null) {
            log.debug("page not set: default to 0");
            page = 0L;
        }
        if (size == null) {
            log.debug("size not set: default to 10");
            size = 10L;
        }
        try {
            final QueryDto query = subsetService.findById(database, subsetId);
            final Long count = subsetService.reExecuteCount(database, query);
            final HttpHeaders headers = new HttpHeaders();
            headers.set("X-Count", "" + count);
            headers.set("Access-Control-Expose-Headers", "X-Count");
            if (request.getMethod().equals("GET")) {
                final QueryResultDto result = subsetService.reExecute(database, query, page, size, null, null);
                result.setId(subsetId);
                log.trace("re-execute query resulted in result {}", result);
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(result);
            }
            return ResponseEntity.ok()
                    .headers(headers)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{queryId}")
    @PreAuthorize("hasAuthority('persist-query')")
    @Observed(name = "dbrepo_subset_persist")
    @Operation(summary = "Persist subset", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Persisted subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Malformed select query",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to persist subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to persist subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryDto> persist(@NotNull @PathVariable("databaseId") Long databaseId,
                                            @NotNull @PathVariable("queryId") Long queryId,
                                            @NotNull @Valid @RequestBody QueryPersistDto data,
                                            @NotNull Principal principal) throws NotAllowedException,
            RemoteUnavailableException, DatabaseNotFoundException, QueryStorePersistException,
            DatabaseUnavailableException, QueryNotFoundException, UserNotFoundException {
        log.debug("endpoint persist query, databaseId={}, queryId={}, data.persist={}, principal.name={}", databaseId,
                queryId, data.getPersist(), principal.getName());
        metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        try {
            subsetService.persist(database, queryId, data.getPersist());
            final QueryDto dto = subsetService.findById(database, queryId);
            log.trace("persist query resulted in query {}", dto);
            return ResponseEntity.accepted()
                    .body(dto);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

}
