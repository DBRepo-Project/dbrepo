package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.DashboardService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/v1/database/{databaseId}/view")
public class ViewEndpoint extends AbstractEndpoint {

    private final UserService userService;
    private final ViewService viewService;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final DashboardService dashboardService;
    private final ReplicationService replicationService;

    @Autowired
    public ViewEndpoint(UserService userService, ViewService viewService, MetadataMapper metadataMapper,
                        DatabaseService databaseService, DashboardService dashboardService,
                        ReplicationService replicationService) {
        this.userService = userService;
        this.viewService = viewService;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.dashboardService = dashboardService;
        this.replicationService = replicationService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_views_findall")
    @Operation(summary = "List views",
            description = "Lists views known to the metadata database.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find views successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ViewBriefDto.class)))}),
            @ApiResponse(responseCode = "404",
                    description = "Database or user could not be found",
                    content = {@Content}),
    })
    public ResponseEntity<List<ViewBriefDto>> findAll(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                      Principal principal) throws UserNotFoundException,
            DatabaseNotFoundException {
        log.debug("endpoint find all views, databaseId={}", databaseId);
        final Database database = databaseService.findById(databaseId);
        return ResponseEntity.ok(filterViews(database, principal)
                .stream()
                .map(metadataMapper::viewToViewBriefDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/replicated")
    @Transactional
    @Observed(name = "dbrepo_view_create_replicated")
    @Operation(summary = "Create replicated view",
            description = "Creates a view received via replication. Preserves original view ID and metadata.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Create replicated view successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewBriefDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database.",
                    content = {@Content}),
    })
    public ResponseEntity<ViewBriefDto> createReplicated(@NotNull @PathVariable("databaseId") UUID databaseId,
                                                         @NotNull @Valid @RequestBody ViewNotificationDto data,
                                                         Principal principal) throws DatabaseNotFoundException, UserNotFoundException, DashboardServiceException, DashboardServiceConnectionException, DataServiceConnectionException, DataServiceException, SearchServiceException, SearchServiceConnectionException {
        log.debug("endpoint create replicated view, databaseId={}, creationId={}", databaseId, data.getCreationId());
        final Database database = databaseService.findById(databaseId);

        // For replicated creation, use internal service account if present, else fallback to principal
        final String username = principal != null ? getUsername(principal) : database.getOwner().getUsername();
        final View view = viewService.createReplicated(database, userService.findByUsername(username), data.getViewDto());
        dashboardService.update(view.getDatabase());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.viewToViewBriefDto(view));
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('create-database-view')")
    @Observed(name = "dbrepo_view_create")
    @Operation(summary = "Create view",
            description = "Creates a view. This can only be performed by the database owner. Requires role `create-database-view`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Create view successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewBriefDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Create view query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Credentials missing",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Failed to find database/user in metadata database.",
                    content = {@Content}),
            @ApiResponse(responseCode = "409",
                    description = "View exists with name",
                    content = {@Content}),
            @ApiResponse(responseCode = "423",
                    description = "Create view resulted in an invalid query statement",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<ViewBriefDto> create(@NotNull @PathVariable("databaseId") UUID databaseId,
                                               @NotNull @Valid @RequestBody CreateViewDto data,
                                               Principal principal) throws NotAllowedException,
            MalformedException, DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            UserNotFoundException, SearchServiceException, SearchServiceConnectionException, TableNotFoundException,
            ImageNotFoundException, ViewExistsException, DashboardServiceException, DashboardServiceConnectionException, ColumnNotFoundException {
        log.debug("endpoint create view, databaseId={}, data.name={}", databaseId, data.getName());
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
            log.error("Failed to create view: not the database owner");
            throw new NotAllowedException("Failed to create view: not the database owner");
        }
        if (database.getViews().stream().anyMatch(v -> v.getInternalName().equals(metadataMapper.nameToInternalName(data.getName())))) {
            log.error("Failed to create view: name exists");
            throw new ViewExistsException("Failed to create view: name exists");
        }
        final View view = viewService.create(database, userService.findByUsername(getUsername(principal)), data);
        dashboardService.update(view.getDatabase());

        // Notify replication service asynchronously similar to database/table flows
        try {
            replicationService.replicateView(view);
        } catch (Exception e) {
            log.warn("Failed to notify replication service for view {}: {}", view.getId(), e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.viewToViewBriefDto(view));
    }

    @GetMapping("/{viewId}")
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_view_find")
    @Operation(summary = "Get view",
            description = "Gets a view with id in the metadata database.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find view successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ViewDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Find view is not permitted",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database, view or user could not be found",
                    content = {@Content}),
    })
    public ResponseEntity<ViewDto> find(@NotNull @PathVariable("databaseId") UUID databaseId,
                                        @NotNull @PathVariable("viewId") UUID viewId,
                                        Principal principal) throws DatabaseNotFoundException,
            ViewNotFoundException, NotAllowedException {
        log.debug("endpoint find view, databaseId={}, viewId={}", databaseId, viewId);
        final Database database = databaseService.findById(databaseId);
        final View view = viewService.findById(database, viewId);
        if (principal != null) {
            if (isSystem(principal)) {
                return ResponseEntity.ok(metadataMapper.viewToViewDto(view));
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUser().getUsername().equals(getUsername(principal)))
                    .findFirst();
            if (view.getIsPublic() || view.getIsSchemaPublic() || optional.isPresent()) {
                return ResponseEntity.ok(metadataMapper.viewToViewDto(view));
            }
        }
        if (!view.getIsPublic() && !view.getIsSchemaPublic()) {
            log.error("Failed to find view: not public and no access found");
            throw new NotAllowedException("Failed to find view: not public and no access found");
        }
        return ResponseEntity.ok(metadataMapper.viewToViewDto(view));
    }

    @DeleteMapping("/{viewId}")
    @Transactional
    @PreAuthorize("hasAuthority('delete-database-view')")
    @Observed(name = "dbrepo_view_delete")
    @Operation(summary = "Delete view",
            description = "Deletes a view with id. Requires role `delete-database-view`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Delete view successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Delete view query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Deletion not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database, view or user could not be found",
                    content = {@Content}),
            @ApiResponse(responseCode = "423",
                    description = "Delete view resulted in an invalid query statement",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("databaseId") UUID databaseId,
                                       @NotNull @PathVariable("viewId") UUID viewId,
                                       Principal principal) throws NotAllowedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, ViewNotFoundException, SearchServiceException,
            SearchServiceConnectionException, UserNotFoundException, DashboardServiceException,
            DashboardServiceConnectionException {
        log.debug("endpoint delete view, databaseId={}, viewId={}", databaseId, viewId);
        final Database database = databaseService.findById(databaseId);
        if (!database.getOwner().getUsername().equals(getUsername(principal))) {
            log.error("Failed to delete view: not the database owner {}", database.getOwner().getUsername());
            throw new NotAllowedException("Failed to delete view: not the database owner " + database.getOwner().getUsername());
        }
        final View view = viewService.findById(database, viewId);
        viewService.delete(view);
        dashboardService.update(databaseService.findById(databaseId));
        return ResponseEntity.accepted()
                .build();
    }

    @PutMapping("/{viewId}")
    @Transactional
    @PreAuthorize("hasAuthority('modify-view-visibility')")
    @Observed(name = "dbrepo_view_update")
    @Operation(summary = "Update view",
            description = "Updates a view with id. This can only be performed by the view owner or database owner. Requires role `create-database-view`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Update view successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Update view query is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "403",
                    description = "Update not allowed",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Database or View could not be found",
                    content = {@Content}),
            @ApiResponse(responseCode = "502",
                    description = "Connection to search service failed",
                    content = {@Content}),
            @ApiResponse(responseCode = "503",
                    description = "Failed to save in search service",
                    content = {@Content}),
    })
    public ResponseEntity<ViewBriefDto> update(@NotNull @PathVariable("databaseId") UUID databaseId,
                                               @NotNull @PathVariable("viewId") UUID viewId,
                                               @NotNull @Valid @RequestBody ViewUpdateDto data,
                                               Principal principal) throws NotAllowedException,
            DataServiceConnectionException, DatabaseNotFoundException, ViewNotFoundException, SearchServiceException,
            SearchServiceConnectionException, UserNotFoundException, DashboardServiceException,
            DashboardServiceConnectionException {
        log.debug("endpoint update view, databaseId={}, viewId={}", databaseId, viewId);
        final Database database = databaseService.findById(databaseId);
        final View view = viewService.findById(database, viewId);
        if (!database.getOwner().getUsername().equals(getUsername(principal)) && !view.getOwner().getUsername().equals(getUsername(principal))) {
            log.error("Failed to update view: not the database- or view owner");
            throw new NotAllowedException("Failed to update view: not the database- or view owner");
        }
        final View view1 = viewService.update(view, data);
        dashboardService.update(view1.getDatabase());
        return ResponseEntity.accepted()
                .body(metadataMapper.viewToViewBriefDto(view1));
    }

}
