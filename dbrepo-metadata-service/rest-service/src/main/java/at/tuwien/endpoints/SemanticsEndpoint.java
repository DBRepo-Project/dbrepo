package at.tuwien.endpoints;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.*;
import at.tuwien.mapper.SemanticMapper;
import at.tuwien.service.EntityService;
import at.tuwien.service.SemanticService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantic")
public class SemanticsEndpoint {

    private final SemanticMapper semanticMapper;
    private final SemanticService semanticService;
    private final EntityService entityService;

    @Autowired
    public SemanticsEndpoint(SemanticMapper semanticMapper, SemanticService semanticService,
                             EntityService entityService) {
        this.semanticMapper = semanticMapper;
        this.semanticService = semanticService;
        this.entityService = entityService;
    }

    @GetMapping("/concept")
    @Transactional(readOnly = true)
    @Observed(name = "dbr_semantic_concepts_findall")
    @Operation(summary = "List semantic concepts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find all semantic concepts",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ConceptDto.class)))}),
    })
    public ResponseEntity<List<ConceptDto>> findAllConcepts() {
        log.debug("endpoint list concepts");
        final List<ConceptDto> dtos = semanticService.findAllConcepts()
                .stream()
                .map(semanticMapper::tableColumnConceptToConceptDto)
                .toList();
        log.trace("Find all concepts resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @GetMapping("/unit")
    @Transactional(readOnly = true)
    @Observed(name = "dbr_semantic_units_findall")
    @Operation(summary = "List semantic units")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find all semantic units",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UnitDto.class)))}),
    })
    public ResponseEntity<List<UnitDto>> findAllUnits() {
        log.debug("endpoint list units");
        final List<UnitDto> dtos = semanticService.findAllUnits()
                .stream()
                .map(semanticMapper::tableColumnUnitToUnitDto)
                .toList();
        log.trace("Find all units resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @GetMapping("/database/{databaseId}/table/{tableId}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Observed(name = "dbr_semantic_table_analyse")
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
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<EntityDto>> analyseTable(@NotNull @PathVariable("databaseId") Long databaseId,
                                                        @NotNull @PathVariable("tableId") Long tableId)
            throws TableNotFoundException, QueryMalformedException, DatabaseNotFoundException, OntologyInvalidException {
        log.debug("endpoint analyse table semantics, databaseId={}, tableId={}", databaseId, tableId);
        final List<EntityDto> dtos = entityService.suggestTableSemantics(databaseId, tableId);
        log.trace("analyse table semantics resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @GetMapping("/database/{databaseId}/table/{tableId}/column/{columnId}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('table-semantic-analyse')")
    @Observed(name = "dbr_semantic_column_analyse")
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
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<TableColumnEntityDto>> analyseTableColumn(@NotNull @PathVariable("databaseId") Long databaseId,
                                                                         @NotNull @PathVariable("tableId") Long tableId,
                                                                         @NotNull @PathVariable("columnId") Long columnId)
            throws QueryMalformedException, TableColumnNotFoundException, TableNotFoundException, DatabaseNotFoundException,
            OntologyInvalidException {
        log.debug("endpoint analyse table column semantics, databaseId={}, tableId={}, columnId={}", databaseId, tableId, columnId);
        final List<TableColumnEntityDto> dtos = entityService.suggestTableColumnSemantics(databaseId, tableId, columnId);
        log.trace("analyse table semantics resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
