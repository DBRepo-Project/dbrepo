package at.tuwien.endpoints;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.mapper.TableMapper;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.security.Principal;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/table/{tableId}/column/{columnId}")
public class TableColumnEndpoint extends AbstractEndpoint {

    private final TableMapper tableMapper;
    private final TableService tableService;

    @Autowired
    public TableColumnEndpoint(TableMapper tableMapper, TableService tableService, DatabaseService databaseService,
                               AccessService accessService) {
        super(accessService, databaseService);
        this.tableMapper = tableMapper;
        this.tableService = tableService;
    }

    @PutMapping
    @Transactional
    @Timed(value = "semantics.column_update", description = "Time needed to update a table column semantic mapping")
    @Operation(summary = "Update a table column semantic mapping", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated column semantics successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ColumnDto.class))}),
            @ApiResponse(responseCode = "400",
                    description = "Update semantic concept query is malformed or update unit of measurement query is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "403",
                    description = "Access to the database is forbidden",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Table, database, semantic concept, unit of measurement or container could not be found",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "405",
                    description = "Update column semantics not permitted",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ColumnDto.class))}),
    })
    public ResponseEntity<ColumnDto> update(@NotNull @PathVariable("id") Long containerId,
                                            @NotNull @PathVariable("databaseId") Long databaseId,
                                            @NotNull @PathVariable("tableId") Long tableId,
                                            @NotNull @PathVariable("columnId") Long columnId,
                                            @NotNull @Valid @RequestBody ColumnSemanticsUpdateDto updateDto,
                                            @NotNull Principal principal) throws NotAllowedException,
            AccessDeniedException, TableNotFoundException, TableMalformedException, DatabaseNotFoundException,
            ContainerNotFoundException, UnitNotFoundException, ConceptNotFoundException {
        log.debug("endpoint update table, containerId={}, databaseId={}, tableId={}, principal={}", containerId,
                databaseId, tableId, principal);
        if (!hasTablePermission(containerId, databaseId, tableId, "TABLE_UPDATE", principal)) {
            log.error("Missing table update permission");
            throw new NotAllowedException("Missing table update permission");
        }
        final TableColumn column = tableService.update(containerId, databaseId, tableId, columnId, updateDto, principal);
        log.info("Updated table semantics of table with id {} and database with id {}", tableId, databaseId);
        final ColumnDto dto = tableMapper.tableColumnToColumnDto(column);
        return ResponseEntity.accepted()
                .body(dto);
    }

}
