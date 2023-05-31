package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableColumnNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.service.TableService;
import io.micrometer.core.annotation.Timed;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantic/database/{databaseId}/table/{tableId}")
public class TableEndpoint {

    private final TableService tableService;

    @Autowired
    public TableEndpoint(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Timed(value = "semantics.table.analyse", description = "Time needed to analyse table semantics")
    @Operation(summary = "Suggest table semantics", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Suggested table semantics successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableColumnEntityDto.class)))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find the table",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Generated query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<EntityDto>> analyseTable(@NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId)
            throws TableNotFoundException, QueryMalformedException {
        log.debug("endpoint analyse table semantics, databaseId={}, tableId={}", databaseId, tableId);
        final List<EntityDto> dtos = tableService.suggestTableSemantics(databaseId, tableId);
        log.trace("analyse table semantics resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @GetMapping("/column/{columnId}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Timed(value = "semantics.table.columnanalyse", description = "Time needed to analyse table column semantics")
    @Operation(summary = "Suggest table column semantics", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Suggested table column semantics successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TableColumnEntityDto.class)))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find the table column",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Generated query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableColumnEntityDto>> analyseTableColumn(@NotNull @PathVariable("databaseId") Long databaseId,
                                                                         @NotNull @PathVariable("tableId") Long tableId,
                                                                         @NotNull @PathVariable("columnId") Long columnId)
            throws QueryMalformedException, TableColumnNotFoundException {
        log.debug("endpoint analyse table column semantics, databaseId={}, tableId={}, columnId={}", databaseId, tableId, columnId);
        final List<TableColumnEntityDto> dtos = tableService.suggestTableColumnSemantics(databaseId, tableId, columnId);
        log.trace("analyse table semantics resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
