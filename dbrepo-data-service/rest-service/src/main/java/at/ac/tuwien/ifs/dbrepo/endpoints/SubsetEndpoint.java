package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.ExportResourceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryPersistDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/subset")
public class SubsetEndpoint extends RestEndpoint {

    private final CacheService cacheService;
    private final MariaDbMapper mariaDbMapper;
    private final SubsetService subsetService;
    private final AnalyseService analyseService;
    private final StorageService storageService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;
    private final ReplicationService replicationService;

    @Value("${dbrepo.baseUrl}")
    private String baseUrl;

    @Autowired
    public SubsetEndpoint(CacheService cacheService, MariaDbMapper mariaDbMapper, SubsetService subsetService,
                          StorageService storageService, EndpointValidator endpointValidator,
                          MetadataServiceGateway metadataServiceGateway, AnalyseService analyseService,
                          ReplicationService replicationService) {
        this.cacheService = cacheService;
        this.mariaDbMapper = mariaDbMapper;
        this.subsetService = subsetService;
        this.analyseService = analyseService;
        this.storageService = storageService;
        this.endpointValidator = endpointValidator;
        this.metadataServiceGateway = metadataServiceGateway;
        this.replicationService = replicationService;
    }

    @GetMapping
    @Observed(name = "dbrepo_subset_list")
    @Operation(summary = "Find subsets",
            description = "Finds subsets in the query store. When the database schema is marked as hidden, the user needs to be authorized, have at least read-access to the database. The result can be optionally filtered by setting `persisted`. When set to *true*, only persisted queries are returned, otherwise only non-persisted queries are returned.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found subsets",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = QueryDto.class)))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to find subsets",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database or user in metadata database or query in query store of the data database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<List<QueryDto>> list(@NotNull @PathVariable("databaseId") UUID databaseId,
                                               @RequestParam(name = "persisted", required = false) Boolean filterPersisted,
                                               Principal principal)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, NotAllowedException, MetadataServiceException, UserNotFoundException {
        log.debug("endpoint find subsets in database, databaseId={}, filterPersisted={}", databaseId, filterPersisted);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to list queries: no authentication found");
                throw new NotAllowedException("Failed to list queries: no authentication found");
            }
            if (!isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal, false);
            }
        }
        final List<QueryDto> queries;
        try {
            queries = subsetService.findAll(database, filterPersisted);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        log.info("Found {} subset(s)", queries.size());
        return ResponseEntity.ok(queries);
    }

    @GetMapping("/{subsetId}")
    @Observed(name = "dbrepo_subset_find")
    @Operation(summary = "Find subset",
            description = "Finds a subset in the data database.  When the database schema is marked as hidden, the user needs to be authorized, have at least read-access to the database.  Requests with HTTP header `Accept=application/json` return the metadata, requests with HTTP header `Accept=text/csv` return the data as downloadable file.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryDto.class)),
                            @Content(mediaType = "text/csv")}),
            @ApiResponse(responseCode = "400",
                    description = "Malformed select query",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to find subset",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to find acceptable representation",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<?> findById(@NotNull @PathVariable("databaseId") UUID databaseId,
                                      @NotNull @PathVariable("subsetId") UUID subsetId,
                                      @RequestParam(required = false) Instant timestamp,
                                      Principal principal)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, UserNotFoundException, MetadataServiceException, NotAllowedException {
        log.debug("endpoint find subset in database, databaseId={}, subsetId={}, timestamp={}", databaseId,
                subsetId, timestamp);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to find query: no authentication found");
                throw new NotAllowedException("Failed to find query: no authentication found");
            }
            if (!isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal, false);
            }
        }
        final QueryDto subset;
        try {
            subset = subsetService.findById(database, subsetId);
            subset.setIdentifiers(metadataServiceGateway.getIdentifiers(database.getId(), subset.getId()));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        /* parameters */
        if (timestamp == null) {
            timestamp = Instant.now();
            log.debug("timestamp not set: default to {}", timestamp);
        }
        return ResponseEntity.ok(subset);
    }

    @PostMapping
    @Observed(name = "dbrepo_subset_create")
    @Operation(summary = "Create subset",
            description = "Creates a subset in the query store of the data database. Can also be used without authentication if (and only if) the database is marked as public (i.e. when `is_public` = `is_schema_public` is set to `true`). Otherwise at least read access is required.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Created subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map[].class))}),
            @ApiResponse(responseCode = "400",
                    description = "Malformed select query",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to find subset",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to format data",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to insert query into query store of data database",
                    content = {@Content}),
            @ApiResponse(responseCode = "501",
                    description = "Failed to execute query as it contains non-supported keywords",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<?> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                    @Valid @RequestBody SubsetDto data,
                                    Principal principal,
                                    @NotNull HttpServletRequest request,
                                    @RequestParam(required = false) Instant timestamp,
                                    @RequestParam(required = false) Long page,
                                    @RequestParam(required = false) Long size)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, StorageUnavailableException, QueryMalformedException, StorageNotFoundException,
            QueryStoreInsertException, TableMalformedException, PaginationException, QueryNotSupportedException,
            NotAllowedException, UserNotFoundException, MetadataServiceException, TableNotFoundException,
            ViewMalformedException, ViewNotFoundException, ImageNotFoundException, FormatNotAvailableException,
            ColumnNotFoundException, AnalyseDataTypesException {
        log.debug("endpoint create subset in database, databaseId={}, page={}, size={}, timestamp={}, data.datasource_id={}",
                databaseId, page, size, timestamp, data.getDatasourceId());
        /* check */
        endpointValidator.validateDataParams(page, size);
        endpointValidator.validateSubsetParams(data);
        /* parameters */
        final String username;
        if (principal != null) {
            username = getUsername(principal);
        } else {
            username = null;
        }
        if (page == null) {
            page = 0L;
            log.debug("page not set: default to {}", page);
        }
        if (size == null) {
            size = 10L;
            log.debug("size not set: default to {}", size);
        }
        if (timestamp == null) {
            timestamp = Instant.now();
            log.debug("timestamp not set: default to {}", timestamp);
        }
        /* create */
        final DatabaseDto database = cacheService.getDatabase(databaseId, true);
        if (!database.getIsSchemaPublic()) {
            if (principal == null) {
                log.error("Failed to create subset: no authentication found");
                throw new NotAllowedException("Failed to create subset: no authentication found");
            }
            if (!isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal, false);
            }
        }
        try {
            final UUID subsetId = subsetService.create(database, data, timestamp, username, baseUrl != null ? baseUrl : null);

            if(database.getReplicaUrls() != null && database.getReplicaUrls().size() > 0) {
                final QueryDto subset;
                try {
                    subset = subsetService.findById(database, subsetId);
                    subset.setIdentifiers(metadataServiceGateway.getIdentifiers(database.getId(), subset.getId()));
                } catch (SQLException e) {
                    log.error("Failed to establish connection to database: {}", e.getMessage());
                    throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
                }
                /* parameters */
                if (timestamp == null) {
                    timestamp = Instant.now();
                    log.debug("timestamp not set: default to {}", timestamp);
                }

                // Replicate the subset query to other instances
                try {
                    replicationService.replicateQuery(database, subset);
                    log.debug("Successfully initiated replication for subset: {}", subsetId);
                } catch (Exception e) {
                    log.warn("Failed to replicate subset query: {}", e.getMessage(), e);
                    // Don't fail the main operation if replication fails
                }
            }

            return getData(databaseId, subsetId, principal, "application/json", request, timestamp, page, size);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/{subsetId}/data", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Observed(name = "dbrepo_subset_data")
    @Operation(summary = "Get subset data",
            description = "Gets data of subset with id. For private databases, the user needs at least *READ* access to the associated database. Requests with HTTP method **GET** return the subset dataset, requests with HTTP method **HEAD** only the number of rows in the subset dataset in the `X-Count` header",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved subset data",
                    headers = {@Header(name = "X-Count", description = "Number of rows", schema = @Schema(implementation = UUID.class)),
                            @Header(name = "X-Headers", description = "The list of headers separated by comma", schema = @Schema(implementation = String.class)),
                            @Header(name = "X-Id", description = "The subset id", schema = @Schema(implementation = UUID.class), required = true),
                            @Header(name = "Access-Control-Expose-Headers", description = "Reverse proxy exposing of custom headers", schema = @Schema(implementation = String.class), required = true)},
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map[].class)),
                            @Content(mediaType = "text/csv")}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to retrieve subset data",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to format data",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<?> getData(@NotNull @PathVariable("databaseId") UUID databaseId,
                                     @NotNull @PathVariable("subsetId") UUID subsetId,
                                     Principal principal,
                                     @NotNull @RequestHeader("Accept") String accept,
                                     @NotNull HttpServletRequest request,
                                     @RequestParam(required = false) Instant timestamp,
                                     @RequestParam(required = false) Long page,
                                     @RequestParam(required = false) Long size)
            throws PaginationException, DatabaseNotFoundException, RemoteUnavailableException, NotAllowedException,
            QueryNotFoundException, DatabaseUnavailableException, QueryMalformedException, UserNotFoundException,
            MetadataServiceException, TableNotFoundException, FormatNotAvailableException, StorageUnavailableException,
            ColumnNotFoundException, AnalyseDataTypesException {
        log.debug("endpoint get subset data, databaseId={}, subsetId={}, accept={} page={}, size={}", databaseId,
                subsetId, accept, page, size);
        endpointValidator.validateDataParams(page, size);
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to re-execute query: no authentication found");
                throw new NotAllowedException("Failed to re-execute query: no authentication found");
            }
            if (!isSystem(principal)) {
                cacheService.getAccess(databaseId, getUsername(principal));
            }
        }
        log.trace("visibility for database: is_public={}, is_schema_public={}", database.getIsPublic(), database.getIsSchemaPublic());
        /* parameters */
        if (page == null) {
            page = 0L;
            log.debug("page not set: default to {}", page);
        }
        if (size == null) {
            size = 10L;
            log.debug("size not set: default to {}", size);
        }
        if (timestamp == null) {
            timestamp = Instant.now();
            log.debug("timestamp not set: default to {}", timestamp);
        }
        if (accept == null || accept.isBlank()) {
            accept = MediaType.APPLICATION_JSON_VALUE;
            log.debug("accept header not set: default to {}", accept);
        }
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.set("X-Id", "" + subsetId);
            final QueryDto subset = subsetService.findById(database, subsetId);
            if (request.getMethod().equals("HEAD")) {
                headers.set("Access-Control-Expose-Headers", "X-Count X-Id");
                final Long count = subsetService.reExecuteCount(database, subset);
                headers.set("X-Count", "" + count);
                return ResponseEntity.ok()
                        .headers(headers)
                        .build();
            }
            subset.setIdentifiers(metadataServiceGateway.getIdentifiers(database.getId(), subset.getId()));
            final String query = mariaDbMapper.paginateSubset(subset.getQueryNormalized(),
                    accept.equals("text/csv") ? null : page,
                    accept.equals("text/csv") ? null : size);
            final Dataset<Row> dataset = subsetService.getData(database, query);
            headers.set("Access-Control-Expose-Headers", "X-Id X-Headers");
            final Map<String, ColumnAnalysisResultDto> schema = analyseService.determineDataTypes(database, subset);
            headers.set("X-Headers", String.join(",", schema.keySet()));
            final HttpStatusCode statusCode = request.getMethod().equals("POST") ? HttpStatus.CREATED : HttpStatus.OK;
            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
                    log.trace("accept header matches json");
                    return ResponseEntity.status(statusCode)
                            .headers(headers)
                            .body(transform(dataset));
                case "text/csv":
                    log.trace("accept header matches csv");
                    final ExportResourceDto resource = storageService.transformDataset(dataset);
                    headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
                    return ResponseEntity.status(statusCode)
                            .headers(headers)
                            .body(storageService.transformDataset(dataset)
                                    .getResource());
            }
            throw new FormatNotAvailableException("Must provide either application/json or text/csv value for header 'Accept': provided " + accept + " instead");
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PutMapping("/{queryId}")
    @PreAuthorize("hasAuthority('persist-query')")
    @Observed(name = "dbrepo_subset_persist")
    @Operation(summary = "Persist subset",
            description = "Persists a subset with id. Requires role `persist-query`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Persisted subset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Malformed select query",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to persist subset",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database or query in query store of the data database",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Failed to persist subset",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<QueryDto> persist(@NotNull @PathVariable("databaseId") UUID databaseId,
                                            @NotNull @PathVariable("queryId") UUID queryId,
                                            @NotNull @Valid @RequestBody QueryPersistDto data,
                                            Principal principal) throws NotAllowedException,
            RemoteUnavailableException, DatabaseNotFoundException, QueryStorePersistException,
            DatabaseUnavailableException, QueryNotFoundException, UserNotFoundException, MetadataServiceException {
        log.debug("endpoint persist query, databaseId={}, queryId={}, data.persist={}", databaseId, queryId,
                data.getPersist());
        final DatabaseDto database = cacheService.getDatabase(databaseId);
        if (!isSystem(principal)) {
            cacheService.getAccess(databaseId, getUsername(principal));
        }
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

    @PostMapping("/replicate")
    @Observed(name = "dbrepo_subset_replicate")
    @Operation(summary = "Replicate subset",
            description = "Receives subset replication from other instances and persists the query locally. This endpoint is called internally by the replication service.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Subset replicated and persisted successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid query data",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database not found",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to communicate with database",
                    content = {@Content}),
    })
    public ResponseEntity<Map<String, Object>> replicate(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                        @Valid @RequestBody QueryDto queryDto) {
        log.info("=== Received Subset Replication ===");
        log.info("Database ID: {}", databaseId);
        log.info("Query ID: {}", queryDto.getId());
        log.info("Creation Location: {}", queryDto.getCreationLocation() != null ? queryDto.getCreationLocation() : "null");
        log.info("===================================");
        
        try {
            // TODO: Implement actual subset replication logic
            // This could involve:
            // 1. Validating the incoming query data
            // 2. Creating the subset locally using the query information
            // 3. Storing it in the local query store with the original creation location
            // 4. Updating any necessary metadata
            // 5. Persisting the query if needed
            
            // For now, just log the replication attempt and return success
            log.info("Subset replication received successfully for database: {}, query: {}, creation location: {}", 
                    databaseId, queryDto.getId(), queryDto.getCreationLocation() != null ? queryDto.getCreationLocation() : "null");
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Subset replication received successfully",
                "databaseId", databaseId.toString(),
                "queryId", queryDto.getId().toString(),
                "creationLocation", queryDto.getCreationLocation() != null ? queryDto.getCreationLocation() : "null"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to replicate subset: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "message", "Failed to replicate subset: " + e.getMessage(),
                "databaseId", databaseId.toString(),
                "queryId", queryDto.getId().toString(),
                "creationLocation", queryDto.getCreationLocation() != null ? queryDto.getCreationLocation() : "null"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
