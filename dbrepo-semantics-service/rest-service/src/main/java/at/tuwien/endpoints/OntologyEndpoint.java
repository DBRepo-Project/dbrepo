package at.tuwien.endpoints;

import at.tuwien.api.semantics.OntologyBriefDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyDto;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.service.OntologyService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.security.Principal;
import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantics/ontology")
public class OntologyEndpoint {

    private final OntologyMapper ontologyMapper;
    private final OntologyService ontologyService;

    @Autowired
    public OntologyEndpoint(OntologyMapper ontologyMapper, OntologyService ontologyService) {
        this.ontologyMapper = ontologyMapper;
        this.ontologyService = ontologyService;
    }

    @GetMapping
    @Transactional
    @Timed(value = "semantics.list-ontologies", description = "Time needed to list ontologies")
    @Operation(summary = "List all ontologies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List all ontologies",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto[].class))}),
    })
    public ResponseEntity<List<OntologyBriefDto>> findAll() {
        log.debug("endpoint find all ontologies");
        final List<OntologyBriefDto> dtos = ontologyService.findAll()
                .stream()
                .map(ontologyMapper::ontologyToOntologyBriefDto)
                .toList();
        log.trace("create ontology resulted in dtos {}", dtos);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAuthority('create-ontology')")
    @Timed(value = "semantics.create-ontology", description = "Time needed to register a new ontology")
    @Operation(summary = "Register a new ontology", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Registered ontology successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))}),
    })
    public ResponseEntity<OntologyDto> create(@NotNull @Valid @RequestBody OntologyCreateDto createDto,
                                              @NotNull Principal principal) {
        log.debug("endpoint create ontology, createDto={}, principal={}", createDto, principal);
        final OntologyDto dto = ontologyMapper.ontologyToOntologyDto(ontologyService.create(createDto));
        log.trace("create ontology resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

}
