package at.tuwien.endpoints;

import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.TableService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantic/database/{databaseId}/table/{tableId}/analyse")
public class TableEndpoint {

    private final TableService tableService;

    @Autowired
    public TableEndpoint(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Timed(value = "semantics.table.analyse", description = "Time needed to analyse table semantics")
    @Operation(summary = "Suggest table semantics", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Suggested table semantics successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TableColumnEntityDto[].class))}),
    })
    public ResponseEntity<List<TableColumnEntityDto>> analyse(@NotNull @PathVariable("databaseId") Long databaseId,
                                                              @NotNull @PathVariable("tableId") Long tableId)
            throws TableNotFoundException, QueryMalformedException {
        log.debug("endpoint analyse table semantics, databaseId={}, tableId={}", databaseId, tableId);
        final List<TableColumnEntityDto> dtos = tableService.suggest(databaseId, tableId);
        log.trace("analyse table semantics resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
