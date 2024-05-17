package at.tuwien.endpoints;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.mapper.SemanticMapper;
import at.tuwien.service.ConceptService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/concept")
public class ConceptEndpoint {

    private final ConceptService conceptService;
    private final SemanticMapper semanticMapper;

    @Autowired
    public ConceptEndpoint(ConceptService conceptService, SemanticMapper semanticMapper) {
        this.conceptService = conceptService;
        this.semanticMapper = semanticMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_metadata_semantic_concepts_findall")
    @Operation(summary = "List semantic concepts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find all semantic concepts",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ConceptDto.class)))}),
    })
    public ResponseEntity<List<ConceptDto>> findAll() {
        log.debug("endpoint list concepts");
        final List<ConceptDto> dtos = conceptService.findAll()
                .stream()
                .map(semanticMapper::tableColumnConceptToConceptDto)
                .toList();
        log.trace("Find all concepts resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
