package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts.UnitDto;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.UnitService;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/v1/unit")
public class UnitEndpoint extends AbstractEndpoint {

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
        return ResponseEntity.ok()
                .body(unitService.findAll()
                        .stream()
                        .map(metadataMapper::tableColumnUnitToUnitDto)
                        .toList());
    }

}
