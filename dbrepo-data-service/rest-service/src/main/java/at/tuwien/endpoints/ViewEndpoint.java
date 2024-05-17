package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.ViewService;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;

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

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Create view", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Created a new view",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
    })
    public ResponseEntity<Void> create(@NotNull @PathVariable("databaseId") Long databaseId,
                                       @Valid @RequestBody ViewCreateDto data) throws DatabaseUnavailableException,
            DatabaseNotFoundException, RemoteUnavailableException, ViewMalformedException {
        log.debug("endpoint create view, databaseId={}, data.name={}", databaseId, data.getName());
        final PrivilegedDatabaseDto database = metadataServiceGateway.getDatabaseById(databaseId);
        try {
            viewService.create(database, data);
        } catch (SQLException e) {
            log.error("Failed to establish connection to database: {}", e.getMessage());
            throw new DatabaseUnavailableException("Failed to establish connection to database: " + e.getMessage(), e);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    @DeleteMapping("/{viewId}")
    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Delete view in database", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Deleted table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DatabaseDto.class))}),
    })
    public ResponseEntity<Void> delete(@NotBlank @PathVariable("databaseId") Long databaseId,
                                       @NotBlank @PathVariable("viewId") Long viewId)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException,
            ViewMalformedException {
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
    @PreAuthorize("hasAuthority('view-database-view-data')")
    @Observed(name = "dbrepo_view_data")
    @Operation(summary = "Get view data", security = {@SecurityRequirement(name = "basicAuth"), @SecurityRequirement(name = "bearerAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Returned view data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
    })
    public ResponseEntity<QueryResultDto> getData(@NotBlank @PathVariable("databaseId") Long databaseId,
                                                  @NotBlank @PathVariable("viewId") Long viewId,
                                                  @RequestParam(required = false) Long page,
                                                  @RequestParam(required = false) Long size,
                                                  @RequestParam(required = false) Instant timestamp,
                                                  @NotNull HttpServletRequest request,
                                                  Principal principal)
            throws DatabaseUnavailableException, RemoteUnavailableException, ViewNotFoundException,
            QueryMalformedException, ViewMalformedException, PaginationException, NotAllowedException {
        log.debug("endpoint get view data, databaseId={}, viewId={}, page={}, size={}, timestamp={}", databaseId, viewId,
                page, size, timestamp);
        endpointValidator.validateDataParams(page, size);
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
        final PrivilegedViewDto view = metadataServiceGateway.getViewById(databaseId, viewId);
        metadataServiceGateway.getAccess(databaseId, UserUtil.getId(principal));
        try {
            final Long count = viewService.count(view, timestamp);
            final HttpHeaders headers = new HttpHeaders();
            headers.set("X-Count", "" + count);
            headers.set("Access-Control-Expose-Headers", "X-Count");
            if (request.getMethod().equals("GET")) {
                final QueryResultDto result = viewService.data(view, timestamp, page, size);
                log.trace("get view data resulted in result {}", result);
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

}
