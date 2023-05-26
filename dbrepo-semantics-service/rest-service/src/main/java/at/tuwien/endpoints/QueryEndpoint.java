package at.tuwien.endpoints;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.semantics.*;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.FilterBadRequestException;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.UriMalformedException;
import at.tuwien.service.OntologyService;
import at.tuwien.service.QueryService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/semantic")
public class QueryEndpoint {

    private final QueryService queryService;
    private final OntologyService ontologyService;

    @Autowired
    public QueryEndpoint(QueryService queryService, OntologyService ontologyService) {
        this.queryService = queryService;
        this.ontologyService = ontologyService;
    }

    @GetMapping("/ontology/{id}/entity")
    @PreAuthorize("hasAuthority('execute-semantic-query')")
    @Timed(value = "semantics.find", description = "Time needed to find entities")
    @Operation(summary = "Find entities", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Found entities",
                    content = {@Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EntityDto[].class))}),
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
    })
    public ResponseEntity<List<EntityDto>> find(@NotNull @PathVariable("id") Long id,
                                                @RequestParam(name = "label", required = false) String label,
                                                @RequestParam(name = "uri", required = false) String uri)
            throws OntologyNotFoundException, QueryMalformedException, UriMalformedException, FilterBadRequestException {
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
        /* get */
        final List<EntityDto> dtos;
        if (uri != null) {
            dtos = queryService.findByUri(ontology, uri);
            log.trace("find entities resulted in dtos {}", dtos);
            return ResponseEntity.ok()
                    .body(dtos);
        }
        dtos = queryService.findByLabel(ontology, label);
        log.trace("find entities resulted in dtos {}", dtos);
        return ResponseEntity.ok()
                .body(dtos);
    }

}
