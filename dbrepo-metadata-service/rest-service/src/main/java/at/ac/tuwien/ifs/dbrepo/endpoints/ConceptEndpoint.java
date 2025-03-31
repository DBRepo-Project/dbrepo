package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.ConceptDto;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.ConceptService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/concept")
public class ConceptEndpoint extends AbstractEndpoint {

    private final ConceptService conceptService;
    private final MetadataMapper metadataMapper;

    @Autowired
    public ConceptEndpoint(ConceptService conceptService, MetadataMapper metadataMapper) {
        this.conceptService = conceptService;
        this.metadataMapper = metadataMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_semantic_concepts_findall")
    @Operation(summary = "List concepts",
            description = "List all semantic concepts known to the metadata database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List concepts",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ConceptDto.class)))}),
    })
    public ResponseEntity<List<ConceptDto>> findAll() {
        log.debug("endpoint list concepts");
        return ResponseEntity.ok()
                .body(conceptService.findAll()
                        .stream()
                        .map(metadataMapper::tableColumnConceptToConceptDto)
                        .toList());
    }

}
