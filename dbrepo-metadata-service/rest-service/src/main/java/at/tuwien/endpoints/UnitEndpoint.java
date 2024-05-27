package at.tuwien.endpoints;

import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.mapper.SemanticMapper;
import at.tuwien.service.UnitService;
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
@RequestMapping(path = "/api/unit")
public class UnitEndpoint {

    private final UnitService unitService;
    private final SemanticMapper semanticMapper;

    @Autowired
    public UnitEndpoint(SemanticMapper semanticMapper, UnitService unitService) {
        this.semanticMapper = semanticMapper;
        this.unitService = unitService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_semantic_units_findall")
    @Operation(summary = "List semantic units")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find all semantic units",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UnitDto.class)))}),
    })
    public ResponseEntity<List<UnitDto>> findAll() {
        log.debug("endpoint list units");
        final List<UnitDto> dtos = unitService.findAll()
                .stream()
                .map(semanticMapper::tableColumnUnitToUnitDto)
                .toList();
        log.trace("Find all units resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
