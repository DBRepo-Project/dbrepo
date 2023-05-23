package at.tuwien.endpoints;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.service.SemanticService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantic")
public class SemanticsEndpoint {

    private final OntologyMapper ontologyMapper;
    private final SemanticService semanticService;

    @Autowired
    public SemanticsEndpoint(OntologyMapper ontologyMapper, SemanticService semanticService) {
        this.ontologyMapper = ontologyMapper;
        this.semanticService = semanticService;
    }

    @PostMapping("/concept")
    @PreAuthorize("hasAuthority('create-semantic-concept')")
    @Timed(value = "semantics.concept.save", description = "Time needed to create or update a semantic concept")
    @Operation(summary = "Create or update a semantic concept")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Created or updated a semantic concept",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConceptDto.class))}),
    })
    public ResponseEntity<ConceptDto> saveConcept(@NotNull @Valid @RequestBody ConceptSaveDto data) {
        log.debug("endpoint save or update concept, data={}", data);
        final ConceptDto dto = ontologyMapper.tableColumnConceptToConceptDto(semanticService.saveConcept(data));
        log.trace("save or update concept resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @PostMapping("/unit")
    @PreAuthorize("hasAuthority('create-semantic-unit')")
    @Timed(value = "semantics.unit.save", description = "Time needed to create or update a semantic unit")
    @Operation(summary = "Create or update a semantic unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Created or updated a semantic unit",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UnitDto.class))}),
    })
    public ResponseEntity<UnitDto> saveConcept(@NotNull @Valid @RequestBody UnitSaveDto data) {
        log.debug("endpoint save or update unit, data={}", data);
        final UnitDto dto = ontologyMapper.tableColumnUnitToUnitDto(semanticService.saveUnit(data));
        log.trace("save or update unit resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

}
