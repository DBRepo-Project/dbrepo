package at.tuwien.endpoint;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.*;
import at.tuwien.service.*;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
                                IdentifierService identifierService, AccessService accessService,
                                QueryConfig queryConfig) {
        super(tableService, accessService, databaseService, identifierService, queryConfig);
        this.tableService = tableService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Timed(value = "history.list", description = "Time needed to retrieve table history")
    @Operation(summary = "Find all history", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find table history successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableHistoryDto[].class))}),
            @ApiResponse(responseCode = "400",
                    description = "Table history query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database or user could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Find table history is not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "503",
                    description = "Connection to the database failed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "504",
                    description = "Query store failed to query table history",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableHistoryDto>> getAll(@NotNull @PathVariable("id") Long containerId,
                                                        @NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId,
                                                        @NotNull Principal principal)
            throws TableNotFoundException, QueryMalformedException, DatabaseNotFoundException, NotAllowedException,
            QueryStoreException, DatabaseConnectionException, UserNotFoundException {
        log.debug("endpoint find all history, containerId={}, databaseid={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "DATA_HISTORY", principal)) {
            log.error("Missing data history permission");
            throw new NotAllowedException("Missing data history permission");
        }
        final List<TableHistoryDto> history = tableService.findHistory(containerId, databaseId, tableId, principal);
        log.trace("find all history resulted in history {}", history);
        return ResponseEntity.ok(history);
    }


}
