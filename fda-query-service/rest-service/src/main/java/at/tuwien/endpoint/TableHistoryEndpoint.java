package at.tuwien.endpoint;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/history")
public class TableHistoryEndpoint extends AbstractEndpoint {

    private final TableService tableService;

    @Autowired
    public TableHistoryEndpoint(TableService tableService, DatabaseService databaseService,
                                IdentifierService identifierService) {
        super(databaseService, identifierService);
        this.tableService = tableService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Operation(summary = "Find all history")
    public ResponseEntity<List<TableHistoryDto>> getAll(@NotNull @PathVariable("id") Long containerId,
                                                        @NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId,
                                                        @NotNull Principal principal)
            throws TableNotFoundException, QueryMalformedException, DatabaseNotFoundException, NotAllowedException,
            QueryStoreException, DatabaseConnectionException {
        if (!hasDatabasePermission(containerId, databaseId, "DATA_HISTORY", principal)) {
            log.error("Missing data history permission");
            throw new NotAllowedException("Missing data history permission");
        }
        final List<TableHistoryDto> history = tableService.findHistory(containerId, databaseId, tableId);
        return ResponseEntity.ok(history);
    }


}
