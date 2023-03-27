package at.tuwien.endpoint;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/history")
public class TableHistoryEndpoint {

    private final TableService tableService;

    @Autowired
    public TableHistoryEndpoint(TableService tableService) {
        this.tableService = tableService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('data-history')")
    @Timed(value = "history.list", description = "Time needed to retrieve table history")
    @Operation(summary = "Find all history", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<TableHistoryDto>> getAll(@NotNull @PathVariable("id") Long containerId,
                                                        @NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId,
                                                        @NotNull Principal principal)
            throws TableNotFoundException, QueryMalformedException, DatabaseNotFoundException,
            QueryStoreException, DatabaseConnectionException, UserNotFoundException {
        log.debug("endpoint find all history, containerId={}, databaseid={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        final List<TableHistoryDto> history = tableService.findHistory(containerId, databaseId, tableId, principal);
        log.trace("find all history resulted in history {}", history);
        return ResponseEntity.ok(history);
    }


}
