package at.tuwien.endpoints;

import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.mapper.MetadataMapper;
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
    private final MetadataMapper metadataMapper;

    @Autowired
    public UnitEndpoint(MetadataMapper metadataMapper, UnitService unitService) {
        this.metadataMapper = metadataMapper;
        this.unitService = unitService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Observed(name = "dbrepo_semantic_units_findall")
    @Operation(summary = "List units",
            description = "Lists units known to the metadata database.")
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
                .map(metadataMapper::tableColumnUnitToUnitDto)
                .toList();
        return ResponseEntity.ok()
                .body(dtos);
    }

}
