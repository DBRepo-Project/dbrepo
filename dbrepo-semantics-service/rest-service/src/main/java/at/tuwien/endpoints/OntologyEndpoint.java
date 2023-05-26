package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.OntologyBriefDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.exception.OntologyNotFoundException;
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
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.security.Principal;
import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantic/ontology")
public class OntologyEndpoint {

    private final OntologyMapper ontologyMapper;
    private final OntologyService ontologyService;

    @Autowired
    public OntologyEndpoint(OntologyMapper ontologyMapper, OntologyService ontologyService) {
        this.ontologyMapper = ontologyMapper;
        this.ontologyService = ontologyService;
    }

    @GetMapping
    @Timed(value = "semantics.ontology.list", description = "Time needed to list ontologies")
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

    @GetMapping("/{id}")
    @Timed(value = "semantics.ontology.find", description = "Time needed to find a specific ontology")
    @Operation(summary = "Find one ontology")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find one ontology",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<OntologyDto> find(@NotNull @PathVariable("id") Long id) throws OntologyNotFoundException {
        log.debug("endpoint find all ontologies, id={}", id);
        final OntologyDto dto = ontologyMapper.ontologyToOntologyDto(ontologyService.find(id));
        log.trace("create ontology resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create-ontology')")
    @Timed(value = "semantics.ontology.create", description = "Time needed to register a new ontology")
    @Operation(summary = "Register a new ontology", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Registered ontology successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))}),
    })
    public ResponseEntity<OntologyDto> create(@NotNull @Valid @RequestBody OntologyCreateDto data,
                                              @NotNull Principal principal) {
        log.debug("endpoint create ontology, data={}, principal={}", data, principal);
        final OntologyDto dto = ontologyMapper.ontologyToOntologyDto(ontologyService.create(data));
        log.trace("create ontology resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('update-ontology')")
    @Timed(value = "semantics.ontology.update", description = "Time needed to update a new ontology")
    @Operation(summary = "Update an ontology", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated ontology successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<OntologyDto> update(@NotNull @PathVariable("id") Long id,
                                              @NotNull @Valid @RequestBody OntologyModifyDto data,
                                              @NotNull Principal principal) throws OntologyNotFoundException {
        log.debug("endpoint update ontology, data={}, principal={}", data, principal);
        final OntologyDto dto = ontologyMapper.ontologyToOntologyDto(ontologyService.update(id, data));
        log.trace("update ontology resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete-ontology')")
    @Timed(value = "semantics.ontology.delete", description = "Time needed to delete an ontology")
    @Operation(summary = "Delete an ontology", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted ontology successfully",
                    content = {@Content(
                            mediaType = "application/json")}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<?> delete(@NotNull @PathVariable("id") Long id) throws OntologyNotFoundException {
        log.debug("endpoint delete ontology, id={}", id);
        ontologyService.delete(id);
        return ResponseEntity.accepted()
                .build();
    }

}
