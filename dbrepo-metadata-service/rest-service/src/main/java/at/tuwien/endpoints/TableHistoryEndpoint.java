package at.tuwien.endpoints;

import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.exception.*;
import at.tuwien.service.TableService;
import at.tuwien.utils.PrincipalUtil;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/database/{databaseId}/table/{tableId}/history")
public class TableHistoryEndpoint {

    private final TableService tableService;

    @Autowired
    public TableHistoryEndpoint(TableService tableService) {
        this.tableService = tableService;
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
    @Transactional(readOnly = true)
    @Observed(name = "dbr_table_history_findall")
    @Operation(summary = "Find all history", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find table history successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableHistoryDto.class)))}),
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
    public ResponseEntity<List<TableHistoryDto>> getAll(@NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId,
                                                        @NotNull Principal principal)
            throws TableNotFoundException, QueryMalformedException, DatabaseNotFoundException,
            QueryStoreException, DatabaseConnectionException, UserNotFoundException {
        log.debug("endpoint find all history, databaseId={}, tableId={}, {}", databaseId, tableId, PrincipalUtil.formatForDebug(principal));
        final List<TableHistoryDto> history = tableService.findHistory(databaseId, tableId, principal);
        log.trace("find all history resulted in history {}", history);
        return ResponseEntity.ok(history);
    }


}
