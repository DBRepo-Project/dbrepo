package at.tuwien.endpoint;

import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.mapper.ViewMapper;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
public class ViewEndpoint extends AbstractEndpoint {

    private final ViewMapper viewMapper;
    private final ViewService viewService;
    private final QueryService queryService;
    private final DatabaseService databaseService;

    @Autowired
    public ViewEndpoint(ViewService viewService, DatabaseService databaseService, IdentifierService identifierService,
                        ViewMapper viewMapper, QueryService queryService, TableService tableService,
                        DatabaseAccessRepository databaseAccessRepository) {
        super(tableService, databaseService, identifierService, databaseAccessRepository);
        this.viewService = viewService;
        this.databaseService = databaseService;
        this.viewMapper = viewMapper;
        this.queryService = queryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "view.list", description = "Time needed to list all views in a database")
    @Operation(summary = "Find all views", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ViewBriefDto>> findAll(@NotNull @PathVariable("id") Long containerId,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, UserNotFoundException {
        log.debug("endpoint find all views, containerId={}, databaseId={}, principal={}", containerId,
                databaseId, principal);
        if (!hasDatabasePermission(containerId, databaseId, "LIST_VIEWS", principal)) {
            log.error("Missing list views permission");
            throw new NotAllowedException("Missing list views permission");
        }
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
    @Timed(value = "view.create", description = "Time needed to create a view")
    @Operation(summary = "Create a view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ViewBriefDto> create(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @Valid @RequestBody ViewCreateDto data,
                                               @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException,
            UserNotFoundException {
        log.debug("endpoint create view, containerId={}, databaseId={}, data={}, principal={}", containerId,
                databaseId, data, principal);
        if (!hasDatabasePermission(containerId, databaseId, "CREATE_VIEW", principal)) {
            log.error("Missing list views permission");
            throw new NotAllowedException("Missing list views permission");
        }
        final Database database = databaseService.find(containerId, databaseId);
        log.trace("create view for database {}", database);
        final ViewBriefDto view = viewMapper.viewToViewBriefDto(viewService.create(containerId, databaseId, data, principal));
        log.trace("create view resulted in view {}", view);
        return ResponseEntity.ok(view);
    }

    @GetMapping("/{viewId}")
    @Transactional(readOnly = true)
    @Timed(value = "view.find", description = "Time needed to find a view")
    @Operation(summary = "Find one view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ViewDto> findAll(@NotNull @PathVariable("id") Long containerId,
                                           @NotNull @PathVariable("databaseId") Long databaseId,
                                           @NotNull @PathVariable("viewId") Long viewId,
                                           Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, ViewNotFoundException, UserNotFoundException {
        log.debug("endpoint find view, containerId={}, databaseId={}, viewId={}, principal={}", containerId,
                databaseId, viewId, principal);
        if (!hasDatabasePermission(containerId, databaseId, "FIND_VIEW", principal)) {
            log.error("Missing find views permission");
            throw new NotAllowedException("Missing find views permission");
        }
        final Database database = databaseService.find(containerId, databaseId);
        log.trace("find view for database {}", database);
        final ViewDto view = viewMapper.viewToViewDto(viewService.findById(databaseId, viewId, principal));
        log.trace("find find resulted in view {}", view);
        return ResponseEntity.ok(view);
    }

    @DeleteMapping("/{viewId}")
    @Transactional
    @Timed(value = "view.delete", description = "Time needed to delete a view")
    @Operation(summary = "Delete one view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("viewId") Long viewId,
                                    @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, ViewNotFoundException, UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException {
        log.debug("endpoint delete view, containerId={}, databaseId={}, viewId={}, principal={}", containerId,
                databaseId, viewId, principal);
        if (!hasDatabasePermission(containerId, databaseId, "DELETE_VIEW", principal)) {
            log.error("Missing delete view permission");
            throw new NotAllowedException("Missing delete view permission");
        }
        viewService.delete(containerId, databaseId, viewId, principal);
        return ResponseEntity.accepted()
                .build();
    }

    @GetMapping("/{viewId}/data")
    @Transactional(readOnly = true)
    @Timed(value = "view.data", description = "Time needed to retrieve data from a view")
    @Operation(summary = "Find view data", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> data(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("viewId") Long viewId,
                                               Principal principal,
                                               @RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size)
            throws DatabaseNotFoundException, NotAllowedException, ViewNotFoundException, PaginationException,
            QueryStoreException, DatabaseConnectionException, TableMalformedException, QueryMalformedException,
            ImageNotSupportedException, ColumnParseException, UserNotFoundException, ContainerNotFoundException {
        log.debug("endpoint find view data, containerId={}, databaseId={}, viewId={}, principal={}, page={}, size={}",
                containerId, databaseId, viewId, principal, page, size);
        /* check */
        if (!hasDatabasePermission(containerId, databaseId, "DATA_VIEW", principal)) {
            log.error("Missing view data in view permission");
            throw new NotAllowedException("Missing view data in view permission");
        }
        validateDataParams(page, size);
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        log.trace("find view data for database {}", database);
        final View view = viewService.findById(databaseId, viewId, principal);
        final ExecuteStatementDto statement = ExecuteStatementDto.builder()
                .statement(view.getQuery())
                .build();
        log.trace("find view execute statement {}", statement);
        final QueryResultDto result = queryService.execute(containerId, databaseId, statement,
                QueryTypeDto.VIEW, principal, page, size, null, null);
        log.trace("find view data resulted in result {}", result);
        return ResponseEntity.ok()
                .body(result);
    }

}
