package at.tuwien.endpoints;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.CreateViewDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.*;
import at.tuwien.validation.EndpointValidator;
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
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
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
import java.util.Map;
import java.util.UUID;

@Log4j2
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/database/{databaseId}/subset")
public class SubsetEndpoint extends RestEndpoint {

    private final MariaDbMapper mariaDbMapper;
    private final SubsetService subsetService;
    private final MetadataMapper metadataMapper;
    private final MetricsService metricsService;
    private final StorageService storageService;
    private final DatabaseService databaseService;
    private final CredentialService credentialService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public SubsetEndpoint(MariaDbMapper mariaDbMapper, SubsetService subsetService, MetadataMapper metadataMapper,
                          MetricsService metricsService, StorageService storageService, DatabaseService databaseService,
                          CredentialService credentialService, EndpointValidator endpointValidator) {
        this.mariaDbMapper = mariaDbMapper;
        this.subsetService = subsetService;
        this.metadataMapper = metadataMapper;
        this.metricsService = metricsService;
        this.storageService = storageService;
        this.databaseService = databaseService;
        this.credentialService = credentialService;
        this.endpointValidator = endpointValidator;
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
    public ResponseEntity<List<QueryDto>> list(@NotNull @PathVariable("databaseId") UUID databaseId,
                                               @RequestParam(name = "persisted", required = false) Boolean filterPersisted,
                                               Principal principal)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, NotAllowedException, MetadataServiceException {
        log.debug("endpoint find subsets in database, databaseId={}, filterPersisted={}", databaseId, filterPersisted);
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        endpointValidator.validateOnlyPrivateSchemaAccess(database, principal);
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
    public ResponseEntity<?> findById(@NotNull @PathVariable("databaseId") UUID databaseId,
                                      @NotNull @PathVariable("subsetId") UUID subsetId,
                                      @NotNull @RequestHeader("Accept") String accept,
                                      @RequestParam(required = false) Instant timestamp,
                                      Principal principal)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, FormatNotAvailableException, StorageUnavailableException, UserNotFoundException,
            MetadataServiceException, TableNotFoundException, QueryMalformedException, NotAllowedException {
        log.debug("endpoint find subset in database, databaseId={}, subsetId={}, accept={}, timestamp={}", databaseId,
                subsetId, accept, timestamp);
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        endpointValidator.validateOnlyPrivateSchemaAccess(database, principal);
        final QueryDto subset;
        try {
            subset = subsetService.findById(database, subsetId);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        /* parameters */
        if (timestamp == null) {
            timestamp = Instant.now();
            log.debug("timestamp not set: default to {}", timestamp);
        }
        if (accept == null || accept.isEmpty() || accept.isBlank()) {
            accept = MediaType.APPLICATION_JSON_VALUE;
            log.debug("accept header not set: default to {}", accept);
        }
        switch (accept) {
            case MediaType.APPLICATION_JSON_VALUE:
                log.trace("accept header matches json");
                return ResponseEntity.ok(subset);
            case "text/csv":
                log.trace("accept header matches csv");
                final String query = mariaDbMapper.rawSelectQuery(subset.getQuery(), timestamp, null, null);
                final Dataset<Row> dataset = subsetService.getData(database, query);
                metricsService.countSubsetGetData(databaseId, subsetId);
                final ExportResourceDto resource = storageService.transformDataset(dataset);
                final HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
                log.trace("export table resulted in resource {}", resource);
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(resource.getResource());
        }
        throw new FormatNotAvailableException("Must provide either application/json or text/csv value for header 'Accept': provided " + accept + " instead");
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
                            schema = @Schema(implementation = List.class))}),
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
    public ResponseEntity<List<Map<String, Object>>> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                            @Valid @RequestBody ExecuteStatementDto data,
                                                            Principal principal,
                                                            @NotNull HttpServletRequest request,
                                                            @RequestParam(required = false) Instant timestamp,
                                                            @RequestParam(required = false) Long page,
                                                            @RequestParam(required = false) Long size)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            QueryNotFoundException, StorageUnavailableException, QueryMalformedException, StorageNotFoundException,
            QueryStoreInsertException, TableMalformedException, PaginationException, QueryNotSupportedException,
            NotAllowedException, UserNotFoundException, MetadataServiceException, TableNotFoundException,
            ViewMalformedException, ViewNotFoundException {
        log.debug("endpoint create subset in database, databaseId={}, data.statement={}, page={}, size={}, " +
                        "timestamp={}", databaseId, data.getStatement(), page, size,
                timestamp);
        /* check */
        endpointValidator.validateDataParams(page, size);
        endpointValidator.validateForbiddenStatements(data.getStatement());
        /* parameters */
        final UUID userId;
        if (principal != null) {
            userId = getId(principal);
        } else {
            userId = null;
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
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        endpointValidator.validateOnlyPrivateSchemaAccess(database, principal);
        try {
            final UUID subsetId = subsetService.create(database, data.getStatement(), timestamp, userId);
            return getData(databaseId, subsetId, principal, request, timestamp, page, size);
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
                            schema = @Schema(implementation = List.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid pagination",
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
    public ResponseEntity<List<Map<String, Object>>> getData(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                             @NotNull @PathVariable("subsetId") UUID subsetId,
                                                             Principal principal,
                                                             @NotNull HttpServletRequest request,
                                                             @RequestParam(required = false) Instant timestamp,
                                                             @RequestParam(required = false) Long page,
                                                             @RequestParam(required = false) Long size)
            throws PaginationException, DatabaseNotFoundException, RemoteUnavailableException, NotAllowedException,
            QueryNotFoundException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            UserNotFoundException, MetadataServiceException, TableNotFoundException, ViewNotFoundException,
            ViewMalformedException {
        log.debug("endpoint get subset data, databaseId={}, subsetId={}, principal.name={} page={}, size={}",
                databaseId, subsetId, principal != null ? principal.getName() : null, page, size);
        endpointValidator.validateDataParams(page, size);
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to re-execute query: no authentication found");
                throw new NotAllowedException("Failed to re-execute query: no authentication found");
            }
            credentialService.getAccess(databaseId, getId(principal));
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
            final String query = mariaDbMapper.rawSelectQuery(subset.getQuery(), timestamp, page, size);
            final Dataset<Row> dataset = subsetService.getData(database, query);
            metricsService.countSubsetGetData(databaseId, subsetId);
            final String viewName = metadataMapper.queryDtoToViewName(subset);
            databaseService.createView(database, CreateViewDto.builder()
                    .name(viewName)
                    .isPublic(false)
                    .isSchemaPublic(false)
                    .query(query)
                    .build());
            final ViewDto view = databaseService.inspectView(database, viewName);
            headers.set("Access-Control-Expose-Headers", "X-Id X-Headers");
            headers.set("X-Headers", String.join(",", view.getColumns().stream().map(ViewColumnDto::getInternalName).toList()));
            return ResponseEntity.status(request.getMethod().equals("POST") ? HttpStatus.CREATED : HttpStatus.OK)
                    .headers(headers)
                    .body(transform(dataset));
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
    public ResponseEntity<QueryDto> persist(@NotNull @PathVariable("databaseId") UUID databaseId,
                                            @NotNull @PathVariable("queryId") UUID queryId,
                                            @NotNull @Valid @RequestBody QueryPersistDto data,
                                            @NotNull Principal principal) throws NotAllowedException,
            RemoteUnavailableException, DatabaseNotFoundException, QueryStorePersistException,
            DatabaseUnavailableException, QueryNotFoundException, UserNotFoundException, MetadataServiceException {
        log.debug("endpoint persist query, databaseId={}, queryId={}, data.persist={}, principal.name={}", databaseId,
                queryId, data.getPersist(), principal.getName());
        final DatabaseDto database = credentialService.getDatabase(databaseId);
        credentialService.getAccess(databaseId, getId(principal));
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
