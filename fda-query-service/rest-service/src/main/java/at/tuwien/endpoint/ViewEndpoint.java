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
    @Operation(summary = "Find all views", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<ViewBriefDto>> findAll(@NotNull @PathVariable("id") Long containerId,
                                                      @NotNull @PathVariable("databaseId") Long databaseId,
                                                      Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, UserNotFoundException {
        if (!hasDatabasePermission(containerId, databaseId, "LIST_VIEWS", principal)) {
            log.error("Missing list views permission");
            throw new NotAllowedException("Missing list views permission");
        }
        final Database database = databaseService.find(containerId, databaseId);
        final List<ViewBriefDto> views = viewService.findAll(databaseId, principal)
                .stream()
                .map(viewMapper::viewToViewBriefDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create a view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ViewBriefDto> create(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @Valid @RequestBody ViewCreateDto data,
                                               @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException,
            UserNotFoundException {
        if (!hasDatabasePermission(containerId, databaseId, "CREATE_VIEW", principal)) {
            log.error("Missing list views permission");
            throw new NotAllowedException("Missing list views permission");
        }
        final Database database = databaseService.find(containerId, databaseId);
        final ViewBriefDto view = viewMapper.viewToViewBriefDto(viewService.create(containerId, databaseId, data, principal));
        return ResponseEntity.ok(view);
    }

    @GetMapping("/{viewId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find one view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ViewDto> findAll(@NotNull @PathVariable("id") Long containerId,
                                           @NotNull @PathVariable("databaseId") Long databaseId,
                                           @NotNull @PathVariable("viewId") Long viewId,
                                           Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, ViewNotFoundException, UserNotFoundException {
        if (!hasDatabasePermission(containerId, databaseId, "FIND_VIEW", principal)) {
            log.error("Missing find views permission");
            throw new NotAllowedException("Missing find views permission");
        }
        final Database database = databaseService.find(containerId, databaseId);
        final ViewDto view = viewMapper.viewToViewDto(viewService.findById(databaseId, viewId, principal));
        return ResponseEntity.ok(view);
    }

    @DeleteMapping("/{viewId}")
    @Transactional
    @Operation(summary = "Delete one view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long containerId,
                                    @NotNull @PathVariable("databaseId") Long databaseId,
                                    @NotNull @PathVariable("viewId") Long viewId,
                                    @NotNull Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, ViewNotFoundException, UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException {
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
        /* check */
        if (!hasDatabasePermission(containerId, databaseId, "DATA_VIEW", principal)) {
            log.error("Missing view data in view permission");
            throw new NotAllowedException("Missing view data in view permission");
        }
        validateDataParams(page, size);
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final View view = viewService.findById(databaseId, viewId, principal);
        final ExecuteStatementDto statement = ExecuteStatementDto.builder()
                .statement(view.getQuery())
                .build();
        final QueryResultDto response = queryService.execute(containerId, databaseId, statement,
                QueryTypeDto.VIEW, principal, page, size, null, null);
        return ResponseEntity.ok()
                .body(response);
    }

}
