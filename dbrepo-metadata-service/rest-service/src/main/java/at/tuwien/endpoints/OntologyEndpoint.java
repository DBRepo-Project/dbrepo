package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.*;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.*;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.service.EntityService;
import at.tuwien.service.OntologyService;
import io.micrometer.observation.annotation.Observed;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/ontology")
public class OntologyEndpoint {

    private final OntologyMapper ontologyMapper;
    private final OntologyService ontologyService;
    private final EntityService entityService;

    @Autowired
    public OntologyEndpoint(OntologyMapper ontologyMapper, OntologyService ontologyService, EntityService entityService) {
        this.ontologyMapper = ontologyMapper;
        this.ontologyService = ontologyService;
        this.entityService = entityService;
    }

    @GetMapping
    @Observed(name = "dbrepo_metadata_ontologies_findall")
    @Operation(summary = "List all ontologies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List all ontologies",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OntologyDto.class)))}),
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

    @GetMapping("/{ontologyId}")
    @Observed(name = "dbrepo_metadata_ontologies_find")
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
    public ResponseEntity<OntologyDto> find(@NotNull @PathVariable("ontologyId") Long ontologyId) throws OntologyNotFoundException {
        log.debug("endpoint find all ontologies, ontologyId={}", ontologyId);
        final OntologyDto dto = ontologyMapper.ontologyToOntologyDto(ontologyService.find(ontologyId));
        log.trace("create ontology resulted in dto {}", dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create-ontology')")
    @Observed(name = "dbrepo_metadata_ontologies_create")
    @Operation(summary = "Register a new ontology", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Registered ontology successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))})
    })
    public ResponseEntity<OntologyDto> create(@NotNull @Valid @RequestBody OntologyCreateDto data,
                                              @NotNull Principal principal) {
        log.debug("endpoint create ontology, data={}", data);
        final OntologyDto dto = ontologyMapper.ontologyToOntologyDto(ontologyService.create(data, principal));
        log.trace("create ontology resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

    @PutMapping("/{ontologyId}")
    @PreAuthorize("hasAuthority('update-ontology')")
    @Observed(name = "dbrepo_metadata_ontologies_update")
    @Operation(summary = "Update an ontology", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<OntologyDto> update(@NotNull @PathVariable("ontologyId") Long ontologyId,
                                              @NotNull @Valid @RequestBody OntologyModifyDto data) throws OntologyNotFoundException {
        log.debug("endpoint update ontology, data={}", data);
        final Ontology ontology = ontologyService.find(ontologyId);
        final OntologyDto dto = ontologyMapper.ontologyToOntologyDto(ontologyService.update(ontology, data));
        log.trace("update ontology resulted in dto {}", dto);
        return ResponseEntity.accepted()
                .body(dto);
    }

    @DeleteMapping("/{ontologyId}")
    @PreAuthorize("hasAuthority('delete-ontology')")
    @Observed(name = "dbrepo_metadata_ontologies_delete")
    @Operation(summary = "Delete an ontology", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
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
    public ResponseEntity<?> delete(@NotNull @PathVariable("ontologyId") Long ontologyId) throws OntologyNotFoundException {
        log.debug("endpoint delete ontology, ontologyId={}", ontologyId);
        final Ontology ontology = ontologyService.find(ontologyId);
        ontologyService.delete(ontology);
        return ResponseEntity.accepted()
                .build();
    }

    @GetMapping("/{ontologyId}/entity")
    @PreAuthorize("hasAuthority('execute-semantic-query')")
    @Observed(name = "dbrepo_metadata_ontologies_entities_find")
    @Operation(summary = "Find entities", security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found entities",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = EntityDto.class)))}),
            @ApiResponse(responseCode = "400",
                    description = "Filter params are invalid",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "417",
                    description = "Generated query or uri is malformed",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiErrorDto.class))}),
    })
    public ResponseEntity<List<EntityDto>> find(@NotNull @PathVariable("ontologyId") Long id,
                                                @RequestParam(name = "label", required = false) String label,
                                                @RequestParam(name = "uri", required = false) String uri)
            throws OntologyNotFoundException, UriMalformedException, FilterBadRequestException, MalformedException {
        log.debug("endpoint find entities by uri, id={}, label={}, uri={}", id, label, uri);
        final Ontology ontology = ontologyService.find(id);
        /* check */
        if ((label != null && uri != null) || (label == null && uri == null)) {
            log.error("Failed to find entities: either label or uri must be defined");
            throw new FilterBadRequestException("Failed to find entities: either label or uri must be defined");
        }
        if (uri != null && !uri.startsWith(ontology.getUri())) {
            log.error("Failed to find entities: uri {} does not start with expected ontology uri {}", uri, ontology.getUri());
            throw new UriMalformedException("Failed to find entity: uri " + uri + " does not start with expected ontology uri " + ontology.getUri());
        }
        if (ontology.getSparqlEndpoint() == null) {
            log.error("Failed to find SPARQL endpoint for ontology with id {}", ontology.getId());
            throw new OntologyNotFoundException("Failed to find SPARQL endpoint for ontology with id " + ontology.getId());
        }
        /* get */
        final List<EntityDto> dtos;
        if (uri != null) {
            dtos = entityService.findByUri(uri);
            return ResponseEntity.ok()
                    .body(dtos);
        }
        dtos = entityService.findByLabel(ontology, label);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
