package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.mapper.PostgresMapper;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
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

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/v1/database/{databaseId}/view")
public class ViewEndpoint {

    private final DSLContext context;
    private final ViewService viewService;
    private final TableService tableService;
    private final PostgresMapper postgresMapper;
    private final MetadataService metadataService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public ViewEndpoint(DSLContext context, ViewService viewService, TableService tableService,
                        PostgresMapper postgresMapper, MetadataService metadataService,
                        EndpointValidator endpointValidator) {
        this.context = context;
        this.viewService = viewService;
        this.tableService = tableService;
        this.postgresMapper = postgresMapper;
        this.metadataService = metadataService;
        this.endpointValidator = endpointValidator;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system')")
    @Observed(name = "dbrepo_view_schema_list")
    @Operation(summary = "Find views",
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found view schemas",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewDto[].class))}),
            @ApiResponse(responseCode = "400",
                    description = "Database schema is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/view in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped to known columns",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "View schema could not be retrieved",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<List<ViewDto>> findAll(@NotNull @PathVariable("databaseId") UUID databaseId)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            DatabaseMalformedException, MetadataServiceException, ViewNotFoundException {
        log.debug("endpoint inspect view schemas, databaseId={}", databaseId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            return ResponseEntity.ok(viewService.explore(database));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Create view",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Created view",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "View schema is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database (or table or view) in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<ViewDto> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                          @Valid @RequestBody CreateViewDto data) throws DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException, MetadataServiceException,
            TableNotFoundException, ImageNotFoundException, QueryMalformedException, ViewNotFoundException,
            ColumnNotFoundException {
        log.debug("endpoint create view, databaseId={}, data.name={}", databaseId, data.getName());
        /* check */
        endpointValidator.validateSubsetParams(data.getQuery());
        /* create */
        final Database database = metadataService.getDatabase(databaseId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(viewService.create(database, postgresMapper.nameToInternalName(data.getName()),
                            postgresMapper.subsetDtoToNormalizedQuery(context, database, data.getQuery()), data.getIsMaterialized()));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{viewId}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Delete view",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted view"),
            @ApiResponse(responseCode = "400",
                    description = "Database schema is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find view in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @NotNull @PathVariable("viewId") UUID viewId)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException,
            ViewMalformedException, MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint delete view, databaseId={}, viewId={}", databaseId, viewId);
        final View view = metadataService.getView(databaseId, viewId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            viewService.delete(database, view);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @PatchMapping("/{viewId}")
    @PreAuthorize("hasAuthority('system')")
    @Operation(summary = "Refresh materialized view",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")},
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Refreshed materialized view"),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find view in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "412",
                    description = "View is not materialized",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> refresh(@NotNull @PathVariable("databaseId") UUID databaseId,
                                        @NotNull @PathVariable("viewId") UUID viewId)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException,
            ViewMalformedException, MetadataServiceException, DatabaseNotFoundException {
        log.debug("endpoint refresh view, databaseId={}, viewId={}", databaseId, viewId);
        final View view = metadataService.getView(databaseId, viewId);
        final Database database = metadataService.getDatabase(databaseId);
        try {
            viewService.refresh(database, view);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/{viewId}/data", method = {RequestMethod.GET, RequestMethod.HEAD})
    @Observed(name = "dbrepo_view_data")
    @Operation(summary = "Get view data",
            description = "Gets data from a view of a database. For private databases, the user needs at least *READ* access to the associated database.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved view data",
                    headers = {@Header(name = "X-Count", description = "Number of rows", schema = @Schema(implementation = Long.class), required = true),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose `X-Count` custom header", schema = @Schema(implementation = String.class), required = true)},
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map[].class)),
                            @Content(mediaType = "text/csv")}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to retrieve view data",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find view in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "406",
                    description = "Failed to format data",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<?> getData(@NotNull @PathVariable("databaseId") UUID databaseId,
                                     @NotNull @PathVariable("viewId") UUID viewId,
                                     @RequestParam(required = false) Long page,
                                     @RequestParam(required = false) Long size,
                                     @RequestParam(required = false) Instant timestamp,
                                     @NotNull HttpServletRequest request,
                                     @NotNull @RequestHeader("Accept") String accept,
                                     Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException, PaginationException,
            QueryMalformedException, NotAllowedException, MetadataServiceException, TableNotFoundException,
            DatabaseNotFoundException, ViewMalformedException, FormatNotAvailableException, MalformedException, ColumnNotFoundException, StorageNotFoundException, ImageInvalidException, AnalyseDataTypesException {
        log.debug("endpoint get view data, databaseId={}, viewId={}, page={}, size={}, accept={}, timestamp={}",
                databaseId, viewId, page, size, accept, timestamp);
        endpointValidator.validateDataParams(page, size);
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
        final View view = metadataService.getView(databaseId, viewId);
        final Database database = metadataService.getDatabase(databaseId);
        if (!view.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to get data from view: unauthorized");
                throw new NotAllowedException("Failed to get data from view: unauthorized");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
            }
        }
        try {
            final HttpHeaders headers = new HttpHeaders();
            if (request.getMethod().equals("HEAD")) {
                headers.set("Access-Control-Expose-Headers", "X-Count");
                headers.set("X-Count", "" + viewService.count(database, view, timestamp));
                return ResponseEntity.ok()
                        .headers(headers)
                        .build();
            }
            final String query = postgresMapper.rawSelectQuery(view.getQuery(), timestamp,
                    accept.equals("text/csv") ? null : page,
                    accept.equals("text/csv") ? null : size);
            headers.set("Access-Control-Expose-Headers", "X-Headers");
//            final String viewName = view.getQueryHash();
//            viewService.create(database, viewName, view.getQuery(), false);
            switch (accept) {
                case MediaType.APPLICATION_JSON_VALUE:
//                    final Dataset<Row> dataset1 = dataService.getSubsetAsJson(database, query);
//                    headers.set("X-Headers", String.join(",", dataMapper.datasetToColumnNameHeader(dataset1)));
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(null);
//                            .body(dataMapper.datasetToJson(dataset1));
                case "text/csv":
//                    final Dataset<Row> dataset2 = dataService.getSubsetAsCsv(database, query);
//                    headers.set("X-Headers", String.join(",", dataMapper.datasetToColumnNameHeader(dataset2)));
                    headers.add("Content-Disposition", "attachment; filename=\"dataset.csv\"");
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(null);
//                            .body(dataset2);
            }
            throw new FormatNotAvailableException("Must provide either application/json or text/csv value for header 'Accept': provided " + accept + " instead");
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{viewId}/statistic")
    @Observed(name = "dbrepo_view_statistic")
    @Operation(summary = "Get view statistic",
            description = "Gets basic statistical properties (min, max, mean, median, std.dev) of numerical columns of a view with id.",
            hidden = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Generated view statistic",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableStatisticDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Failed to obtain column statistic",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to get statistic",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find view or database in metadata database",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content}),
    })
    public ResponseEntity<TableStatisticDto> getStatistic(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                          @NotNull @PathVariable("viewId") UUID viewId,
                                                          Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, TableMalformedException, DatabaseNotFoundException, ViewNotFoundException,
            NotAllowedException {
        log.debug("endpoint generate view statistic, databaseId={}, viewId={}", databaseId, viewId);
        final View view = metadataService.getView(databaseId, viewId);
        final Database database = metadataService.getDatabase(databaseId);
        if (!view.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to get statistic from view: unauthorized");
                throw new NotAllowedException("Failed to get statistic from view: unauthorized");
            }
            if (!AuthUtil.isSystem(principal)) {
                endpointValidator.validateOnlyAccess(database, principal);
            }
        }
        try {
            return ResponseEntity.ok(tableService.getStatistics(database, view.getId(), view.getInternalName()));
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
