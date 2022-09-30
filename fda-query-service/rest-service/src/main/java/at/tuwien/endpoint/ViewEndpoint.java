package at.tuwien.endpoint;

import at.tuwien.SortType;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.QueryService;
import at.tuwien.service.ViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/view")
public class ViewEndpoint extends AbstractEndpoint {

    private final ViewService viewService;
    private final QueryService queryService;
    private final DatabaseService databaseService;

    @Autowired
    public ViewEndpoint(ViewService viewService, DatabaseService databaseService, IdentifierService identifierService,
                        QueryService queryService) {
        super(databaseService, identifierService);
        this.viewService = viewService;
        this.databaseService = databaseService;
        this.queryService = queryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Find all views", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<View>> findAll(@NotNull @PathVariable("id") Long containerId,
                                              @NotNull @PathVariable("databaseId") Long databaseId,
                                              Principal principal) throws DatabaseNotFoundException,
            NotAllowedException {
        if (!hasDatabasePermission(containerId, databaseId, "LIST_VIEWS", principal)) {
            log.error("Missing list views permission");
            throw new NotAllowedException("Missing list views permission");
        }
        final Database database = databaseService.find(containerId, databaseId);
        final List<View> views = viewService.findAll(databaseId);
        return ResponseEntity.ok(views);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create a view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<View> create(@NotNull @PathVariable("id") Long containerId,
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
        final View view = viewService.create(containerId, databaseId, data, principal);
        return ResponseEntity.ok(view);
    }

    @GetMapping("/{viewId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find one view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<View> findAll(@NotNull @PathVariable("id") Long containerId,
                                        @NotNull @PathVariable("databaseId") Long databaseId,
                                        @NotNull @PathVariable("viewId") Long viewId,
                                        Principal principal) throws DatabaseNotFoundException,
            NotAllowedException, ViewNotFoundException {
        if (!hasDatabasePermission(containerId, databaseId, "FIND_VIEW", principal)) {
            log.error("Missing find views permission");
            throw new NotAllowedException("Missing find views permission");
        }
        final Database database = databaseService.find(containerId, databaseId);
        final View view = viewService.findById(databaseId, viewId);
        return ResponseEntity.ok(view);
    }

    @GetMapping("/{viewId}/data")
    @Transactional(readOnly = true)
    @Operation(summary = "Find one view", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<QueryResultDto> data(@NotNull @PathVariable("id") Long containerId,
                                               @NotNull @PathVariable("databaseId") Long databaseId,
                                               @NotNull @PathVariable("viewId") Long viewId,
                                               Principal principal,
                                               @RequestParam(required = false) Long page,
                                               @RequestParam(required = false) Long size)
            throws DatabaseNotFoundException, NotAllowedException, ViewNotFoundException, PaginationException,
            SortException, QueryStoreException, DatabaseConnectionException, TableMalformedException,
            QueryMalformedException, ImageNotSupportedException, ColumnParseException {
        /* check */
        if (!hasDatabasePermission(containerId, databaseId, "DATA_VIEW", principal)) {
            log.error("Missing find views permission");
            throw new NotAllowedException("Missing find views permission");
        }
        validateDataParams(page, size);
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        final View view = viewService.findById(databaseId, viewId);
        final Long count = viewService.count(containerId, databaseId, viewId);
        final HttpHeaders headers = new HttpHeaders();
        headers.set("FDA-COUNT", count.toString());
        final QueryResultDto response = queryService.reExecute(containerId, databaseId, view, page, size);
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }

}
