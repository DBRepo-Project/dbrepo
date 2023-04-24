package at.tuwien.endpoint;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.service.*;
import at.tuwien.validation.EndpointValidator;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/view")
public class ViewEndpoint {

    private final ViewMapper viewMapper;
    private final ViewService viewService;
    private final QueryService queryService;
    private final DatabaseService databaseService;
    private final EndpointValidator endpointValidator;

    @Autowired
    public ViewEndpoint(ViewService viewService, DatabaseService databaseService,
                        ViewMapper viewMapper, QueryService queryService, EndpointValidator endpointValidator) {
        this.viewService = viewService;
        this.databaseService = databaseService;
        this.viewMapper = viewMapper;
        this.queryService = queryService;
        this.endpointValidator = endpointValidator;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "view.list", description = "Time needed to list all views in a database")
    @Operation(summary = "Find all views", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find views successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewBriefDto[].class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find views is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<ViewBriefDto>> findAll(@NotNull @PathVariable("id") Long containerId,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException {
        log.debug("endpoint find all views, containerId={}, databaseId={}, principal={}", containerId,
                databaseId, principal);
        final Database database = databaseService.find(containerId, databaseId);
        log.trace("find all views for database {}", database);
        final List<ViewBriefDto> views = viewService.findAll(databaseId, principal)
                .stream()
                .map(viewMapper::viewToViewBriefDto)
                .collect(Collectors.toList());
        log.trace("find all views resulted in views {}", views);
        return ResponseEntity.ok(views);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('create-database-view')")
    @Timed(value = "view.create", description = "Time needed to create a view")
    @Operation(summary = "Create a view", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Create view successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Create view query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "401",
                    description = "Credentials missing",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Credentials missing",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Create view is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Create view resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ViewBriefDto> create(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @Valid @RequestBody ViewCreateDto data,
                                               @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException,
            UserNotFoundException {
        log.debug("endpoint create view, containerId={}, databaseId={}, data={}, principal={}", containerId,
                databaseId, data, principal);
        /* check */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getOwner().equalsPrincipal(principal)) {
            log.error("Failed to create view: not the database owner");
            throw new NotAllowedException("Failed to create view: not the database owner");
        }
        log.trace("create view for database {}", database);
        final View view;
        view = viewService.create(containerId, databaseId, data, principal);
        final ViewBriefDto dto = viewMapper.viewToViewBriefDto(view);
        log.trace("create view resulted in view {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @GetMapping("/{viewId}")
    @Transactional(readOnly = true)
    @Timed(value = "view.find", description = "Time needed to find a view")
    @Operation(summary = "Find one view", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find view successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, view or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find view is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<ViewDto> find(@NotNull @PathVariable("id") Long containerId,
                                        @NotNull @PathVariable("databaseId") Long databaseId,
                                        @NotNull @PathVariable("viewId") Long viewId,
                                        Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, ViewNotFoundException, UserNotFoundException {
        log.debug("endpoint find view, containerId={}, databaseId={}, viewId={}, principal={}", containerId,
                databaseId, viewId, principal);
        final Database database = databaseService.find(containerId, databaseId);
        log.trace("find view for database {}", database);
        final ViewDto view = viewMapper.viewToViewDto(viewService.findById(databaseId, viewId, principal));
        log.trace("find find resulted in view {}", view);
        return ResponseEntity.ok(view);
    }

    @DeleteMapping("/{viewId}")
    @Transactional
    @PreAuthorize("hasAuthority('delete-database-view')")
    @Timed(value = "view.delete", description = "Time needed to delete a view")
    @Operation(summary = "Delete one view", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Delete view successfully",
                    content = {@Content}),
            @ApiResponse(responseCode = "400",
                    description = "Delete view query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "401",
                    description = "Credentials missing",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Credentials missing",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, view or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Delete view is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Delete view resulted in an invalid query statement",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("viewId") Long viewId,
                                    @NotNull Principal principal) throws DatabaseNotFoundException,
            ViewNotFoundException, UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException, NotAllowedException {
        log.debug("endpoint delete view, containerId={}, databaseId={}, viewId={}, principal={}", containerId,
                databaseId, viewId, principal);
        /* check */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getOwner().equalsPrincipal(principal)) {
            log.error("Failed to delete view: not the database owner");
            throw new NotAllowedException("Failed to delete view: not the database owner");
        }
        viewService.delete(containerId, databaseId, viewId, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @GetMapping("/{viewId}/data")
    @Transactional(readOnly = true)
    @Timed(value = "view.data", description = "Time needed to retrieve data from a view")
    @Operation(summary = "Find view data", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find data successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = QueryResultDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Pagination not in valid range or find data query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "401",
                    description = "Credentials missing",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Credentials missing",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Database, view, container or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find data is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Parsing of resulting columns failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "423",
                    description = "Find data resulted in an invalid query statement",
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
                    description = "Query store failed to query view data",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<QueryResultDto> data(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("viewId") Long viewId,
                                               Principal principal,
                                               @RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size)
            throws DatabaseNotFoundException, NotAllowedException, ViewNotFoundException, PaginationException,
            QueryStoreException, DatabaseConnectionException, TableMalformedException, QueryMalformedException,
            ImageNotSupportedException, ColumnParseException, UserNotFoundException, ContainerNotFoundException, ViewMalformedException {
        log.debug("endpoint find view data, containerId={}, databaseId={}, viewId={}, principal={}, page={}, size={}",
                containerId, databaseId, viewId, principal, page, size);
        /* check */
        endpointValidator.validateDataParams(page, size);
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getIsPublic()) {
            if (principal == null) {
                log.error("Failed to view data of private view: principal is null");
                throw new NotAllowedException("Failed to view data of private view: principal is null");
            }
            if (!User.hasRole(principal, "view-database-view-data")) {
                log.error("Failed to view data of private view: role missing");
                throw new NotAllowedException("Failed to view data of private view: role missing");
            }
        }
        /* find */
        log.trace("find view data for database {}", database);
        final View view = viewService.findById(databaseId, viewId, principal);
        final QueryResultDto result = queryService.viewFindAll(containerId, databaseId, view, page, size, principal);
        log.trace("execute view {}", view);
        log.trace("find view data resulted in result {}", result);
        return ResponseEntity.ok()
                .body(result);
    }

    @GetMapping("/{viewId}/data/count")
    @Transactional(readOnly = true)
    @Timed(value = "view.data.count", description = "Time needed to retrieve data count from a view")
    @Operation(summary = "Find view data count", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Long> count(@NotNull @PathVariable("id") Long containerId,
                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                      @NotNull @PathVariable("viewId") Long viewId,
                                      Principal principal)
            throws DatabaseNotFoundException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            TableMalformedException, QueryMalformedException, ImageNotSupportedException, UserNotFoundException,
            ContainerNotFoundException {
        log.debug("endpoint find view data count, containerId={}, databaseId={}, viewId={}, principal={}",
                containerId, databaseId, viewId, principal);
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        log.trace("find view data for database {}", database);
        final View view = viewService.findById(databaseId, viewId, principal);
        final Long result = queryService.viewCount(containerId, databaseId, view, principal);
        log.trace("execute view {}", view);
        log.trace("find view data resulted in result {}", result);
        return ResponseEntity.ok()
                .body(result);
    }

}
