package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.error.ApiErrorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.semantics.Ontology;
import at.ac.tuwien.ifs.dbrepo.core.exception.FilterBadRequestException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.OntologyNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UriMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.service.EntityService;
import at.ac.tuwien.ifs.dbrepo.service.OntologyService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping(path = "/api/v1/ontology")
public class OntologyEndpoint extends AbstractEndpoint {

    private final EntityService entityService;
    private final MetadataMapper metadataMapper;
    private final OntologyService ontologyService;

    @Autowired
    public OntologyEndpoint(EntityService entityService, MetadataMapper metadataMapper,
                            OntologyService ontologyService) {
        this.entityService = entityService;
        this.metadataMapper = metadataMapper;
        this.ontologyService = ontologyService;
    }

    @GetMapping
    @Observed(name = "dbrepo_ontologies_findall")
    @Operation(summary = "List ontologies",
            description = "Lists all ontologies known to the metadata database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List ontologies",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = OntologyBriefDto.class)))}),
    })
    public ResponseEntity<List<OntologyBriefDto>> findAll() {
        log.debug("endpoint find all ontologies");
        return ResponseEntity.ok(ontologyService.findAll()
                .stream()
                .map(metadataMapper::ontologyToOntologyBriefDto)
                .toList());
    }

    @GetMapping("/{ontologyId}")
    @Observed(name = "dbrepo_ontologies_find")
    @Operation(summary = "Find ontology",
            description = "Finds an ontology with id in the metadata database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Find one ontology",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content}),
    })
    public ResponseEntity<OntologyDto> find(@NotNull @PathVariable("ontologyId") UUID ontologyId)
            throws OntologyNotFoundException {
        log.debug("endpoint find all ontologies, ontologyId={}", ontologyId);
        return ResponseEntity.ok(metadataMapper.ontologyToOntologyDto(ontologyService.find(ontologyId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('create-ontology')")
    @Observed(name = "dbrepo_ontologies_create")
    @Operation(summary = "Create ontology",
            description = "Creates an ontology in the metadata database. Requires role `create-ontology`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                    description = "Registered ontology successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))})
    })
    public ResponseEntity<OntologyDto> create(@NotNull @Valid @RequestBody OntologyCreateDto data,
                                              Principal principal) {
        log.debug("endpoint create ontology, data={}", data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metadataMapper.ontologyToOntologyDto(ontologyService.create(data, principal)));
    }

    @PutMapping("/{ontologyId}")
    @PreAuthorize("hasAuthority('update-ontology')")
    @Observed(name = "dbrepo_ontologies_update")
    @Operation(summary = "Update ontology",
            description = "Updates an ontology with id. Requires role `update-ontology`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Updated ontology successfully",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OntologyDto.class))}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content}),
    })
    public ResponseEntity<OntologyDto> update(@NotNull @PathVariable("ontologyId") UUID ontologyId,
                                              @NotNull @Valid @RequestBody OntologyModifyDto data)
            throws OntologyNotFoundException {
        log.debug("endpoint update ontology, ontologyId={}, data={}", ontologyId, data);
        return ResponseEntity.accepted()
                .body(metadataMapper.ontologyToOntologyDto(
                        ontologyService.update(ontologyService.find(ontologyId), data)));
    }

    @DeleteMapping("/{ontologyId}")
    @PreAuthorize("hasAuthority('delete-ontology')")
    @Observed(name = "dbrepo_ontologies_delete")
    @Operation(summary = "Delete ontology",
            description = "Deletes an ontology with given id. Requires role `delete-ontology`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "Deleted ontology successfully",
                    content = {@Content(
                            mediaType = "application/json")}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content}),
    })
    public ResponseEntity<Void> delete(@NotNull @PathVariable("ontologyId") UUID ontologyId)
            throws OntologyNotFoundException {
        log.debug("endpoint delete ontology, ontologyId={}", ontologyId);
        ontologyService.delete(ontologyService.find(ontologyId));
        return ResponseEntity.accepted()
                .build();
    }

    @GetMapping("/{ontologyId}/entity")
    @PreAuthorize("hasAuthority('execute-semantic-query')")
    @Observed(name = "dbrepo_ontologies_entities_find")
    @Operation(summary = "Find entities",
            description = "Finds semantic entities by label or uri in an ontology with id. Requires role `execute-semantic-query`.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "basicAuth")})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found entities",
                    content = {@Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = EntityDto.class)))}),
            @ApiResponse(responseCode = "400",
                    description = "Filter params are invalid",
                    content = {@Content}),
            @ApiResponse(responseCode = "404",
                    description = "Could not find ontology",
                    content = {@Content}),
            @ApiResponse(responseCode = "417",
                    description = "Generated query or uri is malformed",
                    content = {@Content}),
            @ApiResponse(responseCode = "422",
                    description = "Ontology does not have rdf or sparql endpoint",
                    content = {@Content}),
    })
    public ResponseEntity<List<EntityDto>> find(@NotNull @PathVariable("ontologyId") UUID id,
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
        if (uri != null) {
            return ResponseEntity.ok()
                    .body(entityService.findByUri(uri));
        }
        return ResponseEntity.ok()
                .body(entityService.findByLabel(ontology, label));
    }

}
