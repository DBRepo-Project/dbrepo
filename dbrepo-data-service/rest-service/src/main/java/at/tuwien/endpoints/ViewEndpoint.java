package at.tuwien.endpoints;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.ViewService;
import at.tuwien.utils.UserUtil;
import at.tuwien.validation.EndpointValidator;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
@RequestMapping(path = "/api/database/{databaseId}/view")
public class ViewEndpoint {

    private final ViewService viewService;
    private final EndpointValidator endpointValidator;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public ViewEndpoint(ViewService viewService, EndpointValidator endpointValidator,
                        MetadataServiceGateway metadataServiceGateway) {
        this.viewService = viewService;
        this.endpointValidator = endpointValidator;
        this.metadataServiceGateway = metadataServiceGateway;
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/view in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped to known columns",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "View schema could not be retrieved",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<ViewDto>> getSchema(@NotBlank @PathVariable("databaseId") Long databaseId)
            throws DatabaseUnavailableException, DatabaseNotFoundException, RemoteUnavailableException,
            ViewNotFoundException, DatabaseMalformedException, MetadataServiceException {
        log.debug("endpoint inspect view schemas, databaseId={}", databaseId);
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        try {
            return ResponseEntity.ok(viewService.getSchemas(database));
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ViewDto> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                          @Valid @RequestBody ViewCreateDto data) throws DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException, MetadataServiceException {
        log.debug("endpoint create view, databaseId={}, data.name={}", databaseId, data.getName());
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(viewService.create(database, data));
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
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find view in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotBlank @PathVariable("databaseId") Long databaseId,
                                       @NotBlank @PathVariable("viewId") Long viewId)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException,
            ViewMalformedException, MetadataServiceException {
        log.debug("endpoint delete view, databaseId={}, viewId={}", databaseId, viewId);
        final PrivilegedViewDto view = metadataServiceGateway.getViewById(databaseId, viewId);
        try {
            viewService.delete(view);
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
            description = "Gets data from a view of a database. For private databases, the user needs at least *READ* access to the associated database. Requires role `view-database-view-data`.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Retrieved view data",
                    headers = {@Header(name = "X-Count", description = "Number of rows", schema = @Schema(implementation = Long.class), required = true),
                            @Header(name = "Access-Control-Expose-Headers", description = "Expose `X-Count` custom header", schema = @Schema(implementation = String.class), required = true)},
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Not allowed to retrieve view data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find view in metadata database",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "409",
                    description = "View schema could not be mapped",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryResultDto> getData(@NotBlank @PathVariable("databaseId") Long databaseId,
                                                  @NotBlank @PathVariable("viewId") Long viewId,
                                                  @RequestParam(required = false) Long page,
                                                  @RequestParam(required = false) Long size,
                                                  @RequestParam(required = false) Instant timestamp,
                                                  @NotNull HttpServletRequest request,
                                                  Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException,
            QueryMalformedException, ViewMalformedException, PaginationException, NotAllowedException,
            MetadataServiceException {
        log.debug("endpoint get view data, databaseId={}, viewId={}, page={}, size={}, timestamp={}", databaseId,
                viewId, page, size, timestamp);
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
        final PrivilegedViewDto view = metadataServiceGateway.getViewById(databaseId, viewId);
        if (!view.getIsPublic()) {
            metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        }
        try {
            if (request.getMethod().equals("HEAD")) {
                final HttpHeaders headers = new HttpHeaders();
                headers.set("Access-Control-Expose-Headers", "X-Count");
                headers.set("X-Count", "" + viewService.count(view, timestamp));
                return ResponseEntity.ok()
                        .headers(headers)
                        .build();
            }
            final QueryResultDto result = viewService.data(view, timestamp, page, size);
            log.trace("get view data resulted in result {}", result);
            return ResponseEntity.ok()
                    .body(result);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{viewId}/export")
    @Observed(name = "dbrepo_view_data_export")
    @Operation(summary = "Get view data",
            description = "Gets data from view with id as downloadable file. For tables in private databases, the user needs to have at least *READ* access to the associated database.",
            security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Exported view data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InputStreamResource.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Request pagination or view data select query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Export view data not allowed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find view in metadata database or export dataset",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to establish connection with the metadata service",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<InputStreamResource> exportDataset(@NotBlank @PathVariable("databaseId") Long databaseId,
                                                             @NotBlank @PathVariable("viewId") Long viewId,
                                                             Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException,
            NotAllowedException, MetadataServiceException, StorageUnavailableException, QueryMalformedException,
            SidecarExportException, StorageNotFoundException {
        log.debug("endpoint export view data, databaseId={}, viewId={}", databaseId, viewId);
        /* parameters */
        final PrivilegedViewDto view = metadataServiceGateway.getViewById(databaseId, viewId);
        if (!view.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to export private view: principal is null");
                throw new NotAllowedException("Failed to export private view: principal is null");
            }
            metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        }
        try {
            final HttpHeaders headers = new HttpHeaders();
            final ExportResourceDto resource = viewService.exportDataset(view);
            headers.add("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"");
            log.trace("export table resulted in resource {}", resource);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource.getResource());

        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database", e);
        }
    }

}
