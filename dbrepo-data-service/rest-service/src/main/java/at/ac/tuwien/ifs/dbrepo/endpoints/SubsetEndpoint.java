package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.api.SubsetMetadata;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryPersistDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.PostgresMapper;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/subset")
public class SubsetEndpoint {

    private final DataMapper dataMapper;
    private final PostgresMapper mariaDbMapper;
    private final SubsetService subsetService;
    private final AnalyseService analyseService;
    private final MetadataMapper metadataMapper;
    private final MetadataService metadataService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public SubsetEndpoint(DataMapper dataMapper, PostgresMapper mariaDbMapper, SubsetService subsetService,
                          AnalyseService analyseService, MetadataMapper metadataMapper, MetadataService metadataService,
                          EndpointValidator endpointValidator, MetadataServiceGateway metadataServiceGateway) {
        this.dataMapper = dataMapper;
        this.mariaDbMapper = mariaDbMapper;
        this.subsetService = subsetService;
        this.analyseService = analyseService;
        this.metadataMapper = metadataMapper;
        this.metadataService = metadataService;
        this.endpointValidator = endpointValidator;
        this.metadataServiceGateway = metadataServiceGateway;
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
        final Database database = metadataService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to list queries: no authentication found");
                throw new NotAllowedException("Failed to list queries: no authentication found");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
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
        final Database database = metadataService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to find query: no authentication found");
                throw new NotAllowedException("Failed to find query: no authentication found");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
            }
        }
        final QueryDto subset;
        try {
            subset = metadataMapper.subsetToQueryDto(subsetService.findById(database, subsetId));
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
            @ApiResponse(responseCode = "422",
                    description = "Failed to execute query",
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
            ColumnNotFoundException, AnalyseDataTypesException, QueryExecutionException, MalformedException, DatabaseMalformedException {
        log.debug("endpoint create subset in database, databaseId={}, page={}, size={}, timestamp={}", databaseId, page,
                size, timestamp);
        /* check */
        endpointValidator.validateDataParams(page, size);
        endpointValidator.validateSubsetParams(data);
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
        /* create */
        final Database database = metadataService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to create subset: no authentication found");
                throw new NotAllowedException("Failed to create subset: no authentication found");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
            }
        }
        try {
            final UUID subsetId = subsetService.create(database, data, timestamp);
            return getData(databaseId, subsetId, principal, "application/json", request, timestamp, page, size);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/{subsetId}/data", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Observed(name = "dbrepo_subset_data")
    @Operation(summary = "Get subset data",
            description = "Gets data of subset with id. For private databases, the user needs at least *READ* grant to " +
                    "the associated database. Requests with HTTP method **GET** return the subset dataset. Requests " +
                    "with HTTP method **HEAD** only the number of rows in the subset dataset in the `X-Count` header, " +
                    "the subset id in the `X-Id` header and the `sha256`-result set hash in the `X-Result-Hash` header. " +
                    "Requests with HTTP method **GET** additionally return the result set in the response body.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved subset data",
                    headers = {@Header(name = "X-Count", description = "Number of result set rows", schema = @Schema(implementation = Long.class)),
                            @Header(name = "X-Result-Hash", description = "Hash of result set", schema = @Schema(implementation = String.class)),
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
            @ApiResponse(responseCode = "422",
                    description = "Failed to re-execute query",
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
            MetadataServiceException, FormatNotAvailableException, ColumnNotFoundException,
            AnalyseDataTypesException, QueryExecutionException, DatabaseMalformedException {
        log.debug("endpoint get subset data, databaseId={}, subsetId={}, accept={} page={}, size={}", databaseId,
                subsetId, accept, page, size);
        endpointValidator.validateDataParams(page, size);
        final Database database = metadataService.getDatabase(databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to re-execute query: no authentication found");
                throw new NotAllowedException("Failed to re-execute query: no authentication found");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
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
            final Subset subset = subsetService.findById(database, subsetId);
            if (request.getMethod().equals("HEAD")) {
                final SubsetMetadata metadata = subsetService.getMetadata(database, subset.getQueryNormalized());
                headers.set("X-Count", "" + metadata.getResultCount());
                headers.set("X-Result-Hash", metadata.getResultHash());
                headers.set("Access-Control-Expose-Headers", "X-Count X-Result-Hash X-Id");
                return ResponseEntity.ok()
                        .headers(headers)
                        .build();
            }
            final QueryDto query = metadataMapper.subsetToQueryDto(subset);
            query.setIdentifiers(metadataServiceGateway.getIdentifiers(database.getId(), subset.getId()));
            headers.set("Access-Control-Expose-Headers", "X-Count X-Result-Hash X-Id X-Headers");
            final Map<String, ColumnAnalysisResultDto> schema = analyseService.determineDataTypes(database, query);
            final Set<String> columns = schema.keySet();
            headers.set("X-Headers", String.join(",", columns));
            final HttpStatusCode statusCode = request.getMethod().equals("POST") ? HttpStatus.CREATED : HttpStatus.OK;
            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
                    final List<Map<String, Object>> body = subsetService.getData(database, columns, subset.getQueryNormalized(), page, size);
                    return ResponseEntity.status(statusCode)
                            .headers(headers)
                            .body(body);
                case "text/csv":
                    headers.add("Content-Disposition", "attachment; filename=\"dataset.csv\"");
                    return ResponseEntity.status(statusCode)
                            .headers(headers)
                            .body(subsetService.getDataAsCsv(database, columns, subset.getQueryNormalized()));
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
        final Database database = metadataService.getDatabase(databaseId);
        if (!AuthUtil.isSystem(principal)) {
            endpointValidator.validateOnlyAccess(database, principal);
        }
        try {
            subsetService.persist(database, queryId, data.getPersist());
            final Subset subset = subsetService.findById(database, queryId);
            log.trace("persist query resulted in query {}", subset);
            return ResponseEntity.accepted()
                    .body(metadataMapper.subsetToQueryDto(subset));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

}
