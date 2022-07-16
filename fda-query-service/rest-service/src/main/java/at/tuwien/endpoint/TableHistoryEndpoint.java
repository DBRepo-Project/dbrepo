package at.tuwien.endpoint;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
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
    @Operation(summary = "Find all history")
    public ResponseEntity<List<TableHistoryDto>> getAll(@NotNull @PathVariable("id") Long containerId,
                                                        @NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId)
            throws TableNotFoundException, QueryMalformedException, DatabaseNotFoundException {
        final List<TableHistoryDto> history = tableService.findHistory(containerId, databaseId, tableId);
        return ResponseEntity.ok(history);
    }


}
