package at.tuwien.endpoints;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableColumnNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.mapper.SemanticMapper;
import at.tuwien.service.EntityService;
import at.tuwien.service.SemanticService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
    private final OntologyMapper ontologyMapper;
    private final SemanticService semanticService;
    private final EntityService entityService;

    @Autowired
    public SemanticsEndpoint(SemanticMapper semanticMapper, OntologyMapper ontologyMapper,
                             SemanticService semanticService, EntityService entityService) {
        this.semanticMapper = semanticMapper;
        this.ontologyMapper = ontologyMapper;
        this.semanticService = semanticService;
        this.entityService = entityService;
    }

    @GetMapping("/concept")
    @Transactional(readOnly = true)
    @Timed(value = "semantics.concept.list", description = "Time needed to find all semantic concepts")
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

    @PostMapping("/concept")
    @Transactional
    @PreAuthorize("hasAuthority('create-semantic-concept')")
    @Timed(value = "semantics.concept.save", description = "Time needed to save a semantic concept")
    @Operation(summary = "Create or update a semantic concept", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Saved a semantic concept",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConceptDto.class))}),
    })
    public ResponseEntity<ConceptDto> saveUnit(@NotNull @Valid @RequestBody ConceptSaveDto data) {
        log.debug("endpoint save concept, data={}", data);
        final ConceptDto dto = ontologyMapper.tableColumnConceptToConceptDto(semanticService.saveConcept(data));
        log.trace("save concept resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @GetMapping("/unit")
    @Transactional(readOnly = true)
    @Timed(value = "semantics.concept.list", description = "Time needed to find all semantic units")
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

    @PostMapping("/unit")
    @Transactional
    @PreAuthorize("hasAuthority('create-semantic-unit')")
    @Timed(value = "semantics.unit.save", description = "Time needed to save a semantic unit")
    @Operation(summary = "Save a semantic unit", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Saved a semantic unit",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UnitDto.class))}),
    })
    public ResponseEntity<UnitDto> saveUnit(@NotNull @Valid @RequestBody UnitSaveDto data) {
        log.debug("endpoint save or update unit, data={}", data);
        final UnitDto dto = ontologyMapper.tableColumnUnitToUnitDto(semanticService.saveUnit(data));
        log.trace("save unit resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @GetMapping("/database/{databaseId}/table/{tableId}")
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
        final List<EntityDto> dtos = entityService.suggestTableSemantics(databaseId, tableId);
        log.trace("analyse table semantics resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

    @GetMapping("/database/{databaseId}/table/{tableId}/column/{columnId}")
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
        final List<TableColumnEntityDto> dtos = entityService.suggestTableColumnSemantics(databaseId, tableId, columnId);
        log.trace("analyse table semantics resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
